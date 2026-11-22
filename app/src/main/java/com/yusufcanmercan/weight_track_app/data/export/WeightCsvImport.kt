package com.yusufcanmercan.weight_track_app.data.export

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.local.hasKnownMeasurementTime
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel
import com.yusufcanmercan.weight_track_app.ui.edit.EntryMood
import com.yusufcanmercan.weight_track_app.util.startOfDay
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs

/** İçe aktarılacak dosyanın üst sınırı; daha büyüğü okunmadan reddedilir. */
internal const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

/**
 * Bir CSV dosyasının içe aktarma planı.
 * [added] eklenecek yeni ölçümler, [duplicates] zaten var sayılıp atlananlar,
 * [badLines] okunamayan ya da geçersiz satırların dosyadaki numaraları (1'den başlar).
 */
internal data class CsvImportPlan(
    val added: List<Weight>,
    val duplicates: Int,
    val badLines: List<Int>,
)

/**
 * İki biçimi kabul eder:
 * - uygulamanın kendi dışa aktarımı: `date,time,weight_kg,mood,note`;
 * - basit biçim: `tarih,kilo` (başlık isteğe bağlı; `;` ayırıcıyla ondalık virgül de olur).
 * Tarih `yyyy-MM-dd` ya da `gg.aa.yyyy`, saat `HH:mm` ya da `HH:mm:ss`.
 *
 * Yinelenen sayılanlar: aynı gün aynı kilo ve iki taraftan biri saatsizse; ya da ikisi de saatliyse
 * aynı saniye aynı kilo. Aynı gün farklı saatlerdeki ölçümler ayrı kayıttır.
 */
internal fun planWeightCsvImport(
    text: String,
    existing: List<Weight>,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): CsvImportPlan {
    val rows = parseCsvRows(text.removePrefix("\uFEFF"))
    val header = rows.firstOrNull()?.let { columnsOf(it.cells) }
    val body = if (header != null) rows.drop(1) else rows
    val columns = header ?: Columns(date = 0, weight = 1)

    val kept = existing.toMutableList()
    val added = mutableListOf<Weight>()
    val badLines = mutableListOf<Int>()
    var duplicates = 0
    for (row in body) {
        if (row.cells.all { it.isBlank() }) continue
        val weight = readRow(row.cells, columns, now, zone)
        when {
            weight == null -> badLines += row.line
            kept.any { it.sameMeasurementAs(weight, zone) } -> duplicates++
            else -> {
                added += weight
                kept += weight
            }
        }
    }
    return CsvImportPlan(added, duplicates, badLines)
}

private class Columns(val date: Int, val weight: Int, val time: Int? = null, val mood: Int? = null, val note: Int? = null)

/** Satır bir başlıksa sütun yerlerini döndürür; ilk satır veri ise `null`. */
private fun columnsOf(cells: List<String>): Columns? {
    val names = cells.map { it.trim().lowercase() }
    fun find(vararg options: String) = names.indexOfFirst { it in options }.takeIf { it >= 0 }
    val date = find("date", "tarih")
    val weight = find("weight_kg", "weight", "kg", "kilo")
    if (date != null && weight != null) {
        return Columns(date, weight, find("time", "saat"), find("mood"), find("note", "not"))
    }
    // Tanınmayan başlık: ikinci sütun sayı değilse yine başlık sayılır, sıra tarih + kilo.
    return if (cells.size >= 2 && parseKg(cells[1]) == null && parseDay(cells[0]) == null) Columns(0, 1) else null
}

private fun readRow(cells: List<String>, columns: Columns, now: Long, zone: TimeZone): Weight? {
    val day = parseDay(cells.getOrNull(columns.date) ?: return null) ?: return null
    val kg = parseKg(cells.getOrNull(columns.weight) ?: return null) ?: return null
    if (kg !in EditWeightViewModel.MIN_KG..EditWeightViewModel.MAX_KG) return null
    val timeText = columns.time?.let { cells.getOrNull(it) }?.trim().orEmpty()
    val seconds = if (timeText.isEmpty()) null else parseSecondOfDay(timeText) ?: return null

    val calendar = GregorianCalendar(zone).apply {
        clear()
        set(day.year, day.month - 1, day.dayOfMonth, 0, 0, 0)
    }
    // Takvimde olmayan gün (31 Şubat gibi) kayarak başka güne düşer; bunu geçersiz say.
    if (calendar.get(Calendar.DAY_OF_MONTH) != day.dayOfMonth) return null
    if (seconds != null) calendar.add(Calendar.SECOND, seconds)
    val timeStamp = calendar.timeInMillis
    if (timeStamp > now) return null

    return Weight(
        weight = kg,
        timeStamp = timeStamp,
        note = columns.note?.let { cells.getOrNull(it) }?.let(::importedText)?.take(EditWeightViewModel.MAX_NOTE_LENGTH),
        mood = columns.mood?.let { cells.getOrNull(it) }?.trim()
            ?.let { name -> EntryMood.entries.firstOrNull { it.name == name }?.name },
        hasMeasurementTime = seconds != null,
    )
}

private class Day(val year: Int, val month: Int, val dayOfMonth: Int)

private val isoDate = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""")
private val dottedDate = Regex("""(\d{1,2})[./](\d{1,2})[./](\d{4})""")

private fun parseDay(text: String): Day? {
    val value = text.trim()
    val day = isoDate.matchEntire(value)?.destructured?.let { (y, m, d) -> Day(y.toInt(), m.toInt(), d.toInt()) }
        ?: dottedDate.matchEntire(value)?.destructured?.let { (d, m, y) -> Day(y.toInt(), m.toInt(), d.toInt()) }
        ?: return null
    return day.takeIf { it.year in 1900..9999 && it.month in 1..12 && it.dayOfMonth in 1..31 }
}

private val timeOfDay = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""")

private fun parseSecondOfDay(text: String): Int? {
    val (h, m, s) = timeOfDay.matchEntire(text)?.destructured ?: return null
    val hour = h.toInt()
    val minute = m.toInt()
    val second = s.ifEmpty { "0" }.toInt()
    if (hour > 23 || minute > 59 || second > 59) return null
    return hour * 3600 + minute * 60 + second
}

private fun parseKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Dışa aktarımın formül koruması için eklediği tek kesme işaretini geri alır. */
private fun importedText(value: String): String? = value.removePrefix("'").takeIf { it.isNotBlank() }

private fun Weight.sameMeasurementAs(other: Weight, zone: TimeZone): Boolean {
    if (abs(weight - other.weight) > 0.001) return false
    val timed = hasKnownMeasurementTime(zone)
    val otherTimed = other.hasKnownMeasurementTime(zone)
    return if (timed && otherTimed) {
        timeStamp / 1000 == other.timeStamp / 1000
    } else {
        startOfDay(timeStamp, zone) == startOfDay(other.timeStamp, zone)
    }
}

private class CsvRow(val line: Int, val cells: List<String>)

/** Tırnaklı alanları, alan içi satır sonlarını ve `""` kaçışını tanıyan küçük CSV okuyucu. */
private fun parseCsvRows(text: String): List<CsvRow> {
    val firstLine = text.lineSequence().firstOrNull().orEmpty()
    val separator = if (';' in firstLine && ',' !in firstLine.replace(Regex("""\d,\d"""), "")) ';' else ','
    val rows = mutableListOf<CsvRow>()
    var cells = mutableListOf<String>()
    val cell = StringBuilder()
    var line = 1
    var rowLine = 1
    var quoted = false
    var i = 0
    fun endRow() {
        cells += cell.toString()
        cell.clear()
        rows += CsvRow(rowLine, cells)
        cells = mutableListOf()
    }
    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
            c == '"' && (quoted || cell.isEmpty()) -> quoted = !quoted
            quoted -> { if (c == '\n') line++; cell.append(c) }
            c == separator -> { cells += cell.toString(); cell.clear() }
            c == '\r' -> Unit
            c == '\n' -> { endRow(); line++; rowLine = line }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || cells.isNotEmpty()) endRow()
    return rows
}

package com.yusufcanmercan.weight_track_app.data.export

import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.local.hasKnownMeasurementTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Dışa aktarılan sütunlar ve kilo biçimi uygulamanın arayüz dilinden bağımsızdır. */
internal fun writeWeightCsv(
    output: Appendable,
    measurements: List<Weight>,
    zone: TimeZone = TimeZone.getDefault(),
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = zone }
    // UTF-8 işareti, dosyayı doğrudan açan çalışma sayfalarında Türkçe karakterleri korur.
    output.append("\uFEFFdate,time,weight_kg,mood,note\r\n")
    measurements.sortedWith(compareBy(Weight::timeStamp, Weight::id)).forEach { measurement ->
        val date = Date(measurement.timeStamp)
        output.append(dateFormat.format(date)).append(',')
        if (measurement.hasKnownMeasurementTime(zone)) output.append(timeFormat.format(date))
        output.append(',').append(measurement.weight.toString()).append(',')
        output.append(csvText(measurement.mood)).append(',')
        output.append(csvText(measurement.note)).append("\r\n")
    }
}

private fun csvText(value: String?): String {
    if (value.isNullOrEmpty()) return ""
    val leading = value.trimStart { it.isWhitespace() || it.isISOControl() || it == '\uFEFF' }.firstOrNull()
    // Çalışma sayfası uygulamalarının serbest metni formül olarak çalıştırmasını önler.
    // Başında zaten kesme işareti olan metni de işaretle, böylece kodlama geri alınabilir.
    val prefix = if (value.startsWith('\'') || (leading != null && leading in "=+-@＝＋－＠")) "'" else ""
    return "\"$prefix${value.replace("\"", "\"\"")}\""
}

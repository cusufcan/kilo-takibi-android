package com.yusufcanmercan.weight_track_app.ui.home

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.theme.seyirCurve
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun shareWeeklyCard(context: Context, title: String, period: String, change: String, count: String, chart: ChartData?) {
    val uri = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "weekly-share").apply { mkdirs() }
        folder.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > DAY_MILLIS }?.forEach { it.delete() }
        val image = File.createTempFile("kilo-takibi-", ".png", folder)
        val bitmap = createBitmap(720, 1280)
        try {
            drawCard(context, Canvas(bitmap), title, period, change, count, chart)
            image.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            FileProvider.getUriForFile(context, "${context.packageName}.weekly-share", image)
        } finally {
            bitmap.recycle()
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("Weekly summary", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.weekly_share)))
}

private fun drawCard(context: Context, canvas: Canvas, title: String, period: String, change: String, count: String, chart: ChartData?) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val regular = ResourcesCompat.getFont(context, R.font.seyir_regular) ?: Typeface.DEFAULT
    val bold = ResourcesCompat.getFont(context, R.font.seyir_bold) ?: Typeface.DEFAULT_BOLD
    val ink = 0xFF22005D.toInt()
    val surfaceInk = 0xFF1D1A21.toInt()
    val muted = 0xFF4F4559.toInt()
    val primary = 0xFF6200EE.toInt()
    canvas.drawColor(0xFFFCF8FF.toInt())
    fun text(value: String, baseline: Float, size: Float, color: Int, weight: Typeface) {
        paint.typeface = weight
        paint.textSize = size
        paint.color = color
        val width = paint.measureText(value)
        if (width > 592f) paint.textSize = size * 592f / width
        canvas.drawText(value, 64f, baseline, paint)
    }
    text(context.getString(R.string.app_name), 126f, 42f, surfaceInk, bold)
    text(period, 228f, 24f, muted, regular)
    text(title, 316f, 56f, surfaceInk, bold)
    text(change, 470f, 126f, ink, bold)
    chart?.let { data ->
        paint.color = 0xFFF1ECF5.toInt()
        canvas.drawRoundRect(64f, 536f, 656f, 826f, 28f, 28f, paint)
        fun x(day: Long) = 88f + 544f * day / data.maxDay.coerceAtLeast(1)
        fun y(kg: Double): Float = 760f - (198f * (kg - data.minKg) / (data.maxKg - data.minKg)).toFloat()
        paint.color = 0xFF80758A.toInt()
        data.points.forEach { canvas.drawCircle(x(it.day), y(it.measuredKg), 5f, paint) }
        val path = Path()
        seyirCurve(data.points.map { Offset(x(it.day), y(it.trendKg)) }, path::moveTo, path::cubicTo)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 5f
        paint.color = primary
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        val locale = context.resources.configuration.locales[0]
        val datePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
        val format = SimpleDateFormat(datePattern, locale).apply { timeZone = Utc }
        paint.typeface = regular
        paint.textSize = 20f
        paint.color = muted
        canvas.drawText(format.format(Date(data.startMillis)), 88f, 798f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(format.format(Date(data.startMillis + data.maxDay * DAY_MILLIS)), 632f, 798f, paint)
        paint.textAlign = Paint.Align.LEFT
    }
    text(count, 936f, 27f, ink, bold)
    val footer = context.getString(R.string.weekly_context)
    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 28f
        typeface = regular
    }
    val lines = StaticLayout.Builder.obtain(footer, 0, footer.length, footerPaint, 470)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()
    canvas.withTranslation(64f, 1030f) { lines.draw(this) }
}

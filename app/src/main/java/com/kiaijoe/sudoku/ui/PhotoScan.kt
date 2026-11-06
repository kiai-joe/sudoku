package com.kiaijoe.sudoku.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.kiaijoe.sudoku.R
import com.kiaijoe.sudoku.engine.DigitReader
import com.kiaijoe.sudoku.engine.Glyph
import com.kiaijoe.sudoku.engine.GrayImage
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Android side of scanning a puzzle: a file for the camera app to save
 * to, turning a photo into a [GrayImage] the right way up, and reference
 * digits drawn in the phone's typefaces for the [DigitReader].
 */
object PhotoScan {
    /** The longest side a photo is scaled to before reading: plenty for a grid, and quick. */
    private const val MAX_SIDE = 1200

    /** A fresh file in the app's cache for the camera app to save its photo to, and its shareable address. */
    fun newPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        // Only the latest photo is kept.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "puzzle-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.scans", file)
    }

    /** The photo at [uri] as greys, upright and no bigger than [MAX_SIDE]; null if it can't be read. */
    fun load(context: Context, uri: Uri): GrayImage? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        gray(upright)
    }.getOrNull()

    internal fun gray(bitmap: Bitmap): GrayImage {
        val w = bitmap.width
        val h = bitmap.height
        val argb = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)
        val px = IntArray(w * h) { i ->
            val c = argb[i]
            (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)).roundToInt()
        }
        return GrayImage(w, h, px)
    }

    /** [image] as a bitmap, to show next to the board. */
    fun bitmap(image: GrayImage): Bitmap {
        val argb = IntArray(image.pixels.size) { i -> image.pixels[i].let { Color.rgb(it, it, it) } }
        return Bitmap.createBitmap(argb, image.width, image.height, Bitmap.Config.ARGB_8888)
    }

    @Volatile private var reader: DigitReader? = null

    /**
     * A reader that knows digits 1 to 9 in the faces puzzles are usually
     * printed in: the phone's sans serif, serif and monospaced, plain and
     * bold, and Sumi's own Sora and Inter. Made once, then kept.
     */
    fun reader(context: Context): DigitReader = reader ?: synchronized(this) {
        reader ?: build(context).also { reader = it }
    }

    private fun build(context: Context): DigitReader {
        val faces = buildList {
            for (family in listOf(Typeface.SANS_SERIF, Typeface.SERIF, Typeface.MONOSPACE)) {
                add(Typeface.create(family, Typeface.NORMAL))
                add(Typeface.create(family, Typeface.BOLD))
            }
            for (font in listOf(R.font.sora_regular, R.font.sora_semibold, R.font.inter_regular, R.font.inter_semibold)) {
                ResourcesCompat.getFont(context, font)?.let(::add)
            }
        }
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        val refs = buildList {
            for (face in faces) {
                paint.typeface = face
                for (d in 1..9) {
                    canvas.drawColor(Color.WHITE)
                    canvas.drawText(d.toString(), size / 2f, size * 0.75f, paint)
                    Glyph.of(gray(bitmap))?.let { add(d to it) }
                }
            }
        }
        bitmap.recycle()
        return DigitReader(refs)
    }
}

package com.kiaijoe.sudoku.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** A greyscale picture: [pixels] row by row, 0 black to 255 white. */
class GrayImage(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(pixels.size == width * height)
    }

    operator fun get(x: Int, y: Int) = pixels[y * width + x]

    /** The grey at (x, y) between pixel centres, blended from the four nearest; edges clamp. */
    fun sample(x: Float, y: Float): Float {
        val fx = (x - 0.5f).coerceIn(0f, (width - 1).toFloat())
        val fy = (y - 0.5f).coerceIn(0f, (height - 1).toFloat())
        val x0 = fx.toInt()
        val y0 = fy.toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val ax = fx - x0
        val ay = fy - y0
        val top = this[x0, y0] * (1 - ax) + this[x1, y0] * ax
        val bottom = this[x0, y1] * (1 - ax) + this[x1, y1] * ax
        return top * (1 - ay) + bottom * ay
    }
}

/**
 * A digit's shape made comparable: its ink scaled to fit a [SIZE]×[SIZE]
 * box, keeping its proportions (so a 1 stays narrow), centred and softened
 * a little so small shifts don't matter.
 */
object Glyph {
    const val SIZE = 16

    /** The glyph of the ink in [mask] ([w]×[h], true for ink), or null if there is none. */
    fun of(mask: BooleanArray, w: Int, h: Int): FloatArray? {
        var x0 = w
        var y0 = h
        var x1 = -1
        var y1 = -1
        for (y in 0 until h) for (x in 0 until w) {
            if (mask[y * w + x]) {
                x0 = min(x0, x); x1 = max(x1, x)
                y0 = min(y0, y); y1 = max(y1, y)
            }
        }
        if (x1 < 0) return null
        val bw = x1 - x0 + 1
        val bh = y1 - y0 + 1
        val box = SIZE - 2
        val scale = box.toFloat() / max(bw, bh)
        val ox = (SIZE - bw * scale) / 2f
        val oy = (SIZE - bh * scale) / 2f
        val out = FloatArray(SIZE * SIZE)
        // Four by four samples per output pixel, so thin strokes still register when scaling down.
        val n = 4
        for (ty in 0 until SIZE) for (tx in 0 until SIZE) {
            var ink = 0
            for (sy in 0 until n) for (sx in 0 until n) {
                val px = ((tx + (sx + 0.5f) / n - ox) / scale).toInt() + x0
                val py = ((ty + (sy + 0.5f) / n - oy) / scale).toInt() + y0
                if (px in x0..x1 && py in y0..y1 && mask[py * w + px]) ink++
            }
            out[ty * SIZE + tx] = ink.toFloat() / (n * n)
        }
        return blur(out)
    }

    /** The glyph of all the dark ink in [image], for drawing reference digits. */
    fun of(image: GrayImage): FloatArray? =
        of(BooleanArray(image.pixels.size) { image.pixels[it] < 128 }, image.width, image.height)

    private fun blur(v: FloatArray): FloatArray {
        val out = FloatArray(v.size)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            var sum = 0f
            var count = 0
            for (dy in -1..1) for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until SIZE && ny in 0 until SIZE) {
                    // The centre counts double, so the shape stays crisp.
                    val weight = if (dx == 0 && dy == 0) 2 else 1
                    sum += v[ny * SIZE + nx] * weight
                    count += weight
                }
            }
            out[y * SIZE + x] = sum / count
        }
        return out
    }
}

/**
 * Reads a printed digit by finding the reference glyph it looks most like.
 * [references] are digits 1 to 9 drawn in several typefaces.
 */
class DigitReader(references: List<Pair<Int, FloatArray>>) {
    /** The best [digit], how alike it is (1 the same), and its lead over the best other digit. */
    data class Reading(val digit: Int, val score: Float, val margin: Float)

    private val refs = references.map { (d, g) -> d to centred(g) }

    init {
        require(refs.isNotEmpty())
    }

    fun read(glyph: FloatArray): Reading {
        val g = centred(glyph)
        val best = FloatArray(10) { -1f }
        for ((d, r) in refs) {
            var dot = 0f
            for (i in g.indices) dot += g[i] * r[i]
            if (dot > best[d]) best[d] = dot
        }
        val order = (1..9).sortedByDescending { best[it] }
        return Reading(order[0], best[order[0]], best[order[0]] - best[order[1]])
    }

    /** [v] less its mean, scaled to length 1, so a dot product is the correlation. */
    private fun centred(v: FloatArray): FloatArray {
        val mean = v.average().toFloat()
        val out = FloatArray(v.size) { v[it] - mean }
        val len = sqrt(out.fold(0f) { s, x -> s + x * x })
        if (len > 0f) for (i in out.indices) out[i] /= len
        return out
    }
}

/**
 * Reads a Sudoku from a photo: finds the grid, straightens it, and reads
 * the digit in each cell. Printed puzzles read best; the player checks the
 * result, helped by [Result.unsure].
 */
object Scanner {
    /** Pixels per cell in the straightened grid. */
    const val CELL = 36
    const val SIZE = CELL * 9

    /** How much darker than its surroundings a pixel of the straightened grid must be to count as ink. */
    const val INK_OFFSET = 12

    /** Below this likeness, or with less of a lead over the next digit, a reading is marked unsure. */
    private const val SURE_SCORE = 0.6f
    private const val SURE_MARGIN = 0.05f

    /**
     * What was read: [digits] for the 81 cells (0 empty), the cells whose
     * reading is [unsure], and the straightened [grid] to compare against.
     */
    class Result(val digits: IntArray, val unsure: Set<Int>, val grid: GrayImage)

    /** Reads the puzzle in [image], or null if no grid can be found. */
    fun scan(image: GrayImage, reader: DigitReader): Result? {
        val corners = findGrid(image) ?: return null
        val grid = straighten(image, corners)
        val mask = threshold(grid, radius = CELL / 2, offset = INK_OFFSET)
        eraseLines(mask)
        val digits = IntArray(Grid.CELLS)
        val unsure = HashSet<Int>()
        for (cell in 0 until Grid.CELLS) {
            val glyph = cellGlyph(mask, Grid.row(cell), Grid.col(cell)) ?: continue
            val reading = reader.read(glyph)
            digits[cell] = reading.digit
            if (reading.score < SURE_SCORE || reading.margin < SURE_MARGIN) unsure += cell
        }
        return Result(digits, unsure, grid)
    }

    /** A point in the photo. */
    data class Point(val x: Float, val y: Float)

    /**
     * The grid's corners (top left, top right, bottom right, bottom left):
     * the outermost points of the largest roughly square shape of connected
     * dark lines.
     */
    fun findGrid(image: GrayImage): List<Point>? {
        val w = image.width
        val h = image.height
        val dark = threshold(image, radius = max(4, min(w, h) / 32), offset = 10)
        val seen = BooleanArray(w * h)
        val stack = IntArray(w * h)
        var best: List<Point>? = null
        var bestArea = 0L
        val minSide = min(w, h) * 0.25f
        for (start in dark.indices) {
            if (!dark[start] || seen[start]) continue
            // Walk one connected shape, keeping its bounds and four extreme corners.
            var top = 0
            stack[top++] = start
            seen[start] = true
            var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
            var tl = start; var tr = start; var br = start; var bl = start
            fun sum(i: Int) = i % w + i / w
            fun diff(i: Int) = i % w - i / w
            while (top > 0) {
                val i = stack[--top]
                val x = i % w
                val y = i / w
                x0 = min(x0, x); x1 = max(x1, x); y0 = min(y0, y); y1 = max(y1, y)
                if (sum(i) < sum(tl)) tl = i
                if (sum(i) > sum(br)) br = i
                if (diff(i) > diff(tr)) tr = i
                if (diff(i) < diff(bl)) bl = i
                if (x > 0) top = push(i - 1, dark, seen, stack, top)
                if (x < w - 1) top = push(i + 1, dark, seen, stack, top)
                if (y > 0) top = push(i - w, dark, seen, stack, top)
                if (y < h - 1) top = push(i + w, dark, seen, stack, top)
            }
            val bw = x1 - x0 + 1
            val bh = y1 - y0 + 1
            if (bw < minSide || bh < minSide) continue
            val aspect = bw.toFloat() / bh
            if (aspect < 0.6f || aspect > 1.6f) continue
            val area = bw.toLong() * bh
            if (area > bestArea) {
                bestArea = area
                best = listOf(tl, tr, br, bl).map { Point(it % w + 0.5f, it / w + 0.5f) }
            }
        }
        return best
    }

    private fun push(i: Int, dark: BooleanArray, seen: BooleanArray, stack: IntArray, top: Int): Int {
        if (!dark[i] || seen[i]) return top
        seen[i] = true
        stack[top] = i
        return top + 1
    }

    /** The grid inside [corners], squared up to [SIZE]×[SIZE] pixels. */
    fun straighten(image: GrayImage, corners: List<Point>): GrayImage {
        val map = squareTo(corners)
        val pixels = IntArray(SIZE * SIZE)
        for (j in 0 until SIZE) for (i in 0 until SIZE) {
            val (x, y) = map((i + 0.5f) / SIZE, (j + 0.5f) / SIZE)
            pixels[j * SIZE + i] = image.sample(x, y).toInt()
        }
        return GrayImage(SIZE, SIZE, pixels)
    }

    /**
     * The perspective map from the unit square onto the four [corners]
     * (top left, top right, bottom right, bottom left), after Heckbert.
     */
    private fun squareTo(corners: List<Point>): (Float, Float) -> Pair<Float, Float> {
        val (p0, p1, p2, p3) = corners
        val sx = p0.x - p1.x + p2.x - p3.x
        val sy = p0.y - p1.y + p2.y - p3.y
        val g: Float
        val h: Float
        if (abs(sx) < 1e-3f && abs(sy) < 1e-3f) {
            g = 0f; h = 0f
        } else {
            val dx1 = p1.x - p2.x
            val dx2 = p3.x - p2.x
            val dy1 = p1.y - p2.y
            val dy2 = p3.y - p2.y
            val det = dx1 * dy2 - dx2 * dy1
            g = (sx * dy2 - dx2 * sy) / det
            h = (dx1 * sy - sx * dy1) / det
        }
        val a = p1.x - p0.x + g * p1.x
        val b = p3.x - p0.x + h * p3.x
        val d = p1.y - p0.y + g * p1.y
        val e = p3.y - p0.y + h * p3.y
        return { u, v ->
            val z = g * u + h * v + 1f
            ((a * u + b * v + p0.x) / z) to ((d * u + e * v + p0.y) / z)
        }
    }

    /**
     * Which pixels are ink: darker by [offset] than the average of the
     * square of [radius] around them, so shadows and uneven light don't
     * turn whole areas black.
     */
    fun threshold(image: GrayImage, radius: Int, offset: Int): BooleanArray {
        val w = image.width
        val h = image.height
        val sums = LongArray((w + 1) * (h + 1))
        for (y in 0 until h) {
            var row = 0L
            for (x in 0 until w) {
                row += image[x, y]
                sums[(y + 1) * (w + 1) + x + 1] = sums[y * (w + 1) + x + 1] + row
            }
        }
        val out = BooleanArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val ax = max(0, x - radius); val bx = min(w, x + radius + 1)
            val ay = max(0, y - radius); val by = min(h, y + radius + 1)
            val total = sums[by * (w + 1) + bx] - sums[ay * (w + 1) + bx] - sums[by * (w + 1) + ax] + sums[ay * (w + 1) + ax]
            val mean = total.toFloat() / ((bx - ax) * (by - ay))
            out[y * w + x] = image[x, y] < mean - offset
        }
        return out
    }

    /**
     * Rubs out the grid lines in the straightened [mask]: any straight run
     * of ink across or down longer than most of a cell, which no digit has.
     * Lines that wander into a cell would otherwise join its digit.
     */
    private fun eraseLines(mask: BooleanArray) {
        val long = (CELL * 0.8f).toInt()
        val lines = BooleanArray(mask.size)
        for (horizontal in listOf(true, false)) {
            for (a in 0 until SIZE) {
                var run = 0
                for (b in 0..SIZE) {
                    val i = if (horizontal) a * SIZE + b else b * SIZE + a
                    if (b < SIZE && mask[i]) {
                        run++
                        continue
                    }
                    if (run >= long) {
                        for (k in b - run until b) lines[if (horizontal) a * SIZE + k else k * SIZE + a] = true
                    }
                    run = 0
                }
            }
        }
        // A pixel either side too, for the soft edges of the line.
        for (i in lines.indices) if (lines[i]) {
            val x = i % SIZE
            val y = i / SIZE
            for (dy in -1..1) for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until SIZE && ny in 0 until SIZE) mask[ny * SIZE + nx] = false
            }
        }
    }

    /**
     * The glyph in the cell at [row], [col] of the straightened grid's ink
     * [mask], or null when the cell is empty. The cell's edges are skipped,
     * and so are long thin marks left by the grid lines.
     */
    private fun cellGlyph(mask: BooleanArray, row: Int, col: Int): FloatArray? {
        val margin = (CELL * 0.14f).toInt()
        val inner = CELL - 2 * margin
        val cx = col * CELL + margin
        val cy = row * CELL + margin
        val local = BooleanArray(inner * inner) { mask[(cy + it / inner) * SIZE + cx + it % inner] }
        val seen = BooleanArray(local.size)
        val keep = BooleanArray(local.size)
        val stack = IntArray(local.size)
        val members = IntArray(local.size)
        val pieces = BooleanArray(local.size)
        var hasCore = false
        val centreLo = inner * 0.25f
        val centreHi = inner * 0.75f
        for (start in local.indices) {
            if (!local[start] || seen[start]) continue
            var top = 0
            var count = 0
            stack[top++] = start
            seen[start] = true
            var x0 = inner; var y0 = inner; var x1 = -1; var y1 = -1
            while (top > 0) {
                val i = stack[--top]
                members[count++] = i
                val x = i % inner
                val y = i / inner
                x0 = min(x0, x); x1 = max(x1, x); y0 = min(y0, y); y1 = max(y1, y)
                if (x > 0) top = push(i - 1, local, seen, stack, top)
                if (x < inner - 1) top = push(i + 1, local, seen, stack, top)
                if (y > 0) top = push(i - inner, local, seen, stack, top)
                if (y < inner - 1) top = push(i + inner, local, seen, stack, top)
            }
            val bw = x1 - x0 + 1
            val bh = y1 - y0 + 1
            if (count < inner * inner * 0.01f) continue
            // A leftover grid line: long and thin, along the edge.
            if ((bw > inner * 0.7f && bh < inner * 0.2f) || (bh > inner * 0.85f && bw < inner * 0.15f &&
                    (x0 == 0 || x1 == inner - 1))) continue
            // The digit reaches the middle of the cell. Other pieces are kept apart for now: they may
            // be parts of it broken off by blur, or smudges.
            val central = !(x1 < centreLo || x0 > centreHi || y1 < centreLo || y0 > centreHi)
            for (k in 0 until count) (if (central) keep else pieces)[members[k]] = true
            if (central) hasCore = true
        }
        // With the digit found, pieces beside it are part of it.
        if (hasCore) for (i in pieces.indices) if (pieces[i]) keep[i] = true
        var ink = 0
        var y0 = inner; var y1 = -1
        for (i in keep.indices) if (keep[i]) {
            ink++
            y0 = min(y0, i / inner); y1 = max(y1, i / inner)
        }
        if (ink < inner * inner * 0.03f || y1 - y0 + 1 < inner * 0.35f) return null
        return Glyph.of(keep, inner, inner)
    }
}

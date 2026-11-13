/*
 * UniBoard — Unicode 输入法
 * Copyright (C) 2026 UniBoard contributors
 *
 * 本程序是自由软件：你可以依据 GNU 通用公共许可证第 3 版
 * 或（你选择的）任何更新版本所规定的条款，重新分发和/或修改它。
 *
 * 本程序基于"希望它有用"的目的分发，但不提供任何担保，
 * 也不承诺适用于特定用途。完整条款见项目根目录的 LICENSE 文件。
 */

package org.YavSoft.UniBoard.unicode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import org.YavSoft.UniBoard.R
import org.YavSoft.UniBoard.core.Prefs
import org.YavSoft.UniBoard.core.dp
import org.YavSoft.UniBoard.core.sp
import org.YavSoft.UniBoard.font.FontResolver
import org.YavSoft.UniBoard.ui.theme.KeyboardTheme
import org.YavSoft.UniBoard.ui.theme.KeyboardThemes
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Unicode 字符面板：**索引浏览**。
 *
 * 与上一版的区别：
 * - 去掉搜索框。检索改成「索引」：先选分组，再选区块，然后按格取字，
 *   全部分类与区块都来自 [UnicodeRepository.BLOCKS]（按 Unicode 17.0 整理）；
 * - 每格字符下面标出对应码点（如 4E00），一眼能看出编码；
 * - 默认显示最近使用，可随时切回索引。
 */
class UnicodePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onPick(text: String)
        fun onClose()
        /** 兼容旧接口：本版没有搜索框，永远不会被调用 */
        fun onSearchFocus(active: Boolean) {}
    }

    var listener: Listener? = null

    var theme: KeyboardTheme = KeyboardThemes.resolve(context, Prefs(context))
        set(value) {
            field = value
            invalidate()
        }

    private val fontResolver by lazy { FontResolver.get(context) }
    private val recents = RecentChars(context).apply { load() }

    /** 当前是否处于索引（区块目录）模式 */
    private var indexing = false
    /** -1 表示最近使用 */
    private var blockIndex = -1
    private var results: List<Int> = emptyList()

    private var scrollY = 0f
    private var touchDownY = 0f
    private var dragging = false
    private var pressedCell = -1
    private var handleDragging = false
    private var handleStartY = 0f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = context.sp(15f)
    }
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = context.sp(21f)
    }
    private val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = context.sp(8.5f)
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = context.sp(12f)
    }
    private val baseTypeface: Typeface = Typeface.DEFAULT
    private val tmpRect = RectF()
    private val drawableCache = HashMap<Long, android.graphics.drawable.Drawable>(4)

    /** 兼容旧接口：本版没有搜索框 */
    var searchActive: Boolean = false

    private val handleHeight: Float get() = context.dp(22f)
    private val headerTop: Float get() = handleHeight + context.dp(2f)
    private val headerHeight: Float get() = context.dp(36f)
    private val statusTop: Float get() = headerTop + headerHeight
    private val statusHeight: Float get() = context.dp(24f)
    private val gridTop: Float get() = statusTop + statusHeight + context.dp(2f)
    private val cellSize: Float get() = context.dp(56f)
    private val indexRowHeight: Float get() = context.dp(40f)
    private val columns: Int get() = max(1, (width / cellSize).toInt())

    private val closeRect = RectF()
    private val indexRect = RectF()
    private val prevRect = RectF()
    private val nextRect = RectF()

    init {
        isClickable = true
        refreshBlock()
    }

    // ---- 兼容旧接口：不再接收键盘输入 ----
    fun appendText(text: String) = Unit

    fun backspace() = Unit

    fun clearQuery() = Unit

    private fun refreshBlock() {
        indexing = false
        if (blockIndex < 0) {
            results = recents.list(96)
        } else {
            results = UnicodeRepository.charsInBlock(UnicodeRepository.BLOCKS[blockIndex])
        }
        scrollY = 0f
        invalidate()
    }

    private fun maxScroll(): Float {
        if (results.isEmpty()) return 0f
        val rows = ceil(results.size / columns.toFloat()).toInt()
        val contentHeight = rows * cellSize
        val viewHeight = height - gridTop
        return max(0f, contentHeight - viewHeight)
    }

    private fun maxIndexScroll(): Float {
        val rows = indexRows()
        val viewHeight = height - gridTop
        return max(0f, rows * indexRowHeight - viewHeight)
    }

    /** 索引模式的行数：分组标题各占一行 */
    private fun indexRows(): Int {
        var n = 0
        for (g in UnicodeRepository.GROUPS) {
            n += 1 + UnicodeRepository.blocksInGroup(g).size
        }
        return n
    }

    override fun onDraw(canvas: Canvas) {
        fillPaint.color = theme.keyboardBackground
        val r = context.dp(18f)
        tmpRect.set(0f, 0f, width.toFloat(), height.toFloat() + r)
        canvas.drawRoundRect(tmpRect, r, r, fillPaint)
        fillPaint.color = theme.hintText
        val hw = context.dp(38f)
        val hh = context.dp(4f)
        tmpRect.set((width - hw) / 2f, context.dp(8f), (width + hw) / 2f, context.dp(8f) + hh)
        canvas.drawRoundRect(tmpRect, hh / 2f, hh / 2f, fillPaint)

        drawHeader(canvas)
        drawStatus(canvas)
        if (indexing) drawIndex(canvas) else drawGrid(canvas)
    }

    private fun drawHeader(canvas: Canvas) {
        val fm = textPaint.fontMetrics
        val baseline = headerTop + headerHeight / 2f - (fm.ascent + fm.descent) / 2f

        textPaint.color = theme.textPrimary
        textPaint.textSize = context.sp(15f)
        canvas.drawText("Unicode 字符", context.dp(16f), baseline, textPaint)

        // 索引 / 最近 切换
        val label = if (indexing) "最近" else "索引"
        smallPaint.textSize = context.sp(12f)
        val tw = smallPaint.measureText(label)
        indexRect.set(
            width - context.dp(52f) - tw - context.dp(20f), headerTop + context.dp(5f),
            width - context.dp(52f), headerTop + headerHeight - context.dp(5f)
        )
        fillPaint.color = if (indexing) theme.accent else theme.keyBackground
        canvas.drawRoundRect(indexRect, context.dp(12f), context.dp(12f), fillPaint)
        smallPaint.color = if (indexing) theme.onAccent else theme.textSecondary
        val ifm = smallPaint.fontMetrics
        canvas.drawText(
            label, indexRect.centerX() - tw / 2f,
            indexRect.centerY() - (ifm.ascent + ifm.descent) / 2f, smallPaint
        )

        // 关闭
        closeRect.set(width - context.dp(48f), headerTop, width - context.dp(10f), headerTop + headerHeight)
        val d = iconDrawable(R.drawable.ic_close)
        if (d != null) {
            val size = context.dp(18f)
            val cx = closeRect.centerX()
            val cy = closeRect.centerY()
            d.setBounds(
                (cx - size / 2f).toInt(), (cy - size / 2f).toInt(),
                (cx + size / 2f).toInt(), (cy + size / 2f).toInt()
            )
            d.draw(canvas)
        }
    }

    private fun drawStatus(canvas: Canvas) {
        val btn = context.dp(26f)
        prevRect.set(context.dp(4f), statusTop, context.dp(4f) + btn, statusTop + statusHeight)
        nextRect.set(width - btn - context.dp(4f), statusTop, width - context.dp(4f), statusTop + statusHeight)
        val fm = smallPaint.fontMetrics
        val baseline = statusTop + statusHeight / 2f - (fm.ascent + fm.descent) / 2f
        smallPaint.textSize = context.sp(12f)

        val showArrows = !indexing
        if (showArrows) {
            smallPaint.color = theme.textSecondary
            smallPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("\u25C0", prevRect.centerX(), baseline, smallPaint)
            canvas.drawText("\u25B6", nextRect.centerX(), baseline, smallPaint)
            smallPaint.textAlign = Paint.Align.LEFT
        }

        smallPaint.color = theme.hintText
        val left = if (showArrows) prevRect.right + context.dp(6f) else context.dp(16f)
        val title = when {
            indexing -> "索引 · 共 ${UnicodeRepository.BLOCKS.size} 个区块 / ${UnicodeRepository.GROUPS.size} 个分组"
            blockIndex < 0 -> if (results.isEmpty()) "最近使用为空，点右上「索引」浏览全部区块" else "最近使用"
            else -> {
                val b = UnicodeRepository.BLOCKS[blockIndex]
                "${b.name}  ${UnicodeRepository.hexLabel(b.start)}–${UnicodeRepository.hexLabel(b.end)}"
            }
        }
        canvas.drawText(title, left, baseline, smallPaint)
    }

    private fun drawIndex(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(0f, gridTop, width.toFloat(), height.toFloat())
        var row = 0
        val fm = smallPaint.fontMetrics
        for (group in UnicodeRepository.GROUPS) {
            val y = gridTop + row * indexRowHeight - scrollY
            row++
            if (y + indexRowHeight > gridTop && y < height) {
                smallPaint.color = theme.accent
                smallPaint.textSize = context.sp(11f)
                val gfm = smallPaint.fontMetrics
                canvas.drawText(
                    group, context.dp(16f),
                    y + indexRowHeight - context.dp(10f) - gfm.descent, smallPaint
                )
            }
            for (block in UnicodeRepository.blocksInGroup(group)) {
                val rowY = gridTop + row * indexRowHeight - scrollY
                row++
                if (rowY + indexRowHeight < gridTop || rowY > height) continue
                val idx = UnicodeRepository.BLOCKS.indexOf(block)
                if (idx == pressedCell) {
                    tmpRect.set(
                        context.dp(8f), rowY + context.dp(2f),
                        width - context.dp(8f), rowY + indexRowHeight - context.dp(2f)
                    )
                    fillPaint.color = theme.candidateSelectedBackground
                    canvas.drawRoundRect(tmpRect, context.dp(8f), context.dp(8f), fillPaint)
                }
                smallPaint.textSize = context.sp(13f)
                smallPaint.color = theme.textPrimary
                val sfm = smallPaint.fontMetrics
                val base = rowY + indexRowHeight / 2f - (sfm.ascent + sfm.descent) / 2f
                canvas.drawText(block.name, context.dp(20f), base, smallPaint)
                smallPaint.textSize = context.sp(11f)
                smallPaint.color = theme.hintText
                val range = UnicodeRepository.hexLabel(block.start) + "–" + UnicodeRepository.hexLabel(block.end)
                val rw = smallPaint.measureText(range)
                canvas.drawText(range, width - context.dp(20f) - rw, base, smallPaint)
            }
        }
        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas) {
        if (results.isEmpty()) return
        val cols = columns
        val cell = cellSize
        canvas.save()
        canvas.clipRect(0f, gridTop, width.toFloat(), height.toFloat())
        val gfm = glyphPaint.fontMetrics
        for (i in results.indices) {
            val row = i / cols
            val col = i % cols
            val x = col * cell
            val y = gridTop + row * cell - scrollY
            if (y + cell < gridTop || y > height) continue
            if (i == pressedCell) {
                tmpRect.set(
                    x + context.dp(2f), y + context.dp(2f),
                    x + cell - context.dp(2f), y + cell - context.dp(2f)
                )
                fillPaint.color = theme.candidateSelectedBackground
                canvas.drawRoundRect(tmpRect, context.dp(8f), context.dp(8f), fillPaint)
            }
            val cp = results[i]
            val s = UnicodeRepository.charOf(cp)
            if (s.isEmpty()) continue
            val w = fontResolver.measure(s, glyphPaint, baseTypeface)
            val glyphBaseline = y + cell * 0.58f - (gfm.ascent + gfm.descent) / 2f
            glyphPaint.color = theme.textPrimary
            fontResolver.draw(canvas, s, x + (cell - w) / 2f, glyphBaseline, glyphPaint, baseTypeface)

            // 字符下面标出码点，方便直接读编码
            val code = cp.toString(16).uppercase().padStart(4, '0')
            codePaint.color = theme.hintText
            val cw = codePaint.measureText(code)
            val cfm = codePaint.fontMetrics
            canvas.drawText(code, x + (cell - cw) / 2f, y + cell - context.dp(6f) - cfm.descent, codePaint)
        }
        canvas.restore()
    }

    private fun iconDrawable(resId: Int): android.graphics.drawable.Drawable? {
        val color = theme.textSecondary
        val key = (resId.toLong() shl 32) or (color.toLong() and 0xFFFFFFFFL)
        drawableCache[key]?.let { return it }
        val d = ContextCompat.getDrawable(context, resId)?.mutate() ?: return null
        DrawableCompat.setTint(d, color)
        drawableCache[key] = d
        return d
    }

    private fun hitCell(x: Float, y: Float): Int {
        if (y < gridTop) return -1
        val cols = columns
        val localY = y + scrollY - gridTop
        val row = (localY / cellSize).toInt()
        val col = (x / cellSize).toInt()
        if (col < 0 || col >= cols) return -1
        val index = row * cols + col
        return if (index in results.indices) index else -1
    }

    /** 索引模式下命中的是 BLOCKS 下标 */
    private fun hitIndexRow(y: Float): Int {
        if (y < gridTop) return -1
        val local = y + scrollY - gridTop
        val row = (local / indexRowHeight).toInt()
        var cursor = 0
        for (group in UnicodeRepository.GROUPS) {
            if (row == cursor) return -1      // 分组标题行
            cursor++
            for (block in UnicodeRepository.blocksInGroup(group)) {
                if (row == cursor) return UnicodeRepository.BLOCKS.indexOf(block)
                cursor++
            }
        }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = event.y
                dragging = false
                if (event.y < handleHeight) {
                    handleDragging = true
                    handleStartY = event.y
                    return true
                }
                pressedCell = if (indexing) hitIndexRow(event.y) else hitCell(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (handleDragging) {
                    translationY = (event.y - handleStartY).coerceAtLeast(0f)
                    return true
                }
                if (!dragging && abs(event.y - touchDownY) > context.dp(6f)) {
                    dragging = true
                    pressedCell = -1
                }
                if (dragging) {
                    scrollY -= event.y - touchDownY
                    val limit = if (indexing) maxIndexScroll() else maxScroll()
                    scrollY = scrollY.coerceIn(0f, limit)
                    touchDownY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (handleDragging) {
                    handleDragging = false
                    if (translationY > context.dp(56f)) {
                        listener?.onClose()
                    } else {
                        animate().translationY(0f).setDuration(120).start()
                    }
                    return true
                }
                if (!dragging) {
                    when {
                        closeRect.contains(event.x, event.y) -> listener?.onClose()
                        indexRect.contains(event.x, event.y) -> {
                            indexing = !indexing
                            scrollY = 0f
                            pressedCell = -1
                            invalidate()
                        }
                        !indexing && prevRect.contains(event.x, event.y) -> switchBlock(-1)
                        !indexing && nextRect.contains(event.x, event.y) -> switchBlock(1)
                        indexing && pressedCell >= 0 -> {
                            blockIndex = pressedCell
                            indexing = false
                            refreshBlock()
                        }
                        pressedCell >= 0 -> {
                            val cp = results[pressedCell]
                            recents.add(cp)
                            listener?.onPick(UnicodeRepository.charOf(cp))
                        }
                    }
                }
                pressedCell = -1
                dragging = false
                invalidate()
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedCell = -1
                dragging = false
                handleDragging = false
                translationY = 0f
                invalidate()
            }
        }
        return true
    }

    private fun switchBlock(delta: Int) {
        val n = UnicodeRepository.BLOCKS.size
        blockIndex = if (blockIndex < 0) {
            if (delta > 0) 0 else n - 1
        } else {
            (blockIndex + delta + n) % n
        }
        indexing = false
        refreshBlock()
    }

    fun animateIn() {
        animate().cancel()
        alpha = 0f
        translationY = context.dp(14f)
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(170)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun animateOut() {
        animate().cancel()
        animate()
            .alpha(0f)
            .translationY(context.dp(10f))
            .setDuration(120)
            .start()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

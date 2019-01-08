package me.kanmodel.nov18.ui

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2019-01-08-15:36
 */

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Rectangle

//TEXTAREA 行号显示插件
class LineNumberHeaderView : javax.swing.JComponent() {
    //    private final  Font DEFAULT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private val DEFAULT_FONT = Font("Monospaced", 1, 20)
    val DEFAULT_BACKGROUD = Color(228, 228, 228)
    val DEFAULT_FOREGROUD = Color.BLACK
    val nHEIGHT = Integer.MAX_VALUE - 1000000
    val MARGIN = 5
    private var lineHeight: Int = 0
    private var fontLineHeight: Int = 0
    private var currentRowWidth: Int = 0
    private var fontMetrics: FontMetrics? = null

    val startOffset: Int
        get() = 4

    init {
        font = DEFAULT_FONT
        foreground = DEFAULT_FOREGROUD
        background = DEFAULT_BACKGROUD
        setPreferredSize(9999)
    }

    fun setPreferredSize(row: Int) {
        val width = fontMetrics!!.stringWidth(row.toString())
        println("width$width")
        if (currentRowWidth < width) {
            currentRowWidth = width
            preferredSize = Dimension(2 * MARGIN + width + 1, nHEIGHT)
        }
    }

    override fun setFont(font: Font) {
        super.setFont(font)
        fontMetrics = getFontMetrics(getFont())
        fontLineHeight = fontMetrics!!.height
    }

    fun getLineHeight(): Int {
        return if (lineHeight == 0) {
            fontLineHeight
        } else lineHeight
    }

    fun setLineHeight(lineHeight: Int) {
        if (lineHeight > 0) {
            this.lineHeight = lineHeight
        }
    }

    override fun paintComponent(g: Graphics) {
        val nlineHeight = getLineHeight()
        val startOffset = startOffset
        val drawHere = g.clipBounds
        g.color = background
        g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height)
        g.color = foreground
        val startLineNum = drawHere.y / nlineHeight + 1
        val endLineNum = startLineNum + drawHere.height / nlineHeight
        var start = drawHere.y / nlineHeight * nlineHeight + nlineHeight - startOffset
        for (i in startLineNum..endLineNum) {
            val lineNum = i.toString()
            val width = fontMetrics!!.stringWidth(lineNum)
            g.drawString("$lineNum ", MARGIN + currentRowWidth - width - 1, start)
            start += nlineHeight
        }
        setPreferredSize(endLineNum)
    }

    companion object {

        /**
         * JAVA TextArea行数显示插件
         */
        private val serialVersionUID = 1L
    }
}

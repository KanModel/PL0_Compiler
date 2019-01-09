package me.kanmodel.nov18.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.SwingUtilities

class LineNumberHeaderView : javax.swing.JComponent() {
    private val defaultFont = Font("Monospaced", 1, 20)
    private val defaultBackground = Color(228, 228, 228)
    private val defaultForeground = Color.BLACK!!
    private var lineHeight: Int = 0
    private var fontLineHeight: Int = 0
    private var currentRowWidth: Int = 0
    private var fontMetrics: FontMetrics? = null

    val startOffset: Int
        get() = 4

    init {
        SwingUtilities.invokeLater {
            font = defaultFont
            foreground = defaultForeground
            background = defaultBackground
            setPreferredSize(10)
        }
    }

    private fun setPreferredSize(row: Int) {
        SwingUtilities.invokeLater {
            val width = fontMetrics!!.stringWidth(row.toString())
            if (currentRowWidth < width) {
                currentRowWidth = width
                preferredSize = Dimension(2 * MARGIN + width + 1, nHEIGHT)
            }
        }
    }

    override fun setFont(font: Font) {
        super.setFont(font)
        fontMetrics = getFontMetrics(getFont())
        fontLineHeight = fontMetrics!!.height
    }

    private fun getLineHeight(): Int {
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
        val nLineHeight = getLineHeight()
        val startOffset = startOffset
        val drawHere = g.clipBounds
        g.color = background
        g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height)
        g.color = foreground
        val startLineNum = drawHere.y / nLineHeight + 1
        val endLineNum = startLineNum + drawHere.height / nLineHeight
        var start = drawHere.y / nLineHeight * nLineHeight + nLineHeight - startOffset
        for (i in startLineNum..endLineNum) {
            val lineNum = i.toString()
            val width = fontMetrics!!.stringWidth(lineNum)
            g.drawString("$lineNum ", MARGIN + currentRowWidth - width - 1, start)
            start += nLineHeight
        }
        setPreferredSize(endLineNum)
    }

    companion object {
        private const val serialVersionUID = 1L
        const val MARGIN = 5
        const val nHEIGHT = Integer.MAX_VALUE - 1000000
    }
}

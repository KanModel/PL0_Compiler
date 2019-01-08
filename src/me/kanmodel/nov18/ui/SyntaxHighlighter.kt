package me.kanmodel.nov18.ui

import java.awt.Color
import java.awt.Font
import java.util.HashSet
import javax.swing.JFrame
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.*

/**
 * @description: 高亮渲染类
 * @author: KanModel
 * @create: 2018-11-20 18:14
 */
class SyntaxHighlighter(editor: JTextPane) : DocumentListener {
    private val commentStarts = arrayListOf<Int>()
    private val commentExistStarts = arrayListOf<Int>()
    private val commentExistEnds = arrayListOf<Int>()
    private val keywords: MutableSet<String>
    private val operators: MutableSet<String>
    private val symbols: MutableSet<Char>
    private val keywordStyle: Style = (editor.document as StyledDocument).addStyle("Keyword_Style", null)
    private val functionStyle: Style = (editor.document as StyledDocument).addStyle("Operator_Style", null)
    private val symbolStyle: Style = (editor.document as StyledDocument).addStyle("Symbol_Style", null)
    private val stringStyle: Style = (editor.document as StyledDocument).addStyle("String_Style", null)
    private val commentStyle: Style = (editor.document as StyledDocument).addStyle("Comment_Style", null)
    private val normalStyle: Style = (editor.document as StyledDocument).addStyle("Normal_Style", null)

    init {
        // 准备着色使用的样式
        StyleConstants.setForeground(keywordStyle, Color.ORANGE)
        StyleConstants.setForeground(functionStyle, Color.CYAN)
        StyleConstants.setForeground(symbolStyle, Color.WHITE)
        StyleConstants.setForeground(stringStyle, Color.GREEN)
        StyleConstants.setForeground(commentStyle, Color.GRAY)
        StyleConstants.setForeground(normalStyle, Color.WHITE)

        // 准备关键字
        keywords = HashSet()
        keywords.add("begin")
        keywords.add("end")
        keywords.add("if")
        keywords.add("else")
        keywords.add("then")
        keywords.add("while")
        keywords.add("do")
        keywords.add("call")
        keywords.add("array")
        keywords.add("var")
        keywords.add("const")
        keywords.add("procedure")
        keywords.add("for")
        keywords.add("to")
        keywords.add("until")
        keywords.add("downto")
        keywords.add("step")
        // 准备操作相关函数
        operators = HashSet()
        operators.add("sqrt")
        operators.add("write")
        operators.add("writeln")
        operators.add("print")
        operators.add("println")
        operators.add("read")

        symbols = HashSet()
        symbols.add(':')
        symbols.add(';')
        symbols.add('=')
        symbols.add('+')
        symbols.add('-')
        symbols.add('*')
        symbols.add('/')
        symbols.add('.')
        symbols.add(',')
        symbols.add('\'')
        symbols.add('#')
        symbols.add('"')
        symbols.add('>')
        symbols.add('<')
        symbols.add('!')
        symbols.add('%')
    }

    @Throws(BadLocationException::class)
    fun colouring(doc: StyledDocument, pos: Int, len: Int) {
        // 取得插入或者删除后影响到的单词.
        // 例如"public"在b后插入一个空格, 就变成了:"pub lic", 这时就有两个单词要处理:"pub"和"lic"
        // 这时要取得的范围是pub中p前面的位置和lic中c后面的位置
        var start = indexOfWordStart(doc, pos)
        val end = indexOfWordEnd(doc, pos + len)

        var ch: Char
        while (start < end) {
            ch = getCharAt(doc, start)
            if (Character.isLetter(ch) || ch == '_') {
                // 如果是以字母或者下划线开头, 说明是单词
                // pos为处理后的最后一个下标
                start = colouringWord(doc, start)
            } else if (ch == '{' || ch == '}') {
                if (ch == '{') {
                    commentStarts.add(start)
                    commentExistStarts.add(start)
                } else {
                    if (start !in commentExistEnds) {
                        commentExistEnds.add(start)
//                        println("Ends: $commentExistEnds")
                    }
                    var short = Int.MAX_VALUE
                    var rShort = Int.MAX_VALUE
                    var shortPos = 0
//                    var rShortPos = 0
//                    println("寻找前置{ $commentStarts ccc")
                    for (c in commentExistStarts) {
                        if ((start - c) > 0) {
//                            short = min(start - c, short)
                            if ((start - c) < short) {
                                short = start - c
                                shortPos = c
                            }
                        }
                        if ((c - start) > 0) {
                            if ((c - start) < rShort) {
                                rShort = c - start
//                                rShortPos = c
                            }
                        }
                    }
                    if (short != Int.MAX_VALUE) {
//                        println("配对{ pos:$shortPos colouringComment")
                        colouringComment(doc, shortPos)
                        if (shortPos in commentStarts) {
                            commentStarts.remove(shortPos)
                        }
                    }
                    if (rShort != Int.MAX_VALUE) {
                        colouring(doc, start + 1, rShort)
                    } else {
                        colouring(doc, start + 1, doc.length - start - 1)
                    }
                }
                start = colouringComment(doc, start)
            } else if (ch in symbols) {
                var isComment = false
                commentStarts.forEach {
                    if (it < start) {
                        isComment = true
                    }
                }
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, start, 1, symbolStyle))
                } else {
                    colouringComment(doc, pos)
                }
                ++start
            } else {
                var isComment = false
                commentStarts.forEach {
                    if (it < pos) {
                        isComment = true
                    }
                }
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, start, 1, normalStyle))
                } else {
                    colouringComment(doc, pos)
                }
                ++start
            }
        }
    }

    /**
     * 对单词进行着色, 并返回单词结束的下标.
     *
     * @param doc
     * @param pos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun colouringWord(doc: StyledDocument, pos: Int): Int {
        val wordEnd = indexOfWordEnd(doc, pos)
        val word = doc.getText(pos, wordEnd - pos)

        var isComment = false
        when {
            // 如果是关键字, 就进行关键字的着色, 否则使用普通的着色.
            // 这里有一点要注意, 在insertUpdate和removeUpdate的方法调用的过程中, 不能修改doc的属性.
            // 但我们又要达到能够修改doc的属性, 所以把此任务放到这个方法的外面去执行.
            // 实现这一目的, 可以使用新线程, 但放到swing的事件队列里去处理更轻便一点.
            word in keywords -> {
                commentStarts.forEach {
                    if (it < pos) {
                        isComment = true
                    }
                }
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, keywordStyle))
                } else {
                    colouringComment(doc, pos)
                }
            }
            word in operators -> {
                commentStarts.forEach {
                    if (it < pos) {
                        isComment = true
                    }
                }
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, functionStyle))
                } else {
                    colouringComment(doc, pos)
                }
            }
            else -> {
                commentStarts.forEach {
                    if (it < pos) {
                        isComment = true
                    }
                }
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, normalStyle))
                } else {
                    colouringComment(doc, pos)
                }
            }
        }

        return wordEnd
    }

    /**
     * 对注释进行着色, 并返回单词结束的下标.
     *
     * @param doc
     * @param pos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun colouringComment(doc: StyledDocument, pos: Int): Int {
        val wordEnd = indexOfCommentEnd(doc, pos)
        if (getCharAt(doc, wordEnd - 1) == '}') {
            if (wordEnd - 1 !in commentExistEnds) {
                commentExistEnds.add(wordEnd - 1)
//                println("Ends: $commentExistEnds")
            }
            var short = Int.MAX_VALUE
            var shortPos = 0
            for (c in commentStarts) {
                if ((wordEnd - c) > 0) {
//                            short = min(start - c, short)
                    if ((wordEnd - c) < short) {
                        short = wordEnd - c
                        shortPos = c
                    }
                }
            }
            if (short != Int.MAX_VALUE) {
//                println("配对{ pos:$shortPos")
                commentStarts.remove(shortPos)
            }
        }

        SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, commentStyle))
        return wordEnd
    }

    /**
     * 取得在文档中下标在pos处的字符.
     *
     *
     * 如果pos为doc.getLength(), 返回的是一个文档的结束符, 不会抛出异常. 如果pos<0, 则会抛出异常.
     * 所以pos的有效值是[0, doc.getLength()]
     *
     * @param doc
     * @param pos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun getCharAt(doc: Document, pos: Int): Char {
        return doc.getText(pos, 1)[0]
    }

    /**
     * 取得下标为pos时, 它所在的单词开始的下标. ?±wor^d?± (^表示pos, ?±表示开始或结束的下标)
     *
     * @param doc
     * @param startPos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun indexOfWordStart(doc: Document, startPos: Int): Int {
        var pos = startPos
        // 从pos开始向前找到第一个非单词字符.
        while (pos > 0 && isWordCharacter(doc, pos - 1)) {
            --pos
        }

        return pos
    }

    /**
     * 取得下标为pos时, 它所在的单词结束的下标. ?±wor^d?± (^表示pos, ?±表示开始或结束的下标)
     *
     * @param doc
     * @param endPos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun indexOfWordEnd(doc: Document, endPos: Int): Int {
        var pos = endPos
        // 从pos开始向前找到第一个非单词字符.
        while (isWordCharacter(doc, pos) && pos < doc.length) {
            ++pos
        }

        return pos
    }

    /**
     * 取得下标为pos时, 它所在的单词结束的下标. ?±wor^d?± (^表示pos, ?±表示开始或结束的下标)
     *
     * @param doc
     * @param endPos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun indexOfCommentEnd(doc: Document, endPos: Int): Int {
        var pos = endPos
        // 从pos开始向前找到第一个注释终结符.
        while (isCommentCharacter(doc, pos) && pos < doc.length) {
            ++pos
        }
        pos++
        return pos
    }

    /**
     * 如果一个字符是字母, 数字, 下划线, 则返回true.
     *
     * @param doc
     * @param pos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun isWordCharacter(doc: Document, pos: Int): Boolean {
        val ch = getCharAt(doc, pos)
//        return Character.isLetter(ch) || Character.isDigit(ch) || ch == '_' || ch == '{' || ch == '}'
        return Character.isLetter(ch) || Character.isDigit(ch) || ch == '_'
    }

    /**
     * 如果一个字符是字母, 数字, 下划线, 则返回true.
     *
     * @param doc
     * @param pos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun isCommentCharacter(doc: Document, pos: Int): Boolean {
        val ch = getCharAt(doc, pos)
        return ch != '}'
    }

    override fun changedUpdate(e: DocumentEvent) {

    }

    override fun insertUpdate(e: DocumentEvent) {
        try {
            colouring(e.document as StyledDocument, e.offset, e.length)
//            println("i ${e.offset} ${e.document.getText(e.offset, e.length)}")
//            println(commentStarts)
        } catch (e1: BadLocationException) {
            e1.printStackTrace()
        }

    }

    override fun removeUpdate(e: DocumentEvent) {
        try {
            // 因为删除后光标紧接着影响的单词两边, 所以长度就不需要了
            if (e.offset in commentStarts) {
                commentStarts.remove(e.offset)
                commentExistStarts.remove(e.offset)
//                println("删除{")
                colouring(e.document as StyledDocument, e.offset, e.document.length - e.offset)
            } else {
//                println(getCharAt(e.document as StyledDocument, e.offset))
                if (e.offset in commentExistEnds) {
                    commentExistEnds.remove(e.offset)
//                    println("删除}")
                    val start = e.offset
                    var short = Int.MAX_VALUE
                    var shortPos = 0
                    for (c in commentExistStarts) {
                        if ((start - c) > 0) {
//                            short = min(start - c, short)
                            if ((start - c) < short) {
                                short = start - c
                                shortPos = c
                            }
                        }
                    }
                    if (short != Int.MAX_VALUE) {
                        commentStarts.add(shortPos)
//                        println("前置{ pos:$shortPos $commentStarts")
                        colouringComment(e.document as StyledDocument, shortPos)
                    }
                } else {
                    colouring(e.document as StyledDocument, e.offset, 0)
                }
            }
//            println("d ${e.offset}")
//            println(commentStarts)
        } catch (e1: BadLocationException) {
            e1.printStackTrace()
        }

    }

    /**
     * 完成着色任务
     *
     * @author Biao
     */
    private inner class ColouringTask(private val doc: StyledDocument, private val pos: Int, private val len: Int, private val style: Style) : Runnable {

        override fun run() {
            try {
                // 这里就是对字符进行着色
                doc.setCharacterAttributes(pos, len, style, true)
            } catch (e: Exception) {
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val frame = JFrame("TEST")

            val editor = JTextPane()
            editor.document.addDocumentListener(SyntaxHighlighter(editor))
            editor.background = Color.black
            editor.font = Font("Monospaced", 1, 20)
            frame.contentPane.add(editor)

            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setSize(500, 500)
            frame.isVisible = true
        }
    }
}
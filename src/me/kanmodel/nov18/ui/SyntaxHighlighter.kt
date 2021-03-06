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
    private val keywords: MutableSet<String>
    private val operators: MutableSet<String>
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
            } else if (ch == '{' || ch == '}') {//注释范围解析
                if (ch == '{') {//记录{位置
                    if (start !in commentStarts) {
                        commentStarts.add(start)
                    }
//                    if (start !in commentExistStarts) {
//                        commentExistStarts.add(start)
//                    }
                } else {//对}进行处理
//                    if (start !in commentExistEnds) {
//                        commentExistEnds.add(start)//记录}位置
//                    }
                    var short = Int.MAX_VALUE//最近左侧{距离
                    var rShort = Int.MAX_VALUE//最近右侧{距离
                    var shortPos = 0
                    for (c in commentExistStarts) {
                        if ((start - c) > 0) {//向前寻找最近{
                            if ((start - c) < short) {
                                short = start - c
                                shortPos = c
                            }
                        }
                        if ((c - start) > 0) {//往后寻找最近{
                            if ((c - start) < rShort) {
                                rShort = c - start
                            }
                        }
                    }
                    if (short != Int.MAX_VALUE) {//找对右侧最近{
                        colouringComment(doc, shortPos)
                        if (shortPos in commentStarts) {
                            commentStarts.remove(shortPos)
                        }
                    }
                    if (rShort != Int.MAX_VALUE) {//左侧存在最近{
                        colouring(doc, start + 1, rShort)
                    } else {//不存在则渲染到文本结尾
                        if (start + 1 < doc.length) {//判断是否恰好的结尾情况
                            colouring(doc, start + 1, doc.length - start - 1)
                        }
                    }
                }
                start = colouringComment(doc, start)//渲染注释
            } else {
                val isComment = isComment(start)
                if (!isComment) {//判断是否为被注释包围的字符串
                    SwingUtilities.invokeLater(ColouringTask(doc, start, 1, normalStyle))
                } else {
                    colouringComment(doc, start)
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

        val isComment = isComment(pos)
        when {
            // 如果是关键字, 就进行关键字的着色, 否则使用普通的着色.
            // 这里有一点要注意, 在insertUpdate和removeUpdate的方法调用的过程中, 不能修改doc的属性.
            // 但我们又要达到能够修改doc的属性, 所以把此任务放到这个方法的外面去执行.
            // 实现这一目的, 可以使用新线程, 但放到swing的事件队列里去处理更轻便一点.
            word in keywords -> {//关键字渲染
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, keywordStyle))
                } else {
                    colouringComment(doc, pos)
                }
            }
            word in operators -> {//内置函数渲染
                if (!isComment) {
                    SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, functionStyle))
                } else {
                    colouringComment(doc, pos)
                }
            }
            else -> {//其余字符串
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
        if (getCharAt(doc, wordEnd - 1) == '}') {//判断是否配对}
//            if (wordEnd - 1 !in commentExistEnds) {
//                commentExistEnds.add(wordEnd - 1)
//            }
            var short = Int.MAX_VALUE
            var shortPos = 0
            for (c in commentStarts) {//寻找最近{
                if ((wordEnd - c) > 0) {
                    if ((wordEnd - c) < short) {
                        short = wordEnd - c
                        shortPos = c
                    }
                }
            }
            if (short != Int.MAX_VALUE) {
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
     * 从pos开始向前找到第一个注释终结符 无则返回文本末尾
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
        if (!CompilerFrame.isEdited) {
            CompilerFrame.isEdited = true
            if (CompilerFrame.fileString == null) {
                CompilerFrame.frame.title = "new *${CompilerFrame.titleName}"
            } else {
                CompilerFrame.frame.title = "${CompilerFrame.fileString} *${CompilerFrame.titleName}"
            }
        }
        try {
            if (isDebug) {
                println("${e.offset} ${e.length}")
            }
            for (i in 0 until commentStarts.size) {
                if (e.offset <= commentExistStarts[i]) {
                    commentStarts[i] += e.length
                }
            }
            for (i in 0 until commentExistStarts.size) {
                if (e.offset <= commentExistStarts[i]) {
                    commentExistStarts[i] += e.length
                }
            }
            for (i in 0 until commentExistEnds.size) {
                if (e.offset <= commentExistEnds[i]) {
                    commentExistEnds[i] += e.length
                }
            }
            for (i in e.offset until e.offset + e.length) {
                val char = getCharAt(e.document, i)
                if (char == '}') {
                    commentExistEnds.add(i)
                }else if (char == '{') {
                    commentExistStarts.add(i)
                }
            }
//            commentExistEnds.forEach {
//                if (e.offset < it) {
//                    for (i in 1..e.length) {
//                        it.inc()
//                    }
//                }
//            }
            colouring(e.document as StyledDocument, e.offset, e.length)
        } catch (e1: BadLocationException) {
            e1.printStackTrace()
        }

    }

    override fun removeUpdate(e: DocumentEvent) {
        if (!CompilerFrame.isEdited) {
            CompilerFrame.isEdited = true
            if (CompilerFrame.fileString == null) {
                CompilerFrame.frame.title = "new *${CompilerFrame.titleName}"
            } else {
                CompilerFrame.frame.title = "${CompilerFrame.fileString} *${CompilerFrame.titleName}"
            }
        }
        try {
            if (e.offset in commentExistStarts) {
                commentStarts.remove(e.offset)
                commentExistStarts.remove(e.offset)
                colouring(e.document as StyledDocument, e.offset, e.document.length - e.offset)
            } else {
                // 因为删除后光标紧接着影响的单词两边, 所以长度就不需要了
                if (e.offset in commentExistEnds) {
                    commentExistEnds.remove(e.offset)
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

    private fun isComment(pos: Int): Boolean{
        var isComment = false//判断是否被注释包围
        var isSingle = false
        for (it in commentStarts) {
            if (it < pos) {
                isSingle = true
                isComment = true
                break
            }
        }
        if (!isSingle) {//判断是否被{}包围
            var lShort = Int.MAX_VALUE
            var lPos = 0
            var haveR = false
            for (it in commentExistStarts) {//判断左侧是否有{
                if (it - pos < 0 && pos - it < lShort) {//若有寻求最近{
                    haveR = true
                    lShort = pos - it
                    lPos = it
                }
            }
            if (haveR) {//左侧有{才寻找}
                var rShort = Int.MAX_VALUE
                var rPos = 0
                for (it in commentExistEnds) {
                    if (it - lPos > 0 && it - lPos < rShort) {
                        rShort = it - lPos
                        rPos = it
                    }
                }
                if (pos in lPos..rPos) {
                    isComment = true
                }
            }
        }
        if (isDebug) {
            if (isComment) {
                println("${++count} comment $commentStarts $commentExistStarts $commentExistEnds")
            } else {
                println("${++count} !comment $commentStarts $commentExistStarts $commentExistEnds")
            }
        }
        return isComment
    }

    companion object {


        private val commentStarts = arrayListOf<Int>()
        private val commentExistStarts = arrayListOf<Int>()
        private val commentExistEnds = arrayListOf<Int>()
        val isDebug = false
        var count = 0//debug

        fun clearComment(){
            commentStarts.clear()
            commentExistStarts.clear()
            commentExistEnds.clear()
        }

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
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
 * @description: ������Ⱦ��
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
        // ׼����ɫʹ�õ���ʽ
        StyleConstants.setForeground(keywordStyle, Color.ORANGE)
        StyleConstants.setForeground(functionStyle, Color.CYAN)
        StyleConstants.setForeground(symbolStyle, Color.WHITE)
        StyleConstants.setForeground(stringStyle, Color.GREEN)
        StyleConstants.setForeground(commentStyle, Color.GRAY)
        StyleConstants.setForeground(normalStyle, Color.WHITE)

        // ׼���ؼ���
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
        // ׼��������غ���
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
        // ȡ�ò������ɾ����Ӱ�쵽�ĵ���.
        // ����"public"��b�����һ���ո�, �ͱ����:"pub lic", ��ʱ������������Ҫ����:"pub"��"lic"
        // ��ʱҪȡ�õķ�Χ��pub��pǰ���λ�ú�lic��c�����λ��
        var start = indexOfWordStart(doc, pos)
        val end = indexOfWordEnd(doc, pos + len)

        var ch: Char
        while (start < end) {
            ch = getCharAt(doc, start)
            if (Character.isLetter(ch) || ch == '_') {
                // ���������ĸ�����»��߿�ͷ, ˵���ǵ���
                // posΪ���������һ���±�
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
//                    println("Ѱ��ǰ��{ $commentStarts ccc")
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
//                        println("���{ pos:$shortPos colouringComment")
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
     * �Ե��ʽ�����ɫ, �����ص��ʽ������±�.
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
            // ����ǹؼ���, �ͽ��йؼ��ֵ���ɫ, ����ʹ����ͨ����ɫ.
            // ������һ��Ҫע��, ��insertUpdate��removeUpdate�ķ������õĹ�����, �����޸�doc������.
            // ��������Ҫ�ﵽ�ܹ��޸�doc������, ���԰Ѵ�����ŵ��������������ȥִ��.
            // ʵ����һĿ��, ����ʹ�����߳�, ���ŵ�swing���¼�������ȥ��������һ��.
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
     * ��ע�ͽ�����ɫ, �����ص��ʽ������±�.
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
//                println("���{ pos:$shortPos")
                commentStarts.remove(shortPos)
            }
        }

        SwingUtilities.invokeLater(ColouringTask(doc, pos, wordEnd - pos, commentStyle))
        return wordEnd
    }

    /**
     * ȡ�����ĵ����±���pos�����ַ�.
     *
     *
     * ���posΪdoc.getLength(), ���ص���һ���ĵ��Ľ�����, �����׳��쳣. ���pos<0, ����׳��쳣.
     * ����pos����Чֵ��[0, doc.getLength()]
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
     * ȡ���±�Ϊposʱ, �����ڵĵ��ʿ�ʼ���±�. ?��wor^d?�� (^��ʾpos, ?����ʾ��ʼ��������±�)
     *
     * @param doc
     * @param startPos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun indexOfWordStart(doc: Document, startPos: Int): Int {
        var pos = startPos
        // ��pos��ʼ��ǰ�ҵ���һ���ǵ����ַ�.
        while (pos > 0 && isWordCharacter(doc, pos - 1)) {
            --pos
        }

        return pos
    }

    /**
     * ȡ���±�Ϊposʱ, �����ڵĵ��ʽ������±�. ?��wor^d?�� (^��ʾpos, ?����ʾ��ʼ��������±�)
     *
     * @param doc
     * @param endPos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun indexOfWordEnd(doc: Document, endPos: Int): Int {
        var pos = endPos
        // ��pos��ʼ��ǰ�ҵ���һ���ǵ����ַ�.
        while (isWordCharacter(doc, pos) && pos < doc.length) {
            ++pos
        }

        return pos
    }

    /**
     * ȡ���±�Ϊposʱ, �����ڵĵ��ʽ������±�. ?��wor^d?�� (^��ʾpos, ?����ʾ��ʼ��������±�)
     *
     * @param doc
     * @param endPos
     * @return
     * @throws BadLocationException
     */
    @Throws(BadLocationException::class)
    fun indexOfCommentEnd(doc: Document, endPos: Int): Int {
        var pos = endPos
        // ��pos��ʼ��ǰ�ҵ���һ��ע���ս��.
        while (isCommentCharacter(doc, pos) && pos < doc.length) {
            ++pos
        }
        pos++
        return pos
    }

    /**
     * ���һ���ַ�����ĸ, ����, �»���, �򷵻�true.
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
     * ���һ���ַ�����ĸ, ����, �»���, �򷵻�true.
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
            // ��Ϊɾ�����������Ӱ��ĵ�������, ���Գ��ȾͲ���Ҫ��
            if (e.offset in commentStarts) {
                commentStarts.remove(e.offset)
                commentExistStarts.remove(e.offset)
//                println("ɾ��{")
                colouring(e.document as StyledDocument, e.offset, e.document.length - e.offset)
            } else {
//                println(getCharAt(e.document as StyledDocument, e.offset))
                if (e.offset in commentExistEnds) {
                    commentExistEnds.remove(e.offset)
//                    println("ɾ��}")
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
//                        println("ǰ��{ pos:$shortPos $commentStarts")
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
     * �����ɫ����
     *
     * @author Biao
     */
    private inner class ColouringTask(private val doc: StyledDocument, private val pos: Int, private val len: Int, private val style: Style) : Runnable {

        override fun run() {
            try {
                // ������Ƕ��ַ�������ɫ
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
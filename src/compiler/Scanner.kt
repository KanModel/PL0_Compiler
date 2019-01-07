package compiler

import compiler.error.Err

import java.io.BufferedReader
import java.io.IOException
import java.util.ArrayList

/**
 * �����ʷ�����������Ĺ����Ǵ�Դ���������ȡ�ķ����ţ�����PL/0����������Ҫ��ɲ���֮һ��
 */


class Scanner(val inReader: BufferedReader) {
    /**
     * �ոն�����ַ�
     */
    private var justReadChar = ' '

    /**
     * ��ǰ�к�
     */
    var lineCounter = 0

    /**
     * ��ǰ������� �ַ�����
     */
    private var currentLine: CharArray? = null

    /**
     * ��ǰ�еĳ��ȣ�currentLine length��
     */
    var currentLineLength = 0

    /**
     * ��ǰ�ַ��ڵ�ǰ���е�λ�ã�character counter��
     */
    var charCounter = 0

    /**
     * ��ǰ����ķ���
     */
    var currentSymbol: Symbol? = null

    /**
     * �������б�ע�Ᵽ���ֵĴ��˳��
     */
    private val keyword: Array<String>

    /**
     * �����ֶ�Ӧ�ķ���ֵ
     */
    private val keywordTable: Array<Symbol?> = arrayOfNulls(PL0.KEYWORD_COUNT)

    /**
     * ���ַ��ķ���ֵ
     */
    private val charTable: Array<Symbol?>

    /**
     * ��ʶ�����֣������ǰ�����Ǳ�ʶ���Ļ���
     *
     * @see Parser
     *
     * @see Table.enter
     */
    lateinit var id: String

    /**
     * ��ֵ��С�������ǰ���������ֵĻ���
     *
     * @see Parser
     *
     * @see Table.enter
     */
    var num: Int = 0

    /**
     * �ַ����洢
     */
    var strList = ArrayList<Int>()

    init {

        // ���õ��ַ�����
        charTable = arrayOfNulls(256)
        java.util.Arrays.fill(charTable, Symbol.nul)
        charTable['+'.toInt()] = Symbol.plus
        charTable['-'.toInt()] = Symbol.minus
        charTable['*'.toInt()] = Symbol.times
        charTable['/'.toInt()] = Symbol.slash
        charTable['('.toInt()] = Symbol.lParen
        charTable[')'.toInt()] = Symbol.rParen
        charTable['='.toInt()] = Symbol.equal
        charTable[','.toInt()] = Symbol.comma
        charTable['.'.toInt()] = Symbol.period
        charTable['#'.toInt()] = Symbol.neq
        charTable[';'.toInt()] = Symbol.semicolon
        charTable['['.toInt()] = Symbol.lSquBra
        charTable[']'.toInt()] = Symbol.rSquBra
        charTable['%'.toInt()] = Symbol.mod
        charTable['!'.toInt()] = Symbol.not

        // ���ñ���������,������ĸ˳�򣬱����۰����
        keyword = arrayOf("array", "begin", "call", "const", "do", "else", "end", "if", "odd", "print", "procedure", "read", "sqrt", "then", "var", "while", "write")

        // ���ñ����ַ���
        keywordTable[0] = Symbol.arraySym
        keywordTable[1] = Symbol.beginSym
        keywordTable[2] = Symbol.callSym
        keywordTable[3] = Symbol.constSym
        keywordTable[4] = Symbol.doSym
        keywordTable[5] = Symbol.elseSym
        keywordTable[6] = Symbol.endSym
        keywordTable[7] = Symbol.ifSym
        keywordTable[8] = Symbol.oddSym
        keywordTable[9] = Symbol.printSym
        keywordTable[10] = Symbol.procSym
        keywordTable[11] = Symbol.readSym
        keywordTable[12] = Symbol.sqrtSym
        keywordTable[13] = Symbol.thenSym
        keywordTable[14] = Symbol.varSym
        keywordTable[15] = Symbol.whileSym
        keywordTable[16] = Symbol.writeSym
    }

    /**
     * ��ȡһ���ַ���Ϊ���ٴ���I/O������ÿ�ζ�ȡһ��
     */
    private fun getChar() {
        var line = ""
        try {
            if (charCounter == currentLineLength) {
                while (line == "") {
                    line = inReader.readLine().toLowerCase() + "\n"
                    lineCounter++
                }
                currentLineLength = line.length
                charCounter = 0
                currentLine = line.toCharArray()
                println("${PL0.interpreter.cx} $line")
                PL0.sourcePrintStream.println("${PL0.interpreter.cx} $line")
            }
        } catch (e: IOException) {
            throw Error("program incomplete")
        }

        justReadChar = currentLine!![charCounter]
        charCounter++
    }

    /**
     * �ʷ���������ȡһ���ʷ����ţ��Ǵʷ����������ص�
     */
    fun getSymbol() {
        while (Character.isWhitespace(justReadChar))
            getChar()// �������пհ��ַ�

        if (justReadChar in 'a'..'z' || justReadChar == '_') {
            // �ؼ��ֻ���һ���ʶ��
            matchKeywordOrIdentifier()
        } else if (justReadChar in '0'..'9') {
            // ����
            matchNumber()
        } else if (justReadChar == '\'') {
            // �ַ�
            matchChar()
        } else if (justReadChar == '"') {
            // �ַ���
            matchString()
        } else {
            // ������
            matchOperator()
        }
    }

    /**
     * �����ؼ��ֻ���һ���ʶ��
     */
    private fun matchKeywordOrIdentifier() {
        val i: Int
        val sb = StringBuilder(PL0.SYMBOL_MAX_LENGTH)
        // ���Ȱ��������ʶ�����
        do {
            sb.append(justReadChar)
            getChar()
        } while (justReadChar in 'a'..'z' || justReadChar in '0'..'9' || justReadChar == '_')
        id = sb.toString()

        // Ȼ�������ǲ��Ǳ����֣���ע��ʹ�õ���ʲô����������
        i = java.util.Arrays.binarySearch(keyword, id)

        // ����γɷ�����Ϣ
        if (i < 0) {
            // һ���ʶ��
            currentSymbol = Symbol.ident
        } else {
            // �ؼ���
            currentSymbol = keywordTable[i]
        }
    }

    /**
     * ��������
     */
    private fun matchNumber() {
        var digit = 0
        currentSymbol = Symbol.number
        num = 0
        do {
            num = 10 * num + Character.digit(justReadChar, 10)
            digit++
            getChar()
        } while (justReadChar in '0'..'9')                // ��ȡ���ֵ�ֵ
        digit--
        if (digit > PL0.MAX_NUM_DIGIT)
            Err.report(30)
    }

    /**
     * ����������
     */
    private fun matchOperator() {
        when (justReadChar) {
            ':'        // ��ֵ����
            -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.becomes
                    getChar()
                } else {
                    // ����ʶ��ķ���
                    currentSymbol = Symbol.nul
                }
            }
            '<'        // С�ڻ���С�ڵ���
            -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.leq
                    getChar()
                } else {
                    currentSymbol = Symbol.lss
                }
            }
            '>'        // ���ڻ��ߴ��ڵ���
            -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.geq
                    getChar()
                } else {
                    currentSymbol = Symbol.gtr
                }
            }
            '{'        //ע��
            -> {
                getChar()
                var count = 1000//ע�������
                while (justReadChar != '}' && count-- > 0) {
                    getChar()
                }
                currentSymbol = Symbol.comment
                getChar()
            }
            '+' -> {
                getChar()
                if (justReadChar == '+') {
                    currentSymbol = Symbol.plusplus
                    getChar()
                } else if (justReadChar == '=') {
                    currentSymbol = Symbol.plusAssSym
                    getChar()
                } else {
                    //����+��
                    currentSymbol = Symbol.plus
                }
            }
            '-' -> {
                getChar()
                if (justReadChar == '-') {
                    currentSymbol = Symbol.minusminus
                    getChar()
                } else if (justReadChar == '=') {
                    currentSymbol = Symbol.minusAssSym
                    getChar()
                } else {
                    //����-��
                    currentSymbol = Symbol.minus
                }
            }
            '*' -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.timesAssSym
                    getChar()
                } else {
                    //����+��
                    currentSymbol = Symbol.times
                }
            }
            '/' -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.slashAssSym
                    getChar()
                } else {
                    //����+��
                    currentSymbol = Symbol.slash
                }
            }

            '%' -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.modAssSym
                    getChar()
                } else {
                    //����%��
                    currentSymbol = Symbol.mod
                }
            }

            else        // ����Ϊ���ַ���������������ŷǷ��򷵻�nil��
            -> {
                currentSymbol = charTable[justReadChar.toInt()]
                if (currentSymbol != Symbol.period)
                    getChar()
            }
        }
    }

    /**
     * �����ַ�
     */
    private fun matchChar() {
        currentSymbol = Symbol.number
        getChar()
        num = justReadChar.toInt()
        getChar()
        if (justReadChar != '\'') {
            Err.report(41)
        }
        getChar()
    }

    /**
     * �����ַ�
     */
    private fun matchString() {
        strList.clear()
        currentSymbol = Symbol.string

        getChar()
        while (justReadChar != '"') {
            strList.add(justReadChar.toInt())
            getChar()
        }
        getChar()
    }
}

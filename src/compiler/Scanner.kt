package compiler

import compiler.error.Err

import java.io.BufferedReader
import java.io.IOException
import java.util.ArrayList

/**
 * 　　词法分析器负责的工作是从源代码里面读取文法符号，这是PL/0编译器的主要组成部分之一。
 */


class Scanner(val inReader: BufferedReader) {
    /**
     * 刚刚读入的字符
     */
    private var justReadChar = ' '

    /**
     * 当前行号
     */
    var lineCounter = 0

    /**
     * 当前读入的行 字符数组
     */
    private var currentLine: CharArray? = null

    /**
     * 当前行的长度（currentLine length）
     */
    var currentLineLength = 0

    /**
     * 当前字符在当前行中的位置（character counter）
     */
    var charCounter = 0

    /**
     * 当前读入的符号
     */
    var currentSymbol: Symbol? = null

    /**
     * 保留字列表（注意保留字的存放顺序）
     */
    private val keyword: Array<String>

    /**
     * 保留字对应的符号值
     */
    private val keywordTable: Array<Symbol?> = arrayOfNulls(PL0.KEYWORD_COUNT)

    /**
     * 单字符的符号值
     */
    private val charTable: Array<Symbol?>

    /**
     * 标识符名字（如果当前符号是标识符的话）
     *
     * @see Parser
     *
     * @see Table.enter
     */
    lateinit var id: String

    /**
     * 数值大小（如果当前符号是数字的话）
     *
     * @see Parser
     *
     * @see Table.enter
     */
    var num: Int = 0

    /**
     * 字符串存储
     */
    var strList = ArrayList<Int>()

    init {

        // 设置单字符符号
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

        // 设置保留字名字,按照字母顺序，便于折半查找
        keyword = arrayOf("array", "begin", "call", "const", "do", "else", "end", "if", "odd", "print", "procedure", "read", "sqrt", "then", "var", "while", "write")

        // 设置保留字符号
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
     * 读取一个字符，为减少磁盘I/O次数，每次读取一行
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
     * 词法分析，获取一个词法符号，是词法分析器的重点
     */
    fun getSymbol() {
        while (Character.isWhitespace(justReadChar))
            getChar()// 跳过所有空白字符

        if (justReadChar in 'a'..'z' || justReadChar == '_') {
            // 关键字或者一般标识符
            matchKeywordOrIdentifier()
        } else if (justReadChar in '0'..'9') {
            // 数字
            matchNumber()
        } else if (justReadChar == '\'') {
            // 字符
            matchChar()
        } else if (justReadChar == '"') {
            // 字符串
            matchString()
        } else {
            // 操作符
            matchOperator()
        }
    }

    /**
     * 分析关键字或者一般标识符
     */
    private fun matchKeywordOrIdentifier() {
        val i: Int
        val sb = StringBuilder(PL0.SYMBOL_MAX_LENGTH)
        // 首先把整个单词读出来
        do {
            sb.append(justReadChar)
            getChar()
        } while (justReadChar in 'a'..'z' || justReadChar in '0'..'9' || justReadChar == '_')
        id = sb.toString()

        // 然后搜索是不是保留字（请注意使用的是什么搜索方法）
        i = java.util.Arrays.binarySearch(keyword, id)

        // 最后形成符号信息
        if (i < 0) {
            // 一般标识符
            currentSymbol = Symbol.ident
        } else {
            // 关键字
            currentSymbol = keywordTable[i]
        }
    }

    /**
     * 分析数字
     */
    private fun matchNumber() {
        var digit = 0
        currentSymbol = Symbol.number
        num = 0
        do {
            num = 10 * num + Character.digit(justReadChar, 10)
            digit++
            getChar()
        } while (justReadChar in '0'..'9')                // 获取数字的值
        digit--
        if (digit > PL0.MAX_NUM_DIGIT)
            Err.report(30)
    }

    /**
     * 分析操作符
     */
    private fun matchOperator() {
        when (justReadChar) {
            ':'        // 赋值符号
            -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.becomes
                    getChar()
                } else {
                    // 不能识别的符号
                    currentSymbol = Symbol.nul
                }
            }
            '<'        // 小于或者小于等于
            -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.leq
                    getChar()
                } else {
                    currentSymbol = Symbol.lss
                }
            }
            '>'        // 大于或者大于等于
            -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.geq
                    getChar()
                } else {
                    currentSymbol = Symbol.gtr
                }
            }
            '{'        //注释
            -> {
                getChar()
                var count = 1000//注释最长长度
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
                    //单个+号
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
                    //单个-号
                    currentSymbol = Symbol.minus
                }
            }
            '*' -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.timesAssSym
                    getChar()
                } else {
                    //单个+号
                    currentSymbol = Symbol.times
                }
            }
            '/' -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.slashAssSym
                    getChar()
                } else {
                    //单个+号
                    currentSymbol = Symbol.slash
                }
            }

            '%' -> {
                getChar()
                if (justReadChar == '=') {
                    currentSymbol = Symbol.modAssSym
                    getChar()
                } else {
                    //单个%号
                    currentSymbol = Symbol.mod
                }
            }

            else        // 其他为单字符操作符（如果符号非法则返回nil）
            -> {
                currentSymbol = charTable[justReadChar.toInt()]
                if (currentSymbol != Symbol.period)
                    getChar()
            }
        }
    }

    /**
     * 分析字符
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
     * 分析字符
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

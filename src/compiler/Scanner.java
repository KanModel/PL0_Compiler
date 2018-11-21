package compiler;

import compiler.error.Err;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 　　词法分析器负责的工作是从源代码里面读取文法符号，这是PL/0编译器的主要组成部分之一。
 */

public class Scanner {
    /**
     * 刚刚读入的字符
     */
    private char justReadChar = ' ';

    /**
     * 当前行号
     */
    public int lineCounter = 0;

    /**
     * 当前读入的行 字符数组
     */
    private char[] currentLine;

    /**
     * 当前行的长度（currentLine length）
     */
    public int currentLineLength = 0;

    /**
     * 当前字符在当前行中的位置（character counter）
     */
    public int charCounter = 0;

    /**
     * 当前读入的符号
     */
    public Symbol currentSymbol;

    /**
     * 保留字列表（注意保留字的存放顺序）
     */
    private String[] keyword;

    /**
     * 保留字对应的符号值
     */
    private Symbol[] keywordTable;

    /**
     * 单字符的符号值
     */
    private Symbol[] charTable;

    // 输入流
    private BufferedReader inReader;

    /**
     * 标识符名字（如果当前符号是标识符的话）
     *
     * @see Parser
     * @see Table#enter
     */
    public String id;

    /**
     * 数值大小（如果当前符号是数字的话）
     *
     * @see Parser
     * @see Table#enter
     */
    public int num;

    /**
     * 初始化词法分析器
     *
     * @param input PL/0 源文件输入流
     */
    public Scanner(BufferedReader input) {
        inReader = input;

        // 设置单字符符号
        charTable = new Symbol[256];
        java.util.Arrays.fill(charTable, Symbol.nul);
        charTable['+'] = Symbol.plus;
        charTable['-'] = Symbol.minus;
        charTable['*'] = Symbol.times;
        charTable['/'] = Symbol.slash;
        charTable['('] = Symbol.lParen;
        charTable[')'] = Symbol.rParen;
        charTable['='] = Symbol.equal;
        charTable[','] = Symbol.comma;
        charTable['.'] = Symbol.period;
        charTable['#'] = Symbol.neq;
        charTable[';'] = Symbol.semicolon;
        charTable['['] = Symbol.lSquBra;
        charTable[']'] = Symbol.rSquBra;

        // 设置保留字名字,按照字母顺序，便于折半查找
        keyword = new String[]{"array", "begin", "call", "const", "do", "else","end", "if",
                "odd", "procedure", "read", "sqrt", "then", "var", "while", "write"};

        // 设置保留字符号
        keywordTable = new Symbol[PL0.KEYWORD_COUNT];
        keywordTable[0] = Symbol.arraySym;
        keywordTable[1] = Symbol.beginSym;
        keywordTable[2] = Symbol.callSym;
        keywordTable[3] = Symbol.constSym;
        keywordTable[4] = Symbol.doSym;
        keywordTable[5] = Symbol.elseSym;
        keywordTable[6] = Symbol.endSym;
        keywordTable[7] = Symbol.ifSym;
        keywordTable[8] = Symbol.oddSym;
        keywordTable[9] = Symbol.procSym;
        keywordTable[10] = Symbol.readSym;
        keywordTable[11] = Symbol.sqrtSym;
        keywordTable[12] = Symbol.thenSym;
        keywordTable[13] = Symbol.varSym;
        keywordTable[14] = Symbol.whileSym;
        keywordTable[15] = Symbol.writeSym;
    }

    /**
     * 读取一个字符，为减少磁盘I/O次数，每次读取一行
     */
    void getChar() {
        String line = "";
        try {
            if (charCounter == currentLineLength) {
                while (line.equals("")) {
                    line = inReader.readLine().toLowerCase() + "\n";
                    lineCounter++;
                }
                currentLineLength = line.length();
                charCounter = 0;
                currentLine = line.toCharArray();
                System.out.println(PL0.interpreter.cx + " " + line);
                PL0.sourcePrintStream.println(PL0.interpreter.cx + " " + line);
            }
        } catch (IOException e) {
            throw new Error("program imcomplete");
        }
        justReadChar = currentLine[charCounter];
        charCounter++;
    }

    /**
     * 词法分析，获取一个词法符号，是词法分析器的重点
     */
    public void getSymbol() {
        // Wirth 的 PL/0 编译器使用一系列的if...else...来处理
        // 但是你的助教认为下面的写法能够更加清楚地看出这个函数的处理逻辑
        while (Character.isWhitespace(justReadChar))        // 跳过所有空白字符
            getChar();

        if ((justReadChar >= 'a' && justReadChar <= 'z') || justReadChar == '_') {
            // 关键字或者一般标识符
            matchKeywordOrIdentifier();
        } else if (justReadChar >= '0' && justReadChar <= '9') {
            // 数字
            matchNumber();
        } else {
            // 操作符
            matchOperator();
        }
    }

    /**
     * 分析关键字或者一般标识符
     */
    void matchKeywordOrIdentifier() {
        int i;
        StringBuilder sb = new StringBuilder(PL0.SYMBOL_MAX_LENGTH);
        // 首先把整个单词读出来
        do {
            sb.append(justReadChar);
            getChar();
        }
        while (justReadChar >= 'a' && justReadChar <= 'z' || justReadChar >= '0' && justReadChar <= '9' || justReadChar == '_');
        id = sb.toString();

        // 然后搜索是不是保留字（请注意使用的是什么搜索方法）
        i = java.util.Arrays.binarySearch(keyword, id);

        // 最后形成符号信息
        if (i < 0) {
            // 一般标识符
            currentSymbol = Symbol.ident;
        } else {
            // 关键字
            currentSymbol = keywordTable[i];
        }
    }

    /**
     * 分析数字
     */
    void matchNumber() {
        int digit = 0;
        currentSymbol = Symbol.number;
        num = 0;
        do {
            num = 10 * num + Character.digit(justReadChar, 10);
            digit++;
            getChar();
        } while (justReadChar >= '0' && justReadChar <= '9');                // 获取数字的值
        digit--;
        if (digit > PL0.MAX_NUM_DIGIT)
            Err.report(30);
    }

    /**
     * 分析操作符
     */
    void matchOperator() {
        // 请注意这里的写法跟Wirth的有点不同
        switch (justReadChar) {
            case ':':        // 赋值符号
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.becomes;
                    getChar();
                } else {
                    // 不能识别的符号
                    currentSymbol = Symbol.nul;
                }
                break;
            case '<':        // 小于或者小于等于
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.leq;
                    getChar();
                } else {
                    currentSymbol = Symbol.lss;
                }
                break;
            case '>':        // 大于或者大于等于
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.geq;
                    getChar();
                } else {
                    currentSymbol = Symbol.gtr;
                }
                break;
            case '{':        //注释
                getChar();
                int count = 1000;//注释最长长度
                while ((justReadChar != '}') && count-- > 0) {
                    getChar();
                }
                currentSymbol = Symbol.comment;
                getChar();
                break;
            case '+':
                getChar();
                if (justReadChar == '+') {
                    currentSymbol = Symbol.plusplus;
                    getChar();
                } else if (justReadChar == '=') {
                    currentSymbol = Symbol.plusAssSym;
                    getChar();
                } else {
                    //单个+号
                    currentSymbol = Symbol.plus;
                }
                break;
            case '-':
                getChar();
                if (justReadChar == '-') {
                    currentSymbol = Symbol.minusminus;
                    getChar();
                } else if (justReadChar == '=') {
                    currentSymbol = Symbol.minusAssSym;
                    getChar();
                } else {
                    //单个-号
                    currentSymbol = Symbol.minus;
                }
                break;
            case '*':
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.timesAssSym;
                    getChar();
                } else {
                    //单个+号
                    currentSymbol = Symbol.times;
                }
                break;
            case '/':
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.slashAssSym;
                    getChar();
                } else {
                    //单个+号
                    currentSymbol = Symbol.slash;
                }
                break;
            default:        // 其他为单字符操作符（如果符号非法则返回nil）
                currentSymbol = charTable[justReadChar];
                if (currentSymbol != Symbol.period)
                    getChar();
                break;
        }
    }
}

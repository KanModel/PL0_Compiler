package compiler;

import compiler.error.Err;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * �����ʷ�����������Ĺ����Ǵ�Դ���������ȡ�ķ����ţ�����PL/0����������Ҫ��ɲ���֮һ��
 */

public class Scanner {
    /**
     * �ոն�����ַ�
     */
    private char justReadChar = ' ';

    /**
     * ��ǰ�к�
     */
    public int lineCounter = 0;

    /**
     * ��ǰ������� �ַ�����
     */
    private char[] currentLine;

    /**
     * ��ǰ�еĳ��ȣ�currentLine length��
     */
    public int currentLineLength = 0;

    /**
     * ��ǰ�ַ��ڵ�ǰ���е�λ�ã�character counter��
     */
    public int charCounter = 0;

    /**
     * ��ǰ����ķ���
     */
    public Symbol currentSymbol;

    /**
     * �������б�ע�Ᵽ���ֵĴ��˳��
     */
    private String[] keyword;

    /**
     * �����ֶ�Ӧ�ķ���ֵ
     */
    private Symbol[] keywordTable;

    /**
     * ���ַ��ķ���ֵ
     */
    private Symbol[] charTable;

    // ������
    private BufferedReader inReader;

    /**
     * ��ʶ�����֣������ǰ�����Ǳ�ʶ���Ļ���
     *
     * @see Parser
     * @see Table#enter
     */
    public String id;

    /**
     * ��ֵ��С�������ǰ���������ֵĻ���
     *
     * @see Parser
     * @see Table#enter
     */
    public int num;

    /**
     * ��ʼ���ʷ�������
     *
     * @param input PL/0 Դ�ļ�������
     */
    public Scanner(BufferedReader input) {
        inReader = input;

        // ���õ��ַ�����
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

        // ���ñ���������,������ĸ˳�򣬱����۰����
        keyword = new String[]{"array", "begin", "call", "const", "do", "else","end", "if",
                "odd", "procedure", "read", "sqrt", "then", "var", "while", "write"};

        // ���ñ����ַ���
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
     * ��ȡһ���ַ���Ϊ���ٴ���I/O������ÿ�ζ�ȡһ��
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
     * �ʷ���������ȡһ���ʷ����ţ��Ǵʷ����������ص�
     */
    public void getSymbol() {
        // Wirth �� PL/0 ������ʹ��һϵ�е�if...else...������
        // �������������Ϊ�����д���ܹ���������ؿ�����������Ĵ����߼�
        while (Character.isWhitespace(justReadChar))        // �������пհ��ַ�
            getChar();

        if ((justReadChar >= 'a' && justReadChar <= 'z') || justReadChar == '_') {
            // �ؼ��ֻ���һ���ʶ��
            matchKeywordOrIdentifier();
        } else if (justReadChar >= '0' && justReadChar <= '9') {
            // ����
            matchNumber();
        } else {
            // ������
            matchOperator();
        }
    }

    /**
     * �����ؼ��ֻ���һ���ʶ��
     */
    void matchKeywordOrIdentifier() {
        int i;
        StringBuilder sb = new StringBuilder(PL0.SYMBOL_MAX_LENGTH);
        // ���Ȱ��������ʶ�����
        do {
            sb.append(justReadChar);
            getChar();
        }
        while (justReadChar >= 'a' && justReadChar <= 'z' || justReadChar >= '0' && justReadChar <= '9' || justReadChar == '_');
        id = sb.toString();

        // Ȼ�������ǲ��Ǳ����֣���ע��ʹ�õ���ʲô����������
        i = java.util.Arrays.binarySearch(keyword, id);

        // ����γɷ�����Ϣ
        if (i < 0) {
            // һ���ʶ��
            currentSymbol = Symbol.ident;
        } else {
            // �ؼ���
            currentSymbol = keywordTable[i];
        }
    }

    /**
     * ��������
     */
    void matchNumber() {
        int digit = 0;
        currentSymbol = Symbol.number;
        num = 0;
        do {
            num = 10 * num + Character.digit(justReadChar, 10);
            digit++;
            getChar();
        } while (justReadChar >= '0' && justReadChar <= '9');                // ��ȡ���ֵ�ֵ
        digit--;
        if (digit > PL0.MAX_NUM_DIGIT)
            Err.report(30);
    }

    /**
     * ����������
     */
    void matchOperator() {
        // ��ע�������д����Wirth���е㲻ͬ
        switch (justReadChar) {
            case ':':        // ��ֵ����
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.becomes;
                    getChar();
                } else {
                    // ����ʶ��ķ���
                    currentSymbol = Symbol.nul;
                }
                break;
            case '<':        // С�ڻ���С�ڵ���
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.leq;
                    getChar();
                } else {
                    currentSymbol = Symbol.lss;
                }
                break;
            case '>':        // ���ڻ��ߴ��ڵ���
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.geq;
                    getChar();
                } else {
                    currentSymbol = Symbol.gtr;
                }
                break;
            case '{':        //ע��
                getChar();
                int count = 1000;//ע�������
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
                    //����+��
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
                    //����-��
                    currentSymbol = Symbol.minus;
                }
                break;
            case '*':
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.timesAssSym;
                    getChar();
                } else {
                    //����+��
                    currentSymbol = Symbol.times;
                }
                break;
            case '/':
                getChar();
                if (justReadChar == '=') {
                    currentSymbol = Symbol.slashAssSym;
                    getChar();
                } else {
                    //����+��
                    currentSymbol = Symbol.slash;
                }
                break;
            default:        // ����Ϊ���ַ���������������ŷǷ��򷵻�nil��
                currentSymbol = charTable[justReadChar];
                if (currentSymbol != Symbol.period)
                    getChar();
                break;
        }
    }
}

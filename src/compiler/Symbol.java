package compiler;

/**
 * �����ַ��ŵı���
 */
public enum Symbol {
    nul, ident, number, plus, minus, times, slash,//0-6
    oddSym, equal, neq, lss, leq, gtr, geq, lParen, rParen,//7-15
    comma, //����,
    semicolon, //�ֺ�;
    period, //���.
    becomes, //��ֵ:=
    beginSym, endSym, ifSym, thenSym, whileSym, writeSym,
    readSym, doSym, callSym, constSym, varSym, procSym,
    sqrtSym, //����
    comment, //ע��
    plusAssSym,
    minusAssSym,
    timesAssSym,
    slashAssSym,
    plusplus, //��������++
    minusminus //�Լ�����--
}
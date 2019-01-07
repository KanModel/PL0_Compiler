package compiler

/**
 * �����ַ��ŵı���
 */
enum class Symbol {
    nul, ident, number, plus, minus, times, slash, //0-6
    oddSym, equal, neq, lss, leq, gtr, geq, lParen, rParen, //7-15
    comma, //����,
    semicolon, //�ֺ�;
    period, //���.
    becomes, //��ֵ:=
    beginSym, endSym, ifSym, thenSym, whileSym, writeSym,
    readSym, doSym, callSym, constSym, varSym, procSym,
    printSym,
    elseSym,
    sqrtSym, //����
    comment, //ע��
    arraySym,
    lSquBra,
    rSquBra,
    plusAssSym,
    minusAssSym,
    timesAssSym,
    slashAssSym,
    plusplus, //��������++
    minusminus, //�Լ�����--
    string,
    mod, //ȡģ
    modAssSym, //%=
    not//�߼���
}
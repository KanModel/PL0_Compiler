package compiler;

/**
 * ¡¡¸÷ÖÖ·ûºÅµÄ±àÂë
 */
public enum Symbol {
    nul, ident, number, plus, minus, times, slash,//0-6
    oddSym, equal, neq, lss, leq, gtr, geq, lParen, rParen,//7-15
    comma, //¶ººÅ,
    semicolon, //·ÖºÅ;
    period, //¾äºÅ.
    becomes, //¸³Öµ:=
    beginSym, endSym, ifSym, thenSym, whileSym, writeSym,
    readSym, doSym, callSym, constSym, varSym, procSym,
    comment, //×¢ÊÍ
    plusplus, //×ÔÔö·ûºÅ++
    minusminus //×Ô¼õ·ûºÅ--
}
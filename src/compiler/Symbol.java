package compiler;

/**
 * 　各种符号的编码
 */
public enum Symbol {
    nul, ident, number, plus, minus, times, slash,
    oddSym, equal, neq, lss, leq, gtr, geq, lparen, rparen,
    comma, //逗号,
    semicolon, //分号;
    period, becomes,
    beginSym, endSym, ifSym, thenSym, whileSym, writeSym,
    readSym, doSym, callSym, constSym, varSym, procSym,
    comment
}
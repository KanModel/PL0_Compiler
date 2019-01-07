package compiler

/**
 * 　各种符号的编码
 */
enum class Symbol {
    nul, ident, number, plus, minus, times, slash, //0-6
    oddSym, equal, neq, lss, leq, gtr, geq, lParen, rParen, //7-15
    comma, //逗号,
    semicolon, //分号;
    period, //句号.
    becomes, //赋值:=
    beginSym, endSym, ifSym, thenSym, whileSym, writeSym,
    readSym, doSym, callSym, constSym, varSym, procSym,
    printSym,
    elseSym,
    sqrtSym, //开方
    comment, //注释
    arraySym,
    lSquBra,
    rSquBra,
    plusAssSym,
    minusAssSym,
    timesAssSym,
    slashAssSym,
    plusplus, //自增符号++
    minusminus, //自减符号--
    string,
    mod, //取模
    modAssSym, //%=
    not//逻辑非
}
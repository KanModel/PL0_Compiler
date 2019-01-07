package compiler.error

import compiler.PL0

import java.util.ArrayList

/**
 * 　　这个类只是包含了报错函数以及错误计数器。
 */
object Err {
    /**
     * 错误计数器，编译过程中一共有多少个错误
     */
    var err = 0

    /**
     * 错误内容记录
     */
    var errors = ArrayList<ErrorInfo>()

    /**
     * 报错函数
     *
     * @param errCode 错误码
     */
    fun report(errCode: Int) {
        val s = CharArray(PL0.scanner.charCounter - 1)
        java.util.Arrays.fill(s, ' ')
        val space = String(s)
        println("****$space!$errCode")
        PL0.sourcePrintStream.println("****$space!$errCode")
        errors.add(ErrorInfo(errCode, PL0.scanner.lineCounter, PL0.scanner.charCounter))
        err++
    }

    fun showResult() {
        println()
        for (i in errors.indices) {
            println("Error in line ${errors[i].lineCount}: ${errors[i].errorInfo} - ${errors[i].errCode}")
        }
    }
}

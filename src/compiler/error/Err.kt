package compiler.error

import compiler.PL0

import java.util.ArrayList

/**
 * ���������ֻ�ǰ����˱������Լ������������
 */
object Err {
    /**
     * ��������������������һ���ж��ٸ�����
     */
    var err = 0

    /**
     * �������ݼ�¼
     */
    var errors = ArrayList<ErrorInfo>()

    /**
     * ������
     *
     * @param errCode ������
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

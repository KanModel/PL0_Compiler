package compiler.error;

import compiler.PL0;

import java.util.ArrayList;

/**
 * 　　这个类只是包含了报错函数以及错误计数器。
 */
public class Err {
    /**
     * 错误计数器，编译过程中一共有多少个错误
     */
    public static int err = 0;

    /**
     * 错误内容记录
     */
    public static ArrayList<ErrorInfo> errors = new ArrayList<>();

    /**
     * 报错函数
     *
     * @param errCode 错误码
     */
    public static void report(int errCode) {
        char[] s = new char[PL0.scanner.charCounter - 1];
        java.util.Arrays.fill(s, ' ');
        String space = new String(s);
        System.out.println("****" + space + "!" + errCode);
        PL0.sourcePrintStream.println("****" + space + "!" + errCode);
        errors.add(new ErrorInfo(errCode, PL0.scanner.lineCounter, PL0.scanner.charCounter));
        err++;
    }

    public static void showResult() {
        System.out.println();
        for (int i = 0; i < errors.size(); i++) {
            System.out.println("Error in line " + errors.get(i).lineCount + ": " + errors.get(i).errorInfo);
        }
    }
}

package compiler.error;

import compiler.PL0;

import java.util.ArrayList;

/**
 * ���������ֻ�ǰ����˱������Լ������������
 */
public class Err {
    /**
     * ��������������������һ���ж��ٸ�����
     */
    public static int err = 0;

    /**
     * �������ݼ�¼
     */
    public static ArrayList<ErrorInfo> errors = new ArrayList<>();

    /**
     * ������
     *
     * @param errCode ������
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

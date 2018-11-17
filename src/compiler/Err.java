package compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

class ErrorInfo {
    int errCode;
    int lineCount;
    int posCount;
    String errorInfo;

    public ErrorInfo(int errCode, int lineCount, int posCount) {
        this.errCode = errCode;
        this.lineCount = lineCount;
        this.posCount = posCount;
        this.errorInfo = ErrorReason.get(errCode);
    }
}

class ErrorReason {
    public static Map<Integer, String> reason = new HashMap<>();

    public static void init() {
        reason.put(1, "�� = д���� :=");
        reason.put(2, "����˵�� = ��Ӧ������");
        reason.put(3, "����˵����ʶ��Ӧ�� =");
        reason.put(4, "procedure��ӦΪ��ʶ�� const ��Ӧ�Ǳ�ʶ�� var ��Ӧ�Ǳ�ʶ");
        reason.put(5, "©���˶��Ż��߷ֺ�");
        reason.put(9, "ȱ�پ��");
        reason.put(10, "ȱ�ٷֺ�");
        reason.put(11, "��ʶ��δ����");
        reason.put(12, "��ֵ����ʽ����");
        reason.put(13, "û�м�⵽��ֵ����");
        reason.put(14, "call��ӦΪ��ʶ��");
        reason.put(15, "call���ʶ��ӦΪ����");
        reason.put(16, "ȱ��then");
        reason.put(17, "ȱ��end��ֺ�");
        reason.put(18, "ȱ��do");
        reason.put(21, "����Ϊ����");
        reason.put(22, "ȱ��������");
        reason.put(31, "������ֵ��Χ");
        reason.put(32, "read()�еı�ʶ�����Ǳ���");
        reason.put(33, "write()��ӦΪ�������ʽ");
        reason.put(34, "��ʽ����Ӧ��������");
        reason.put(35, "read()��Ӧ���������ı�����");
    }

    public static String get(int errCode) {
        if (reason.get(errCode) != null) {
            return reason.get(errCode);
        } else {
            return "δ֪ԭ��";
        }
    }
}

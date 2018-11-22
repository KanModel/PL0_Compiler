/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2018-11-20-13:57
 */
package compiler.error;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: ��¼������Ϣ
 * @author: KanModel
 * @create: 2018-11-20 13:57
 */
public class ErrorReason {
    public static Map<Integer, String> reason = new HashMap<>();

    public static void init() {
        Err.errors.clear();
        Err.err = 0;

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
        reason.put(30, "������ֵ��Χ");
        reason.put(32, "read()�еı�ʶ�����Ǳ���");
        reason.put(33, "write()��ӦΪ�������ʽ");
        reason.put(34, "��ʽ����Ӧ��������");
        reason.put(35, "read()��Ӧ���������ı�����");
        reason.put(41, "ȱ�ٵ�����");
    }

    public static String get(int errCode) {
        if (reason.get(errCode) != null) {
            return reason.get(errCode);
        } else {
            return "δ֪ԭ��";
        }
    }
}


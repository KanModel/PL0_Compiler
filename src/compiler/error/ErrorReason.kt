/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2018-11-20-13:57
 */
package compiler.error

import java.util.HashMap

/**
 * @description: ��¼������Ϣ
 * @author: KanModel
 * @create: 2018-11-20 13:57
 */
object ErrorReason {
    var reason: MutableMap<Int, String> = HashMap()

    fun init() {
        Err.errors.clear()
        Err.err = 0

        reason[1] = "�� = д���� :="
        reason[2] = "����˵�� = ��Ӧ������"
        reason[3] = "����˵����ʶ��Ӧ�� ="
        reason[4] = "procedure��ӦΪ��ʶ�� const ��Ӧ�Ǳ�ʶ�� var ��Ӧ�Ǳ�ʶ"
        reason[5] = "©���˶��Ż��߷ֺ�"
        reason[9] = "ȱ�پ��"
        reason[10] = "ȱ�ٷֺ�"
        reason[11] = "��ʶ��δ����"
        reason[12] = "��ֵ����ʽ����"
        reason[13] = "û�м�⵽��ֵ����"
        reason[14] = "call��ӦΪ��ʶ��"
        reason[15] = "call���ʶ��ӦΪ����"
        reason[16] = "ȱ��then"
        reason[17] = "ȱ��end��ֺ�"
        reason[18] = "ȱ��do"
        reason[21] = "����Ϊ����"
        reason[22] = "ȱ��������"
        reason[31] = "������ֵ��Χ"
        reason[30] = "������ֵ��Χ"
        reason[32] = "read()�еı�ʶ�����Ǳ���"
        reason[33] = "write()��ӦΪ�������ʽ"
        reason[34] = "��ʽ����Ӧ��������"
        reason[35] = "read()��Ӧ���������ı�����"
        reason[41] = "ȱ�ٵ�����"
    }

    operator fun get(errCode: Int): String? {
        return if (reason[errCode] != null) {
            reason[errCode]
        } else {
            "δ֪ԭ��"
        }
    }
}


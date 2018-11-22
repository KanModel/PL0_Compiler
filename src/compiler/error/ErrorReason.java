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
 * @description: 记录错误信息
 * @author: KanModel
 * @create: 2018-11-20 13:57
 */
public class ErrorReason {
    public static Map<Integer, String> reason = new HashMap<>();

    public static void init() {
        Err.errors.clear();
        Err.err = 0;

        reason.put(1, "把 = 写成了 :=");
        reason.put(2, "常量说明 = 后应是数字");
        reason.put(3, "常量说明标识后应是 =");
        reason.put(4, "procedure后应为标识符 const 后应是标识符 var 后应是标识");
        reason.put(5, "漏掉了逗号或者分号");
        reason.put(9, "缺少句号");
        reason.put(10, "缺少分号");
        reason.put(11, "标识符未定义");
        reason.put(12, "赋值语句格式错误");
        reason.put(13, "没有检测到赋值符号");
        reason.put(14, "call后应为标识符");
        reason.put(15, "call后标识符应为过程");
        reason.put(16, "缺少then");
        reason.put(17, "缺少end或分号");
        reason.put(18, "缺少do");
        reason.put(21, "不能为过程");
        reason.put(22, "缺少右括号");
        reason.put(31, "超过数值范围");
        reason.put(30, "超过数值范围");
        reason.put(32, "read()中的标识符不是变量");
        reason.put(33, "write()中应为完整表达式");
        reason.put(34, "格式错误，应是左括号");
        reason.put(35, "read()中应是声明过的变量名");
        reason.put(41, "缺少单引号");
    }

    public static String get(int errCode) {
        if (reason.get(errCode) != null) {
            return reason.get(errCode);
        } else {
            return "未知原因";
        }
    }
}


package compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        reason.put(32, "read()中的标识符不是变量");
        reason.put(33, "write()中应为完整表达式");
        reason.put(34, "格式错误，应是左括号");
        reason.put(35, "read()中应是声明过的变量名");
    }

    public static String get(int errCode) {
        if (reason.get(errCode) != null) {
            return reason.get(errCode);
        } else {
            return "未知原因";
        }
    }
}

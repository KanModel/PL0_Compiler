/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2018-11-20-13:57
 */
package compiler.error

import java.util.HashMap

/**
 * @description: 记录错误信息
 * @author: KanModel
 * @create: 2018-11-20 13:57
 */
object ErrorReason {
    var reason: MutableMap<Int, String> = HashMap()

    fun init() {
        Err.errors.clear()
        Err.err = 0

        reason[1] = "把 = 写成了 :="
        reason[2] = "常量说明 = 后应是数字"
        reason[3] = "常量说明标识后应是 ="
        reason[4] = "procedure后应为标识符 const 后应是标识符 var 后应是标识"
        reason[5] = "漏掉了逗号或者分号"
        reason[9] = "缺少句号"
        reason[10] = "缺少分号"
        reason[11] = "标识符未定义"
        reason[12] = "赋值语句格式错误"
        reason[13] = "没有检测到赋值符号"
        reason[14] = "call后应为标识符"
        reason[15] = "call后标识符应为过程"
        reason[16] = "缺少then"
        reason[17] = "缺少end或分号"
        reason[18] = "缺少do"
        reason[21] = "不能为过程"
        reason[22] = "缺少右括号"
        reason[31] = "超过数值范围"
        reason[30] = "超过数值范围"
        reason[32] = "read()中的标识符不是变量"
        reason[33] = "write()中应为完整表达式"
        reason[34] = "格式错误，应是左括号"
        reason[35] = "read()中应是声明过的变量名"
        reason[41] = "缺少单引号"
    }

    operator fun get(errCode: Int): String? {
        return if (reason[errCode] != null) {
            reason[errCode]
        } else {
            "未知原因"
        }
    }
}


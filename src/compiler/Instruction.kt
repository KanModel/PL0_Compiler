package compiler

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2019-01-06-22:50
 */
/**
 * 　　这个类对应C语言版本中的 fct 枚举类型和 instruction 结构，代表虚拟机指令
 */
class Instruction(
        /**
         * 虚拟机代码指令
         */
        var f: Fct,
        /**
         * 引用层与声明层的层次差
         */
        var l: Int,
        /**
         * 指令参数
         */
        var a: Int)
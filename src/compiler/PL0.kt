package compiler

import compiler.error.Err
import compiler.error.ErrorReason

import java.io.*

/**
 *
 * 这个版本的 PL/0 编译器根据 C 语言的版本改写而成。两个版本在基本逻辑上是一致
 * 的，有些地方可能有所改动，例如getsym()和statement()两个函数，另外请注意C语言
 * 版本中的全局变量分散到构成编译器各个类中，为便于查找，保留了这些全局变量原来的名字。
 *
 *
 * 阅读过程中若有疑问请及时咨询你的助教。
 */
class PL0
/**
 * 构造函数，初始化编译器所有组成部分
 * @param fin PL/0 源文件的输入流
 */
(fin: BufferedReader) {

    init {
        // 各部件的构造函数中都含有C语言版本的 init() 函数的一部分代码
        table = Table()
        interpreter = Interpreter()
        scanner = Scanner(fin)
        parser = Parser(scanner, table, interpreter)
        inReader = fin
    }

    /**
     * 执行编译动作
     * @return 是否编译成功
     */
    fun compile(): Boolean {
        var abort = false

        try {
            parser.nextSymbol()        // 前瞻分析需要预先读入一个符号
            parser.parse()            // 开始语法分析过程（连同语法检查、目标代码生成）
        } catch (e: Error) {
            // 如果是发生严重错误则直接中止
            abort = true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            PL0.pcodePrintStream.close()
            PL0.sourcePrintStream.close()
            PL0.tablePrintStream.close()
        }
        if (abort)
            System.exit(0)

        // 编译成功是指完成编译过程并且没有错误
        return Err.err == 0
    }

    companion object {
        lateinit var inReader: BufferedReader

        // 编译程序的常数
        val SYMBOL_MAX_LENGTH = 10            // 符号的最大长度
        val MAX_NUM = 65565        // 最大允许的数值
        val CX_MAX = 500        // 最多的虚拟机代码数
        val LEVEL_MAX = 3            // 最大允许过程嵌套声明层数 [0, LEVEL_MAX]
        val MAX_NUM_DIGIT = 14            // number的最大位数
        val KEYWORD_COUNT = 32            // 关键字个数
        val TABLE_MAX = 200        // 名字表容量

        // 一些全局变量，其他关键的变量分布如下：
        // cx, code : compiler.Interpreter
        // dx : compiler.Parser
        // tableSize, table : compiler.Table
        var pcodePrintStream = PrintStream("pcode.tmp")// 输出虚拟机代码
        var sourcePrintStream = PrintStream("source.tmp")// 输出源文件及其各行对应的首地址
        var resultPrintStream = PrintStream("result.tmp")// 输出结果
        var tablePrintStream = PrintStream("table.tmp")// 输出名字表
        var listSwitch: Boolean = false            // 显示虚拟机代码与否
        var tableSwitch: Boolean = false            // 显示名字表与否

        // 一个典型的编译器的组成部分
        lateinit var scanner: Scanner                    // 词法分析器
        lateinit var parser: Parser                // 语法分析器
        lateinit var interpreter: Interpreter            // 类P-Code解释器（及目标代码生成工具）
        lateinit var table: Table                    // 名字表

        // 为避免多次创建BufferedReader，我们使用全局统一的Reader
        var stdin = BufferedReader(InputStreamReader(System.`in`))// 标准输入

        /**
         * 主函数
         */
        @JvmStatic
        fun main(args: Array<String>) {
            ErrorReason.init()

            // 原来 C 语言版的一些语句划分到compile()和Parser.parse()中
            var fname = ""
            stdin = BufferedReader(InputStreamReader(System.`in`))
            val fin: BufferedReader
            try {
                // 输入文件名
                print("Input pl/0 file?   ")
                while (fname == "")
                    fname = stdin.readLine()
                fin = BufferedReader(FileReader(fname), 4096)

                // 是否输出虚拟机代码
                fname = ""
                print("List object code?(Y/N)")
                while (fname == "")
                    fname = stdin.readLine()
                PL0.listSwitch = fname[0] == 'y' || fname[0] == 'Y'

                // 是否输出名字表
                fname = ""
                print("List symbol table?(Y/N)")
                while (fname == "")
                    fname = stdin.readLine()
                PL0.tableSwitch = fname[0] == 'y' || fname[0] == 'Y'

                PL0.sourcePrintStream = PrintStream("source.tmp")
                PL0.sourcePrintStream.println("Input pl/0 file?   $fname")

                // 构造编译器并初始化
                val pl0 = PL0(fin)

                if (pl0.compile()) {
                    // 如果成功编译则接着解释运行
                    PL0.resultPrintStream = PrintStream("result.tmp")
                    interpreter.interpret()
                    PL0.resultPrintStream.close()
                } else {
                    print("Errors in pl/0 program")
                    Err.showResult()
                }

            } catch (e: IOException) {
                println("Can't open file!")
            }

        }
    }
}

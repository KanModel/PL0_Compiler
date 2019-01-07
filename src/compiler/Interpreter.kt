package compiler

/**
 * 　　类P-Code代码解释器（含代码生成函数），这个类包含了C语言版中两个重要的全局变量 cx 和 code
 */
class Interpreter {
    // 解释执行时使用的栈大小
    private val STACK_SIZE = 500

    /**
     * 虚拟机代码指针，取值范围[0, CX_MAX-1]
     */
    var cx = 0

    /**
     * 存放虚拟机代码的数组
     */
    var code = arrayOfNulls<Instruction>(PL0.CX_MAX)

    /**
     * 生成虚拟机代码
     *
     * @param x instruction.f
     * @param y instruction.l
     * @param z instruction.a
     */
    fun generatePCode(x: Fct, y: Int, z: Int) {
        if (cx >= PL0.CX_MAX) {
            throw Error("Program too long")
        }

        code[cx] = Instruction(x, y, z)
        cx++
    }

    /**
     * 输出目标代码清单
     *
     * @param start 开始输出的位置
     */
    fun listCode(start: Int) {
        if (PL0.listSwitch) {
            for (i in start until cx) {
                val msg = i.toString() + " " + code[i]!!.f + " " + code[i]!!.l + " " + code[i]!!.a
                println(msg)
                PL0.pcodePrintStream.println(msg)
            }
        }
    }

    /**
     * 解释程序
     */
    fun interpret() {
        var p: Int
        var b: Int
        var t: Int                        // 指令指针，指令基址，栈顶指针 t代表栈顶下一个新空间的位置
        var ins: Instruction?                            // 存放当前指令
        val s = IntArray(STACK_SIZE)        // 栈

        println("start pl0")
        p = 0
        b = p
        t = b
        s[2] = 0
        s[1] = s[2]
        s[0] = s[1]
        do {
            ins = code[p]                    // 读当前指令
            p++
            when (ins!!.f) {
                Fct.LIT                // 将a的值取到栈顶
                -> {
                    s[t] = ins.a
                    t++
                }
                Fct.OPR                // 数学、逻辑运算
                -> when (ins.a) {
                    0 -> {
                        t = b
                        p = s[t + 2]
                        b = s[t + 1]
                    }
                    1 -> s[t - 1] = -s[t - 1]
                    2 -> {
                        t--
                        s[t - 1] = s[t - 1] + s[t]
                    }
                    3 -> {
                        t--
                        s[t - 1] = s[t - 1] - s[t]
                    }
                    4 -> {
                        t--
                        s[t - 1] = s[t - 1] * s[t]
                    }
                    5 -> {
                        t--
                        s[t - 1] = s[t - 1] / s[t]
                    }
                    6 -> s[t - 1] = s[t - 1] % 2
                    8 -> {
                        t--
                        s[t - 1] = if (s[t - 1] == s[t]) 1 else 0
                    }
                    9 -> {
                        t--
                        s[t - 1] = if (s[t - 1] != s[t]) 1 else 0
                    }
                    10 -> {
                        t--
                        s[t - 1] = if (s[t - 1] < s[t]) 1 else 0
                    }
                    11 -> {
                        t--
                        s[t - 1] = if (s[t - 1] >= s[t]) 1 else 0
                    }
                    12 -> {
                        t--
                        s[t - 1] = if (s[t - 1] > s[t]) 1 else 0
                    }
                    13 -> {
                        t--
                        s[t - 1] = if (s[t - 1] <= s[t]) 1 else 0
                    }
                    14 -> {
                        print(s[t - 1])
                        PL0.resultPrintStream.print(s[t - 1])
                        t--
                    }
                    15 -> {
                        println()
                        PL0.resultPrintStream.println()
                    }
                    16 -> {
                        //                            System.out.print("Input:");
                        //                            PL0.resultPrintStream.print("Input:");
                        s[t] = 0
                        try {
                            s[t] = Integer.parseInt(PL0.stdin.readLine())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        PL0.resultPrintStream.println(s[t])
                        t++
                    }
                    17 -> s[t - 1] = s[t - 1] + 1
                    18 -> s[t - 1] = s[t - 1] - 1
                    19 -> s[t - 1] = Math.sqrt(s[t - 1].toDouble()).toInt()
                    20 -> {
                        print(s[t - 1].toChar())
                        PL0.resultPrintStream.print(s[t - 1].toChar())
                        t--
                    }
                    21 -> {
                        t--
                        s[t - 1] = s[t - 1] % s[t]
                    }
                    22 -> s[t - 1] = (if (s[t - 1] == 0) 1 else 0)
                }//为mod添加的新的虚拟机指令
                Fct.LOD                // 取相对当前过程的数据基地址为a的内存的值到栈顶
                -> {
                    s[t] = s[base(ins.l, s, b) + ins.a]
                    t++
                }
                Fct.LAD                // 以栈顶的数据为偏移量读取数据
                -> s[t - 1] = s[base(ins.l, s, b) + ins.a - s[t - 1]]
                Fct.STO                // 栈顶的值存到相对当前过程的数据基地址为a的内存
                -> {
                    t--
                    s[base(ins.l, s, b) + ins.a] = s[t]
                }
                Fct.STA                // 以次栈顶的数据为偏移量保存栈顶数据
                -> {
                    s[base(ins.l, s, b) + ins.a - s[t - 2]] = s[t - 1]
                    t -= 2
                }
                Fct.CAL                // 调用子过程
                -> {
                    s[t] = base(ins.l, s, b)    // 将静态作用域基地址入栈
                    s[t + 1] = b                    // 将动态作用域基地址入栈
                    s[t + 2] = p                    // 将当前指令指针入栈
                    b = t                    // 改变基地址指针值为新过程的基地址
                    p = ins.a                    // 跳转
                }
                Fct.INT            // 分配内存
                -> t += ins.a
                Fct.JMP                // 直接跳转
                -> p = ins.a
                Fct.JPC                // 条件跳转（当栈顶为0的时候跳转）
                -> {
                    t--
                    if (s[t] == 0)
                        p = ins.a
                }
            }
        } while (p != 0)
        println()
    }

    /**
     * 通过给定的层次差来获得该层的堆栈帧基地址
     *
     * @param l 目标层次与当前层次的层次差
     * @param s 运行栈
     * @param b 当前层堆栈帧基地址
     * @return 目标层次的堆栈帧基地址
     */
    private fun base(l: Int, s: IntArray, b: Int): Int {
        var level = l
        var b1 = b
        while (level > 0) {
            b1 = s[b1]
            level--
        }
        return b1
    }
}

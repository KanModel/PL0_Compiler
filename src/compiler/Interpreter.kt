package compiler

/**
 * ������P-Code��������������������ɺ�����������������C���԰���������Ҫ��ȫ�ֱ��� cx �� code
 */
class Interpreter {
    // ����ִ��ʱʹ�õ�ջ��С
    private val STACK_SIZE = 500

    /**
     * ���������ָ�룬ȡֵ��Χ[0, CX_MAX-1]
     */
    var cx = 0

    /**
     * �����������������
     */
    var code = arrayOfNulls<Instruction>(PL0.CX_MAX)

    /**
     * �������������
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
     * ���Ŀ������嵥
     *
     * @param start ��ʼ�����λ��
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
     * ���ͳ���
     */
    fun interpret() {
        var p: Int
        var b: Int
        var t: Int                        // ָ��ָ�룬ָ���ַ��ջ��ָ�� t����ջ����һ���¿ռ��λ��
        var ins: Instruction?                            // ��ŵ�ǰָ��
        val s = IntArray(STACK_SIZE)        // ջ

        println("start pl0")
        p = 0
        b = p
        t = b
        s[2] = 0
        s[1] = s[2]
        s[0] = s[1]
        do {
            ins = code[p]                    // ����ǰָ��
            p++
            when (ins!!.f) {
                Fct.LIT                // ��a��ֵȡ��ջ��
                -> {
                    s[t] = ins.a
                    t++
                }
                Fct.OPR                // ��ѧ���߼�����
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
                }//Ϊmod��ӵ��µ������ָ��
                Fct.LOD                // ȡ��Ե�ǰ���̵����ݻ���ַΪa���ڴ��ֵ��ջ��
                -> {
                    s[t] = s[base(ins.l, s, b) + ins.a]
                    t++
                }
                Fct.LAD                // ��ջ��������Ϊƫ������ȡ����
                -> s[t - 1] = s[base(ins.l, s, b) + ins.a - s[t - 1]]
                Fct.STO                // ջ����ֵ�浽��Ե�ǰ���̵����ݻ���ַΪa���ڴ�
                -> {
                    t--
                    s[base(ins.l, s, b) + ins.a] = s[t]
                }
                Fct.STA                // �Դ�ջ��������Ϊƫ��������ջ������
                -> {
                    s[base(ins.l, s, b) + ins.a - s[t - 2]] = s[t - 1]
                    t -= 2
                }
                Fct.CAL                // �����ӹ���
                -> {
                    s[t] = base(ins.l, s, b)    // ����̬���������ַ��ջ
                    s[t + 1] = b                    // ����̬���������ַ��ջ
                    s[t + 2] = p                    // ����ǰָ��ָ����ջ
                    b = t                    // �ı����ַָ��ֵΪ�¹��̵Ļ���ַ
                    p = ins.a                    // ��ת
                }
                Fct.INT            // �����ڴ�
                -> t += ins.a
                Fct.JMP                // ֱ����ת
                -> p = ins.a
                Fct.JPC                // ������ת����ջ��Ϊ0��ʱ����ת��
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
     * ͨ�������Ĳ�β�����øò�Ķ�ջ֡����ַ
     *
     * @param l Ŀ�����뵱ǰ��εĲ�β�
     * @param s ����ջ
     * @param b ��ǰ���ջ֡����ַ
     * @return Ŀ���εĶ�ջ֡����ַ
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

package compiler

import compiler.error.Err


/**
 * 　　这个类封装了PL/0编译器的符号表，C语言版本中关键的全局变量tx和table[]就在这里。
 */
class Table {

    /**
     * 名字表，请使用get()函数访问
     *
     * @see .get
     */
    private val table = arrayOfNulls<Item>(PL0.TABLE_MAX)

    /**
     * 当前名字表项指针，也可以理解为当前有效的名字表大小（table size）
     */
    var tableSize = 0

    /**
     * 　　即C语言版本中的tablestruct结构。
     */
    class Item {
        internal var name: String? = null        // 名字
        internal var kind: Objekt? = null        // 类型：const, var or procedure
        internal var `val`: Int = 0            // 数值，仅const使用
        internal var level: Int = 0            // 所处层，var和procedure使用
        internal var adr: Int = 0            // 地址，var和procedure使用
        internal var size: Int = 0            // 需要分配的数据区空间, 仅procedure使用
    }

    /**
     * 获得名字表某一项的内容
     *
     * @param i 名字表中的位置
     * @return 名字表第 i 项的内容
     */
    operator fun get(i: Int): Item? {
        if (table[i] == null) {
            table[i] = Item()
            table[i]!!.name = ""
        }
        return table[i]
    }

    fun copyItem(copy: Item): Item {
        val clone = Item()
        clone.adr = copy.adr
        clone.kind = copy.kind
        clone.name = copy.name
        clone.level = copy.level
        clone.`val` = copy.`val`
        clone.size = copy.size
        return clone
    }

    /**
     * 把某个符号登陆到名字表中，注意参数跟C语言版本不同
     *
     * @param symbolType 该符号的类型：const, var, procedure
     * @param level      名字所在的层次
     * @param dx         当前应分配的变量的相对地址，注意调用enter()后dx要加一
     */
    fun enter(symbolType: Objekt, level: Int, dx: Int) {
        tableSize++
        val item = get(tableSize)
        if (item != null) {
            item.name = PL0.scanner.id
            item.kind = symbolType
            when (symbolType) {
                Objekt.constant                    // 常量名字
                -> if (PL0.scanner.num > PL0.MAX_NUM) {
                    Err.report(31)        // 数字过大溢出
                    item.`val` = 0
                } else {
                    item.`val` = PL0.scanner.num
                }
                Objekt.variable                    // 变量名字
                -> {
                    item.level = level
                    item.adr = dx
                }
                Objekt.array                    // 变量名字
                -> {
                    item.level = level
                    item.adr = dx
                }
                Objekt.procedure                    // 过程名字
                -> item.level = level
            }
        }            // 注意id和num都是从词法分析器获得
    }

    /**
     * 打印符号表内容，摘自C语言版本的 block() 函数。
     *
     * @param start 当前作用域符号表区间的左端
     */
    fun debugTable(start: Int) {
        if (!PL0.tableSwitch)
            return
        println("TABLE:")
        if (start >= tableSize)
            println("    NULL")
        for (i in start + 1..tableSize) {
            var msg = "OOPS! UNKNOWN TABLE ITEM!"
            when (table[i]!!.kind) {
                Objekt.constant -> msg = "    " + i + " const " + table[i]!!.name + " val=" + table[i]!!.`val`
                Objekt.variable -> msg = "    " + i + " var   " + table[i]!!.name + " lev=" + table[i]!!.level + " addr=" + table[i]!!.adr
                Objekt.procedure -> msg = "    " + i + " proc  " + table[i]!!.name + " lev=" + table[i]!!.level + " addr=" + table[i]!!.adr + " size=" + table[i]!!.size
                Objekt.array -> msg = "    " + i + " array  " + table[i]!!.name + " lev=" + table[i]!!.level + " addr=" + table[i]!!.adr
            }
            println(msg)
            PL0.tablePrintStream.println(msg)
        }
        println()
    }

    /**
     * 在名字表中查找某个名字的位置
     *
     * @param idt 要查找的名字
     * @return 如果找到则返回名字项的下标，否则返回0
     */
    fun position(idt: String): Int {
        for (i in tableSize downTo 1)
            if (get(i)!!.name == idt)
                return i

        return 0
    }
}

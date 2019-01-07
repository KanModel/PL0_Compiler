package compiler

import compiler.error.Err


/**
 * ����������װ��PL/0�������ķ��ű�C���԰汾�йؼ���ȫ�ֱ���tx��table[]�������
 */
class Table {

    /**
     * ���ֱ���ʹ��get()��������
     *
     * @see .get
     */
    private val table = arrayOfNulls<Item>(PL0.TABLE_MAX)

    /**
     * ��ǰ���ֱ���ָ�룬Ҳ�������Ϊ��ǰ��Ч�����ֱ��С��table size��
     */
    var tableSize = 0

    /**
     * ������C���԰汾�е�tablestruct�ṹ��
     */
    class Item {
        internal var name: String? = null        // ����
        internal var kind: Object? = null        // ���ͣ�const, var or procedure
        internal var `val`: Int = 0            // ��ֵ����constʹ��
        internal var level: Int = 0            // �����㣬var��procedureʹ��
        internal var adr: Int = 0            // ��ַ��var��procedureʹ��
        internal var size: Int = 0            // ��Ҫ������������ռ�, ��procedureʹ��
    }

    /**
     * ������ֱ�ĳһ�������
     *
     * @param i ���ֱ��е�λ��
     * @return ���ֱ�� i �������
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
     * ��ĳ�����ŵ�½�����ֱ��У�ע�������C���԰汾��ͬ
     *
     * @param symbolType �÷��ŵ����ͣ�const, var, procedure
     * @param level      �������ڵĲ��
     * @param dx         ��ǰӦ����ı�������Ե�ַ��ע�����enter()��dxҪ��һ
     */
    fun enter(symbolType: Object, level: Int, dx: Int) {
        tableSize++
        val item = get(tableSize)
        if (item != null) {
            item.name = PL0.scanner.id
            item.kind = symbolType
            when (symbolType) {
                Object.constant                    // ��������
                -> if (PL0.scanner.num > PL0.MAX_NUM) {
                    Err.report(31)        // ���ֹ������
                    item.`val` = 0
                } else {
                    item.`val` = PL0.scanner.num
                }
                Object.variable                    // ��������
                -> {
                    item.level = level
                    item.adr = dx
                }
                Object.array                    // ��������
                -> {
                    item.level = level
                    item.adr = dx
                }
                Object.procedure                    // ��������
                -> item.level = level
            }
        }            // ע��id��num���ǴӴʷ����������
    }

    /**
     * ��ӡ���ű����ݣ�ժ��C���԰汾�� block() ������
     *
     * @param start ��ǰ��������ű���������
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
                Object.constant -> msg = "    " + i + " const " + table[i]!!.name + " val=" + table[i]!!.`val`
                Object.variable -> msg = "    " + i + " var   " + table[i]!!.name + " lev=" + table[i]!!.level + " addr=" + table[i]!!.adr
                Object.procedure -> msg = "    " + i + " proc  " + table[i]!!.name + " lev=" + table[i]!!.level + " addr=" + table[i]!!.adr + " size=" + table[i]!!.size
                Object.array -> msg = "    " + i + " array  " + table[i]!!.name + " lev=" + table[i]!!.level + " addr=" + table[i]!!.adr
            }
            println(msg)
            PL0.tablePrintStream.println(msg)
        }
        println()
    }

    /**
     * �����ֱ��в���ĳ�����ֵ�λ��
     *
     * @param idt Ҫ���ҵ�����
     * @return ����ҵ��򷵻���������±꣬���򷵻�0
     */
    fun position(idt: String): Int {
        for (i in tableSize downTo 1)
            if (get(i)!!.name == idt)
                return i

        return 0
    }
}

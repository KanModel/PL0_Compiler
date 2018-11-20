package compiler;

import compiler.error.Err;

/**
 * �������ͣ�Ϊ�����Java�Ĺؼ���Object��ͻ�����Ǹĳ�Objekt
 */
enum Objekt {
    constant, variable, procedure, array
}

/**
 * ����������װ��PL/0�������ķ��ű�C���԰汾�йؼ���ȫ�ֱ���tx��table[]�������
 */
public class Table {
    /**
     * ������C���԰汾�е�tablestruct�ṹ��
     */
    public class Item {
        String name;        // ����
        Objekt kind;        // ���ͣ�const, var or procedure
        int val;            // ��ֵ����constʹ��
        int level;            // �����㣬var��procedureʹ��
        int adr;            // ��ַ��var��procedureʹ��
        int size;            // ��Ҫ������������ռ�, ��procedureʹ��
    }

    /**
     * ���ֱ���ʹ��get()��������
     *
     * @see #get(int)
     */
    private Item[] table = new Item[PL0.TABLE_MAX];

    /**
     * ��ǰ���ֱ���ָ�룬Ҳ�������Ϊ��ǰ��Ч�����ֱ��С��table size��
     */
    public int tableSize = 0;

    /**
     * ������ֱ�ĳһ�������
     *
     * @param i ���ֱ��е�λ��
     * @return ���ֱ�� i �������
     */
    public Item get(int i) {
        if (table[i] == null) {
            table[i] = new Item();
            table[i].name = "";
        }
        return table[i];
    }

    public Item copyItem(Item copy){
        Item clone = new Item();
        clone.adr = copy.adr;
        clone.kind = copy.kind;
        clone.name = copy.name;
        clone.level = copy.level;
        clone.val = copy.val;
        clone.size = copy.size;
        return clone;
    }

    /**
     * ��ĳ�����ŵ�½�����ֱ��У�ע�������C���԰汾��ͬ
     *
     * @param symbolType �÷��ŵ����ͣ�const, var, procedure
     * @param level      �������ڵĲ��
     * @param dx         ��ǰӦ����ı�������Ե�ַ��ע�����enter()��dxҪ��һ
     */
    public void enter(Objekt symbolType, int level, int dx) {
        tableSize++;
        Item item = get(tableSize);
        item.name = PL0.scanner.id;            // ע��id��num���ǴӴʷ����������
        item.kind = symbolType;
        switch (symbolType) {
            case constant:                    // ��������
                if (PL0.scanner.num > PL0.MAX_NUM) {
                    Err.report(31);        // ���ֹ������
                    item.val = 0;
                } else {
                    item.val = PL0.scanner.num;
                }
                break;
            case variable:                    // ��������
                item.level = level;
                item.adr = dx;
                break;
            case array:                    // ��������
                item.level = level;
                item.adr = dx;
                break;
            case procedure:                    // ��������
                item.level = level;
                break;
        }
    }

    /**
     * ��ӡ���ű����ݣ�ժ��C���԰汾�� block() ������
     *
     * @param start ��ǰ��������ű���������
     */
    public void debugTable(int start) {
        if (!PL0.tableSwitch)
            return;
        System.out.println("TABLE:");
        if (start >= tableSize)
            System.out.println("    NULL");
        for (int i = start + 1; i <= tableSize; i++) {
            String msg = "OOPS! UNKNOWN TABLE ITEM!";
            switch (table[i].kind) {
                case constant:
                    msg = "    " + i + " const " + table[i].name + " val=" + table[i].val;
                    break;
                case variable:
                    msg = "    " + i + " var   " + table[i].name + " lev=" + table[i].level + " addr=" + table[i].adr;
                    break;
                case procedure:
                    msg = "    " + i + " proc  " + table[i].name + " lev=" + table[i].level + " addr=" + table[i].adr + " size=" + table[i].size;
                    break;
                case array:
                    msg = "    " + i + " array  " + table[i].name + " lev=" + table[i].level + " addr=" + table[i].adr;
                    break;
            }
            System.out.println(msg);
            PL0.tablePrintStream.println(msg);
        }
        System.out.println();
    }

    /**
     * �����ֱ��в���ĳ�����ֵ�λ��
     *
     * @param idt Ҫ���ҵ�����
     * @return ����ҵ��򷵻���������±꣬���򷵻�0
     */
    public int position(String idt) {
        for (int i = tableSize; i > 0; i--)
            if (get(i).name.equals(idt))
                return i;

        return 0;
    }
}

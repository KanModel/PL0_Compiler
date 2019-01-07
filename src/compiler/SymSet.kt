package compiler

import java.util.BitSet

/**
 * ���ǰ� java.util.BitSet ��װһ�£��Ա��ڱ�д����
 */
class SymSet
/**
 * ����һ�����ż���
 * @param nbits ������ϵ�����
 */
(nbits: Int) : BitSet(nbits) {

    /**
     * ��һ�����ŷŵ�������
     * @param s Ҫ���õķ���
     */
    fun set(s: Symbol) {
        set(s.ordinal)
    }

    /**
     * ���һ�������Ƿ��ڼ�����
     * @param s Ҫ���ķ���
     * @return �������ڼ����У��򷵻�true�����򷵻�false
     */
    operator fun get(s: Symbol): Boolean {
        return get(s.ordinal)
    }

    companion object {

        /**
         * �����û���ر�����
         */
        private val serialVersionUID = 8136959240158320958L
    }
}

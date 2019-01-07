package compiler

import java.util.BitSet

/**
 * 我们把 java.util.BitSet 包装一下，以便于编写代码
 */
class SymSet
/**
 * 构造一个符号集合
 * @param nBits 这个集合的容量
 */
(nBits: Int) : BitSet(nBits) {

    /**
     * 把一个符号放到集合中
     * @param s 要放置的符号
     */
    fun set(s: Symbol) {
        set(s.ordinal)
    }

    /**
     * 检查一个符号是否在集合中
     * @param s 要检查的符号
     * @return 若符号在集合中，则返回true，否则返回false
     */
    operator fun get(s: Symbol): Boolean {
        return get(s.ordinal)
    }

    companion object {

        /**
         * 这个域没有特别意义
         */
        private const val serialVersionUID = 8136959240158320958L
    }
}

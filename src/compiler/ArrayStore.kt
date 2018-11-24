package compiler

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2018-11-24-14:42
 */
/**
 * @description: 数组信息存储
 * @author: KanModel
 * @create: 2018-11-24 14:42
 */
object ArrayStore {

    val arrayInfoList = ArrayList<ArrayInfo>()

    data class ArrayInfo(val ident: String, var dim: Int = 1, val seq: ArrayList<Int> = ArrayList())

    fun add(ident: String, size: Int) {
        var isFind = false
        for (i in arrayInfoList) {
            if (i.ident == ident) {
                isFind = true
                i.dim++
                i.seq.add(size)
                break
            }
        }
        if (!isFind) {
            val info = ArrayInfo(ident)
            info.seq.add(size)
            arrayInfoList.add(info)
        }
    }

    fun get(ident: String): ArrayInfo? {
        for (i in arrayInfoList) {
            if (i.ident == ident) {
                return i
            }
        }
        return null
    }
}

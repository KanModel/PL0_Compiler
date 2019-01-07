package compiler.error

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2018-11-20-14:27
 */

/**
 * @description: ¥ÌŒÛ–≈œ¢¿‡
 * @author: KanModel
 * @create: 2018-11-20 14:27
 */
class ErrorInfo(internal var errCode: Int, internal var lineCount: Int, internal var posCount: Int) {
    internal var errorInfo: String

    init {
        this.errorInfo = ErrorReason.get(errCode)!!
    }
}

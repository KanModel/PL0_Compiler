package compiler.error;/**
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
public class ErrorInfo {
    int errCode;
    int lineCount;
    int posCount;
    String errorInfo;

    public ErrorInfo(int errCode, int lineCount, int posCount) {
        this.errCode = errCode;
        this.lineCount = lineCount;
        this.posCount = posCount;
        this.errorInfo = ErrorReason.get(errCode);
    }
}

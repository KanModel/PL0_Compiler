package compiler

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2019-01-06-22:58
 */
/**
 * 符号类型，为避免和Java的关键字Object冲突，我们改成Objekt
 */
enum class Object {
    constant, variable, procedure, array
}
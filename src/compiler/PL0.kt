package compiler

import compiler.error.Err
import compiler.error.ErrorReason

import java.io.*

/**
 *
 * ����汾�� PL/0 ���������� C ���Եİ汾��д���ɡ������汾�ڻ����߼�����һ��
 * �ģ���Щ�ط����������Ķ�������getsym()��statement()����������������ע��C����
 * �汾�е�ȫ�ֱ�����ɢ�����ɱ������������У�Ϊ���ڲ��ң���������Щȫ�ֱ���ԭ�������֡�
 *
 *
 * �Ķ����������������뼰ʱ��ѯ������̡�
 */
class PL0
/**
 * ���캯������ʼ��������������ɲ���
 * @param fin PL/0 Դ�ļ���������
 */
(fin: BufferedReader) {

    init {
        // �������Ĺ��캯���ж�����C���԰汾�� init() ������һ���ִ���
        table = Table()
        interpreter = Interpreter()
        scanner = Scanner(fin)
        parser = Parser(scanner, table, interpreter)
        inReader = fin
    }

    /**
     * ִ�б��붯��
     * @return �Ƿ����ɹ�
     */
    fun compile(): Boolean {
        var abort = false

        try {
            parser.nextSymbol()        // ǰհ������ҪԤ�ȶ���һ������
            parser.parse()            // ��ʼ�﷨�������̣���ͬ�﷨��顢Ŀ��������ɣ�
        } catch (e: Error) {
            // ����Ƿ������ش�����ֱ����ֹ
            abort = true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            PL0.pcodePrintStream.close()
            PL0.sourcePrintStream.close()
            PL0.tablePrintStream.close()
        }
        if (abort)
            System.exit(0)

        // ����ɹ���ָ��ɱ�����̲���û�д���
        return Err.err == 0
    }

    companion object {
        lateinit var inReader: BufferedReader

        // �������ĳ���
        val SYMBOL_MAX_LENGTH = 10            // ���ŵ���󳤶�
        val MAX_NUM = 65565        // ����������ֵ
        val CX_MAX = 500        // ���������������
        val LEVEL_MAX = 3            // ����������Ƕ���������� [0, LEVEL_MAX]
        val MAX_NUM_DIGIT = 14            // number�����λ��
        val KEYWORD_COUNT = 32            // �ؼ��ָ���
        val TABLE_MAX = 200        // ���ֱ�����

        // һЩȫ�ֱ����������ؼ��ı����ֲ����£�
        // cx, code : compiler.Interpreter
        // dx : compiler.Parser
        // tableSize, table : compiler.Table
        var pcodePrintStream = PrintStream("pcode.tmp")// ������������
        var sourcePrintStream = PrintStream("source.tmp")// ���Դ�ļ�������ж�Ӧ���׵�ַ
        var resultPrintStream = PrintStream("result.tmp")// ������
        var tablePrintStream = PrintStream("table.tmp")// ������ֱ�
        var listSwitch: Boolean = false            // ��ʾ������������
        var tableSwitch: Boolean = false            // ��ʾ���ֱ����

        // һ�����͵ı���������ɲ���
        lateinit var scanner: Scanner                    // �ʷ�������
        lateinit var parser: Parser                // �﷨������
        lateinit var interpreter: Interpreter            // ��P-Code����������Ŀ��������ɹ��ߣ�
        lateinit var table: Table                    // ���ֱ�

        // Ϊ�����δ���BufferedReader������ʹ��ȫ��ͳһ��Reader
        var stdin = BufferedReader(InputStreamReader(System.`in`))// ��׼����

        /**
         * ������
         */
        @JvmStatic
        fun main(args: Array<String>) {
            ErrorReason.init()

            // ԭ�� C ���԰��һЩ��仮�ֵ�compile()��Parser.parse()��
            var fname = ""
            stdin = BufferedReader(InputStreamReader(System.`in`))
            val fin: BufferedReader
            try {
                // �����ļ���
                print("Input pl/0 file?   ")
                while (fname == "")
                    fname = stdin.readLine()
                fin = BufferedReader(FileReader(fname), 4096)

                // �Ƿ�������������
                fname = ""
                print("List object code?(Y/N)")
                while (fname == "")
                    fname = stdin.readLine()
                PL0.listSwitch = fname[0] == 'y' || fname[0] == 'Y'

                // �Ƿ�������ֱ�
                fname = ""
                print("List symbol table?(Y/N)")
                while (fname == "")
                    fname = stdin.readLine()
                PL0.tableSwitch = fname[0] == 'y' || fname[0] == 'Y'

                PL0.sourcePrintStream = PrintStream("source.tmp")
                PL0.sourcePrintStream.println("Input pl/0 file?   $fname")

                // �������������ʼ��
                val pl0 = PL0(fin)

                if (pl0.compile()) {
                    // ����ɹ���������Ž�������
                    PL0.resultPrintStream = PrintStream("result.tmp")
                    interpreter.interpret()
                    PL0.resultPrintStream.close()
                } else {
                    print("Errors in pl/0 program")
                    Err.showResult()
                }

            } catch (e: IOException) {
                println("Can't open file!")
            }

        }
    }
}

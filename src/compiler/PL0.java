package compiler;

import java.io.*;

/**
 *<p>����汾�� PL/0 ���������� C ���Եİ汾��д���ɡ������汾�ڻ����߼�����һ��
 *�ģ���Щ�ط����������Ķ�������getsym()��statement()����������������ע��C����
 *�汾�е�ȫ�ֱ�����ɢ�����ɱ������������У�Ϊ���ڲ��ң���������Щȫ�ֱ���ԭ�������֡�</p>
 *
 *<p>�Ķ����������������뼰ʱ��ѯ������̡�</p>
 */
public class PL0 {
	// �������ĳ���
	public static final int SYMBOL_MAX_LENGTH = 10;			// ���ŵ���󳤶�
	public static final int MAX_NUM = 2047;		// ����������ֵ
	public static final int CX_MAX = 500;		// ���������������
	public static final int LEVEL_MAX = 3;			// ����������Ƕ���������� [0, LEVEL_MAX]
	public static final int MAX_NUM_DIGIT = 14;			// number�����λ��
	public static final int KEYWORD_COUNT = 32;			// �ؼ��ָ���
	public static final int TABLE_MAX = 100;		// ���ֱ�����
	
	// һЩȫ�ֱ����������ؼ��ı����ֲ����£�
	// cx, code : compiler.Interpreter
	// dx : compiler.Parser
	// tableSize, table : compiler.Table
	public static PrintStream pcodePrintStream;				// ������������
	public static PrintStream sourcePrintStream;				// ���Դ�ļ�������ж�Ӧ���׵�ַ
	public static PrintStream resultPrintStream;				// ������
	public static PrintStream tablePrintStream;				// ������ֱ�
	public static boolean listSwitch;			// ��ʾ������������
	public static boolean tableSwitch;			// ��ʾ���ֱ����
	
	// һ�����͵ı���������ɲ���
	public static Scanner scanner;					// �ʷ�������
	public static Parser  parser;				// �﷨������
	public static Interpreter interpreter;			// ��P-Code����������Ŀ��������ɹ��ߣ�
	public static Table table;					// ���ֱ�
	
	// Ϊ�����δ���BufferedReader������ʹ��ȫ��ͳһ��Reader
	public static BufferedReader stdin;			// ��׼����
	
	/**
	 * ���캯������ʼ��������������ɲ���
	 * @param fin PL/0 Դ�ļ���������
	 */
	public PL0(BufferedReader fin) {
		// �������Ĺ��캯���ж�����C���԰汾�� init() ������һ���ִ���
		table = new Table();
		interpreter = new Interpreter();
		scanner = new Scanner(fin);
		parser = new Parser(scanner, table, interpreter);
	}

	/**
	 * ִ�б��붯��
	 * @return �Ƿ����ɹ�
	 */
	boolean compile() {
		boolean abort = false;
		
		try {
			PL0.pcodePrintStream = new PrintStream("pcode.tmp");
			PL0.tablePrintStream = new PrintStream("table.tmp");
			parser.nextSymbol();		// ǰհ������ҪԤ�ȶ���һ������
			parser.parse();			// ��ʼ�﷨�������̣���ͬ�﷨��顢Ŀ��������ɣ�
		} catch (Error e) {
			// ����Ƿ������ش�����ֱ����ֹ
			abort = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally { 
			PL0.pcodePrintStream.close();
			PL0.sourcePrintStream.close();
			PL0.tablePrintStream.close();
		}
		if (abort)
			System.exit(0);
				
		// ����ɹ���ָ��ɱ�����̲���û�д���
		return (Err.err == 0);
	}

	/**
	 * ������
	 */
	public static void main(String[] args) {
	    ErrorReason.init();

		// ԭ�� C ���԰��һЩ��仮�ֵ�compile()��Parser.parse()��
		String fname = "D:\\ALL_WORKSPACE\\JAVA\\Course_CS\\PL0_JAVA\\demo\\pcode4.txt";//todo
		stdin = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader fin;
		try {
			// �����ļ���
			System.out.print("Input pl/0 file?   ");
			while (fname.equals(""))
				fname = stdin.readLine();
			fin = new BufferedReader(new FileReader(fname), 4096);

			// �Ƿ�������������
			fname = "y";//todo
			System.out.print("List object code?(Y/N)");
			while (fname.equals(""))
				fname = stdin.readLine();
			PL0.listSwitch = (fname.charAt(0)=='y' || fname.charAt(0)=='Y');

			// �Ƿ�������ֱ�
			fname = "y";//todo
			System.out.print("List symbol table?(Y/N)");
			while (fname.equals(""))
				fname = stdin.readLine();
			PL0.tableSwitch = (fname.charAt(0)=='y' || fname.charAt(0)=='Y');

			PL0.sourcePrintStream = new PrintStream("source.tmp");
			PL0.sourcePrintStream.println("Input pl/0 file?   " + fname);

			// �������������ʼ��
			PL0 pl0 = new PL0(fin);

			if (pl0.compile()) {
				// ����ɹ���������Ž�������
				PL0.resultPrintStream = new PrintStream("result.tmp");
				interpreter.interpret();
				PL0.resultPrintStream.close();
			} else {
				System.out.print("Errors in pl/0 program");
				Err.showResult();
			}

		} catch (IOException e) {
			System.out.println("Can't open file!");
		}

		System.out.println();
	}
}

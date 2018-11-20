package me.kanmodel.nov18.ui

import compiler.error.Err
import compiler.error.ErrorReason
import compiler.PL0
import java.awt.*
import java.io.*
import javax.swing.*

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: KanModel
 * Date: 2018-11-20-13:39
 */
/**
 * @description: todo
 * @author: KanModel
 * @create: 2018-11-20 13:39
 */
class CompilerFrame : JFrame() {
    private val jTextArea: JTextArea = JTextArea(10, 40)
    private var jScrollPane: JScrollPane = JScrollPane(jTextArea)
    private var open: FileDialog = FileDialog(this, "���ĵ�", FileDialog.LOAD)
    private var save: FileDialog = FileDialog(this, "�����ĵ�", FileDialog.SAVE)

    init {
        jTextArea.font = Font("Monospaced", 1, 20)
        jTextArea.lineWrap = true//����ָ���������
        jScrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        jScrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        jScrollPane.setViewportView(jTextArea)

        val menuBar = JMenuBar()//�˵���
        val fileMenu = JMenu("�ļ�")
        val projectMenu = JMenu("��Ŀ")
        val helpMenu = JMenu("����")
        val openItem = JMenuItem("��")
        val closeItem = JMenuItem("�ر�")
        val saveItem = JMenuItem("����")
        val aboutItem = JMenuItem("����")
        val compileItem = JMenuItem("����")
        val runItem = JMenuItem("����")
        menuBar.add(fileMenu)
        menuBar.add(projectMenu)
        menuBar.add(helpMenu)
        fileMenu.add(openItem)
        fileMenu.add(saveItem)
        fileMenu.add(closeItem)
        projectMenu.add(compileItem)
        projectMenu.add(runItem)
        helpMenu.add(aboutItem)
        openItem.addActionListener {
            showFileOpenDialog(this)
        }
        saveItem.addActionListener {
            if (file == null) {
                newFile()
            }
            saveFile()
        }
        compileItem.addActionListener {
            compileFile(fileString)
        }
        runItem.addActionListener {
            run()
        }
        closeItem.addActionListener { System.exit(0) }


        add(menuBar, BorderLayout.NORTH)
        add(jScrollPane, BorderLayout.CENTER)

        title = "PL0 Compiler"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        size = Dimension(540, 270)
        setLocationRelativeTo(null)
//        isResizable = false
    }

    private fun compileFile(file: String) {
        ErrorReason.init()

        PL0.stdin = BufferedReader(InputStreamReader(System.`in`))
        val fin: BufferedReader
        try {
            fin = BufferedReader(FileReader(file), 4096)

            // �Ƿ�������������
            PL0.listSwitch = true

            // �Ƿ�������ֱ�
            PL0.tableSwitch = true

            PL0.sourcePrintStream = PrintStream("source.tmp")
            PL0.sourcePrintStream.println("Input pl/0 file?   $file")

            // �������������ʼ��
            val pl0 = PL0(fin)

            isCompileSuccess = pl0.compile()
            if (isCompileSuccess) {
                print("Compile succeed\n")
            } else {
                print("Errors in pl/0 program\n")
                Err.showResult()
            }

        } catch (e: IOException) {
            println("Can't open file!")
        }
    }

    private fun run() {
        if (isCompileSuccess) {
            PL0.resultPrintStream = PrintStream("result.tmp")
            PL0.interpreter.interpret()
            PL0.resultPrintStream.close()
        } else {
            JOptionPane.showMessageDialog(frame, "���ȱ���ɹ�������!", "����", JOptionPane.WARNING_MESSAGE)
        }
    }

    private fun showFileOpenDialog(parent: Component) {
        /*val fileChooser = JFileChooser()

        fileChooser.currentDirectory = File(".")

        fileChooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
        fileChooser.isMultiSelectionEnabled = false

        val result = fileChooser.showOpenDialog(parent)

        if (result == JFileChooser.APPROVE_OPTION) {
            isCompileSuccess = false
            file = fileChooser.selectedFile
            val selectedFile = fileChooser.selectedFile
            fileString = selectedFile.absolutePath
            println(selectedFile.absolutePath)
            println(selectedFile.parentFile)
        }*/
        open.isVisible = true

        val dirPath = open.directory
        val fileName = open.file
        fileString = dirPath + fileName
        println(dirPath)
        println(fileName)
        if (dirPath == null || fileName == null) {
            return
        }
        file = File(dirPath, fileName)

        jTextArea.text = ""//���ļ�֮ǰ����ı�����

        try {
            val br = BufferedReader(FileReader(file))
            var line: String?
            line = br.readLine()
            while (line != null) {
                //�������ı�׷�ӵ��ĵ���β�����ģ��Ϊ null �����ַ���Ϊ null ��գ���ִ���κβ�����
                //��Ȼ����� Swing ���������̰߳�ȫ�ģ����˷������̰߳�ȫ�ġ�
                jTextArea.append(line + "\r\n")
                line = br.readLine()
            }
        } catch (ex: IOException) {
            throw RuntimeException("��ȡʧ�ܣ�")
        }
    }

    //�½�һ���ļ�
    private fun newFile() {
        if (file == null) {
            save.isVisible = true
            val dirPath = save.directory
            val fileName = save.file
            if (dirPath == null || fileName == null) {
                return
            }
            file = File(dirPath, fileName)
        }
    }

    //�����ļ�
    private fun saveFile() {
        try {
            val bw = BufferedWriter(FileWriter(file))
            val text = jTextArea.text
            bw.write(text)
            bw.close()
        } catch (ex: IOException) {
            throw RuntimeException()
        }
    }

    companion object {
        private var file: File? = null
        var isCompileSuccess = false
        lateinit var fileString: String
        val frame = CompilerFrame()


        @JvmStatic
        fun main(args: Array<String>) {
            frame.isVisible = true
        }
    }
}
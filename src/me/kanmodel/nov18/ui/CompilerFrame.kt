package me.kanmodel.nov18.ui

import compiler.error.Err
import compiler.error.ErrorReason
import compiler.PL0
import java.awt.*
import java.io.*
import javax.swing.*
import javax.swing.JTextPane

/**
 * @description: 编译器客户端
 * @author: KanModel
 * @create: 2018-11-20 13:39
 */
class CompilerFrame : JFrame() {
    private val editor = JTextPane()
    private var jScrollPane: JScrollPane = JScrollPane(editor)
    private var open: FileDialog = FileDialog(this, "打开文档", FileDialog.LOAD)
    private var save: FileDialog = FileDialog(this, "保存文档", FileDialog.SAVE)

    init {
        initHighlightPane()
        jScrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        jScrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        jScrollPane.setViewportView(editor)

        val menuBar = JMenuBar()//菜单栏
        val fileMenu = JMenu("文件")
        val projectMenu = JMenu("项目")
        val helpMenu = JMenu("帮助")
        val openItem = JMenuItem("打开")
        val closeItem = JMenuItem("关闭")
        val saveItem = JMenuItem("保存")
        val aboutItem = JMenuItem("关于")
        val compileItem = JMenuItem("编译")
        val runItem = JMenuItem("运行")
        val compileAndRunItem = JMenuItem("编译&运行")
        menuBar.add(fileMenu)
        menuBar.add(projectMenu)
        menuBar.add(helpMenu)
        fileMenu.add(openItem)
        fileMenu.add(saveItem)
        fileMenu.add(closeItem)
        projectMenu.add(compileItem)
        projectMenu.add(runItem)
        projectMenu.add(compileAndRunItem)
        helpMenu.add(aboutItem)
        openItem.addActionListener {
            showFileOpenDialog()
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
        compileAndRunItem.addActionListener {
            compileFile(fileString)
            run()
        }
        closeItem.addActionListener { System.exit(0) }
        aboutItem.addActionListener { JOptionPane.showMessageDialog(null, "PL0 编译器客户端") }

        add(menuBar, BorderLayout.NORTH)
        add(jScrollPane, BorderLayout.CENTER)

        title = "PL0 Compiler"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        size = Dimension(800, 600)
        setLocationRelativeTo(null)
//        isResizable = false
    }

    private fun initHighlightPane(){
        editor.document.addDocumentListener(SyntaxHighlighter(editor))
        editor.background = Color.black
        editor.font = Font("Monospaced", 1, 20)
    }

    private fun compileFile(file: String?) {
        ErrorReason.init()

        if (file == null) {
            JOptionPane.showMessageDialog(frame, "请先打开文件!", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        PL0.stdin = BufferedReader(InputStreamReader(System.`in`))
        val fin: BufferedReader
        try {
            fin = BufferedReader(FileReader(file), 4096)

            // 是否输出虚拟机代码
            PL0.listSwitch = true

            // 是否输出名字表
            PL0.tableSwitch = true

            PL0.sourcePrintStream = PrintStream("source.tmp")
            PL0.sourcePrintStream.println("Input pl/0 file?   $file")

            // 构造编译器并初始化
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
            JOptionPane.showMessageDialog(frame, "请先编译成功后运行!", "警告", JOptionPane.WARNING_MESSAGE)
        }
    }

    private fun showFileOpenDialog() {
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

        editor.text = ""//打开文件之前清空文本区域

        try {
            val br = BufferedReader(FileReader(file))
            var line: String?
            line = br.readLine()
            while (line != null) {
                //将给定文本追加到文档结尾。如果模型为 null 或者字符串为 null 或空，则不执行任何操作。
                //虽然大多数 Swing 方法不是线程安全的，但此方法是线程安全的。
//                jTextArea.append(line + "\r\n")
                editor.text = editor.text + line + "\r\n"
                line = br.readLine()
            }
        } catch (ex: IOException) {
            throw RuntimeException("读取失败！")
        }
    }

    //新建一个文件
    private fun newFile() {
        if (file == null) {
            save.isVisible = true
            val dirPath = save.directory
            val fileName = save.file
            fileString = dirPath + fileName
            if (dirPath == null || fileName == null) {
                return
            }
            file = File(dirPath, fileName)
        }
    }

    //保存文件
    private fun saveFile() {
        try {
            val bw = BufferedWriter(FileWriter(file))
            val text = editor.text
            bw.write(text)
            bw.close()
        } catch (ex: IOException) {
            throw RuntimeException()
        }
    }

    companion object {
        private var file: File? = null
        var isCompileSuccess = false
        var fileString: String? = null
        val frame = CompilerFrame()


        @JvmStatic
        fun main(args: Array<String>) {
            frame.isVisible = true
        }
    }
}
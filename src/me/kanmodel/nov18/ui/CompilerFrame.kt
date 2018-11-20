package me.kanmodel.nov18.ui

import compiler.error.Err
import compiler.error.ErrorReason
import compiler.PL0
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
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

    init {
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
        compileItem.addActionListener {
            compileFile(fileString)
        }
        runItem.addActionListener {
            run()
        }

        add(menuBar, BorderLayout.NORTH)

        title = "PL0 Compiler"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        size = Dimension(540, 270)
        setLocationRelativeTo(null)
        isResizable = false
    }

    companion object {
        var isCompileSuccess = false
        lateinit var fileString: String
        val frame = CompilerFrame()


        @JvmStatic
        fun main(args: Array<String>) {
            frame.isVisible = true
        }

        fun compileFile(file: String) {
            ErrorReason.init()

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

        fun run() {
            if (isCompileSuccess) {
                PL0.resultPrintStream = PrintStream("result.tmp")
                PL0.interpreter.interpret()
                PL0.resultPrintStream.close()
            } else {
                JOptionPane.showMessageDialog(frame, "请先编译成功后运行!", "警告", JOptionPane.WARNING_MESSAGE)
            }
        }

        fun showFileOpenDialog(parent: Component) {
            val fileChooser = JFileChooser()

            fileChooser.currentDirectory = File(".")

            fileChooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            fileChooser.isMultiSelectionEnabled = false

            val result = fileChooser.showOpenDialog(parent)

            if (result == JFileChooser.APPROVE_OPTION) {
                isCompileSuccess = false
                val file = fileChooser.selectedFile
                fileString = file.absolutePath
                println(file.absolutePath)
                println(file.parentFile)
            }
        }
    }
}
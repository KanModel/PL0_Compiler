package me.kanmodel.nov18.ui

import compiler.ArrayStore
import compiler.error.Err
import compiler.error.ErrorReason
import compiler.PL0
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.*
import java.lang.Exception
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
        val fileMenu = JMenu("File")
        fileMenu.setMnemonic('F')
        val projectMenu = JMenu("Project")
        projectMenu.setMnemonic('j')
        val helpMenu = JMenu("Help")
        helpMenu.setMnemonic('H')
        val newItem = JMenuItem("New")
        newItem.setMnemonic('N')
        newItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK)
        val openItem = JMenuItem("Open")
        openItem.setMnemonic('O')
        openItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK)
        val closeItem = JMenuItem("Exit")
        val saveItem = JMenuItem("Save")
        saveItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK)
        saveItem.setMnemonic('S')
        val aboutItem = JMenuItem("About")
        val compileItem = JMenuItem("Compile")
        val runItem = JMenuItem("Run")
        val compileAndRunItem = JMenuItem("Compile&Run")
        compileAndRunItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F5, KeyEvent.SHIFT_MASK)
        menuBar.add(fileMenu)
        menuBar.add(projectMenu)
        menuBar.add(helpMenu)
        fileMenu.add(newItem)
        fileMenu.add(openItem)
        fileMenu.add(saveItem)
        fileMenu.add(closeItem)
        projectMenu.add(compileItem)
        projectMenu.add(runItem)
        projectMenu.add(compileAndRunItem)
        helpMenu.add(aboutItem)
        newItem.addActionListener {
            newFile()
        }
        openItem.addActionListener {
            openFile()
        }
        saveItem.addActionListener {
            if (file == null) {
                createNewFile()
            }
            if (file != null) {
                saveFile()
            }
        }
        compileItem.addActionListener {
            Thread {
                compileFile(fileString)
            }.start()
        }
        val runItemAction = ActionListener {
            Thread {
                run()
            }.start()
        }
        runItem.addActionListener(runItemAction)
        runItem.registerKeyboardAction(runItemAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)
        compileAndRunItem.addActionListener {
            Thread {
                compileFile(fileString)
                run()
            }.start()
        }
        closeItem.addActionListener { System.exit(0) }
        aboutItem.addActionListener {
            JOptionPane.showMessageDialog(null, "\n" +
                    "             PL0 编译器客户端\n" +
                    "                         ——by KanModel\n" +
                    "https://github.com/KanModel/PL0_Compiler")
        }

        val paneKeyListener = object : KeyListener{
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent?) {
                if (e != null) {
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        var pos = editor.caretPosition - 2
                        var end : Int = editor.caretPosition - 1
//                        println(end)
                        val doc = editor.document
                        var tabCount = 0
                        while (doc.getText(pos, 1)[0] != '\n' && pos > 0) {
                            if (doc.getText(pos, 1)[0].toInt() == 9)
                                tabCount++
                            pos--
                        }
                        if (pos == 0) {
                            if (doc.getText(pos, 1)[0].toInt() == 9) {
                                tabCount++
                            }
                        }
                        while (doc.getText(end, 1)[0] != '\n' && end < doc.endPosition.offset){}
                        end++
//                        println(tabCount)
                        if (tabCount > 0) {
//                            SwingUtilities.invokeLater {
//                                editor.document.remove(end, 1)
//                            }
//                            var str = "${13.toChar()}\n"
                            var str = ""
                            for (i in 1..tabCount) {
                                str = "$str${9.toChar()}"
//                                println(str)
                            }
                            SwingUtilities.invokeLater {
                                editor.document.insertString(end, str, null)
                            }
                        }
                    }
                }
            }
            override fun keyPressed(e: KeyEvent?) {
                if (e != null) {
                    if (e.isControlDown && e.keyCode == KeyEvent.VK_D) {
                        val pos = editor.caretPosition
                        val doc = editor.document
                        var start : Int = pos - 1
                        var end : Int = pos
                        while (doc.getText(start, 1)[0] != '\n' && start > 0)
                            start--
                        while (doc.getText(end, 1)[0] != '\n' && end < doc.endPosition.offset)
                            end++
                        var copy = doc.getText(start, end - start)
                        copy = copy.replace("\n", "")
//                        println(copy)
//                        println(end - start + 1)
                        SwingUtilities.invokeLater {
                            editor.document.insertString(end, "${13.toChar()}\n$copy", null)
                        }
                        SwingUtilities.invokeLater{
                            try {
                                if (start == 0) {
                                    editor.caretPosition += end - start + 2
                                } else {
                                    editor.caretPosition += end - start + 1
                                }
                            } catch (e: Exception) {

                            }
                        }
                    }
//                    if (e.isShiftDown && e.keyCode == KeyEvent.VK_ENTER) {
//                        val pos = editor.caretPosition
//                        val doc = editor.document
//                        var start : Int = pos - 1
//                        var end : Int = pos
//                        var tabCount = 0
//                        while (doc.getText(start, 1)[0] != '\n' && start > 0) {
//                            if (doc.getText(pos, 1)[0].toInt() == 9)
//                                tabCount++
//                            start--
//                        }
//                        while (doc.getText(end, 1)[0] != '\n' && end < doc.endPosition.offset)
//                            end++
//                        editor.document.insertString(end, "${13.toChar()}\n", null)
//                    }
                }
            }
        }
        editor.addKeyListener(paneKeyListener)

        add(menuBar, BorderLayout.NORTH)
        add(jScrollPane, BorderLayout.CENTER)

        title = "new$titleName"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        size = Dimension(800, 600)
        setLocationRelativeTo(null)
//        isResizable = false
    }

    private fun initHighlightPane() {
        editor.document.addDocumentListener(SyntaxHighlighter(editor))
        editor.background = Color.black
        editor.font = Font("Monospaced", 1, 20)
    }

    private fun compileFile(file: String?) {
        if (Companion.file == null) {
            createNewFile()
        }
        saveFile()//先保存后编译
        ErrorReason.init()
        ArrayStore.arrayInfoList.clear()

        if (file == null) {
            JOptionPane.showMessageDialog(frame, "请先打开文件!", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        val fin: BufferedReader
        try {
            fin = BufferedReader(FileReader(file), 4096)

            // 是否输出虚拟机代码
            PL0.listSwitch = true

            // 是否输出名字表
            PL0.tableSwitch = true

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
            PL0.interpreter.interpret()
            PL0.resultPrintStream.close()
        } else {
            JOptionPane.showMessageDialog(frame, "请先编译成功后运行!", "警告", JOptionPane.WARNING_MESSAGE)
        }
    }

    private fun newFile() {
        title = "new$titleName"
        editor.text = ""//打开文件之前清空文本区域
        file = null
    }

    private fun openFile() {
        open.isVisible = true

        val dirPath = open.directory
        val fileName = open.file
        if (dirPath == null || fileName == null) {
            return
        }
        fileString = dirPath + fileName
        println("打开文件: $fileString")
        frame.title = "$fileString$titleName"
        file = File(dirPath, fileName)

        editor.text = ""//打开文件之前清空文本区域

        try {
            val br = BufferedReader(FileReader(file))
            var line: String?
            line = br.readLine()
            var text = ""
            while (line != null) {
                //将给定文本追加到文档结尾。如果模型为 null 或者字符串为 null 或空，则不执行任何操作。
                //虽然大多数 Swing 方法不是线程安全的，但此方法是线程安全的。
//                jTextArea.append(line + "\r\n")
                text = text + line + "\r\n"
                line = br.readLine()
            }
            SwingUtilities.invokeLater {
                editor.text = text
            }
        } catch (ex: IOException) {
            throw RuntimeException("读取失败！")
        }
    }

    //新建一个文件
    private fun createNewFile() {
        if (file == null) {
            save.isVisible = true
            if (save.directory != null) {
                val dirPath = save.directory
                val fileName = save.file
                fileString = dirPath + fileName
                frame.title = "$fileString$titleName"
                if (dirPath == null || fileName == null) {
                    return
                }
                file = File(dirPath, fileName)
            }
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
        const val titleName = " - PL0 Compiler"
        private var file: File? = null
        var isCompileSuccess = false
        var fileString: String? = null


        val frame = CompilerFrame()

        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                frame.isVisible = true
            }
        }
    }
}
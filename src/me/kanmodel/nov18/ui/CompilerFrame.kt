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
import kotlin.concurrent.thread

/**
 * @description: 编译器客户端
 * @author: KanModel
 * @create: 2018-11-20 13:39
 */
class CompilerFrame : JFrame() {
    private val editor = JTextPane()
    private var jScrollPane: JScrollPane = JScrollPane(editor)
    private var open: FileDialog = FileDialog(this, "Open File", FileDialog.LOAD)
    private var save: FileDialog = FileDialog(this, "Save File", FileDialog.SAVE)

    init {
        initHighlightPane()
        jScrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        jScrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        jScrollPane.setViewportView(editor)
        jScrollPane.setRowHeaderView(LineNumberHeaderView())

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
        closeItem.setMnemonic('E')
        val saveItem = JMenuItem("Save")
        saveItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK)
        saveItem.setMnemonic('S')
        val compileItem = JMenuItem("Compile")
        compileItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F6, KeyEvent.SHIFT_MASK)
        compileItem.setMnemonic('C')
        val runItem = JMenuItem("Run")
        runItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.SHIFT_MASK)
        runItem.setMnemonic('U')
        val compileAndRunItem = JMenuItem("Compile&Run")
        compileAndRunItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F5, KeyEvent.SHIFT_MASK)
        compileAndRunItem.setMnemonic('R')
        val shortcutItem = JMenuItem("Shortcut")
        shortcutItem.setMnemonic('O')
        val aboutItem = JMenuItem("About")
        aboutItem.setMnemonic('A')
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
        helpMenu.add(shortcutItem)
        helpMenu.add(aboutItem)
        newItem.addActionListener {
            thread(start = true) {
                println("new thread")
                newFile()
            }
        }
        openItem.addActionListener {
            thread(start = true) {
                println("open thread")
                openFile()
            }
        }
        saveItem.addActionListener {
            thread(start = true) {
                if (file == null) {
                    createNewFile()
                    saveFile()
                } else {
                    saveFile()
                    SwingUtilities.invokeLater {
                        if (isEdited) {
                            isEdited = false
                            frame.title = "$fileString$titleName"
                        }
                    }
                }
            }
        }
        val compileItemAction = ActionListener {
            thread(start = true) {
                compileFile(fileString)
            }
        }
        compileItem.addActionListener(compileItemAction)
        val runItemAction = ActionListener {
            thread(start = true) {
                run()
            }
        }
        runItem.addActionListener(runItemAction)
        runItem.registerKeyboardAction(runItemAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)
        compileItem.registerKeyboardAction(compileItemAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)
        compileAndRunItem.addActionListener {
            thread(start = true) {
                compileFile(fileString)
                run()
            }
        }
        closeItem.addActionListener { System.exit(0) }
        val shortCutMessage = JLabel("<html><table>\n" +
                "<br>Shortcut List</br>\n" +
                "<tr>\n" +
                "  <td>New</td>\n" +
                "  <td>Ctrl-N</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Open</td>\n" +
                "  <td>Ctrl-O</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Save</td>\n" +
                "  <td>Ctrl-S</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Compile</td>\n" +
                "  <td>F6/Shift-F6</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Run</td>\n" +
                "  <td>F5/Shift-U</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Compile&Run</td>\n" +
                "  <td>Shift-F5</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Quick Copy</td>\n" +
                "  <td>Ctrl-D</td>\n" +
                "</tr>\n" +
                "</table></html>")
        shortcutItem.addActionListener {
            thread(start = true) {
                JOptionPane.showMessageDialog(null, shortCutMessage)
            }
        }
        aboutItem.addActionListener {
            thread(start = true) {
                JOptionPane.showMessageDialog(null, "\n" +
                        "             PL0 Editor\n" +
                        "                         ——by KanModel\n" +
                        "https://github.com/KanModel/PL0_Compiler")
            }
        }

        val paneKeyListener = object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent?) {
                if (e != null) {
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        val doc = editor.document
                        if (doc.length > 2) {
                            var pos = editor.caretPosition - 2
                            var end: Int = editor.caretPosition - 1
//                        println(end)
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
                            while (doc.getText(end, 1)[0] != '\n' && end < doc.endPosition.offset) {
                            }
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
            }

            override fun keyPressed(e: KeyEvent?) {
                if (e != null) {
                    if (e.isControlDown && e.keyCode == KeyEvent.VK_D) {

                        val doc = editor.document
                        if (doc.length > 2) {
                            val pos = editor.caretPosition
                            var start: Int = pos - 1
                            var end: Int = pos
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
                            SwingUtilities.invokeLater {
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
        if (isEdited) {
            isEdited = false
            SwingUtilities.invokeLater {
                title = "$fileString$titleName"
            }
        }
        ErrorReason.init()
        ArrayStore.arrayInfoList.clear()

        if (file == null) {
            JOptionPane.showMessageDialog(frame, "Please open file!", "Waring", JOptionPane.WARNING_MESSAGE)
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
            JOptionPane.showMessageDialog(frame, "Please compile", "Waring", JOptionPane.WARNING_MESSAGE)
        }
    }

    private fun newFile() {
        SwingUtilities.invokeLater {
            editor.text = ""//打开文件之前清空文本区域
            title = "new$titleName"
        }
        file = null
        fileString = null
        isEdited = false
    }

    private fun openFile() {
        open.isVisible = true

        val dirPath = open.directory
        val fileName = open.file
        if (dirPath == null || fileName == null) {
            return
        }
        fileString = dirPath + fileName
        println("Open file: $fileString")
        isCompileSuccess = false
        file = File(dirPath, fileName)

        editor.text = ""//打开文件之前清空文本区域

        try {
            val br = BufferedReader(FileReader(file))
            var line: String?
            line = br.readLine()
            var text = ""
            while (line != null) {
                text = text + line + "\r\n"
                line = br.readLine()
            }
            SwingUtilities.invokeLater {
                editor.text = text
                isEdited = false
                frame.title = "$fileString$titleName"
            }
        } catch (ex: IOException) {
//            throw RuntimeException("Read failed")
            println("Read failed, maybe the file not exists.")
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
                SwingUtilities.invokeLater {
                    frame.title = "$fileString$titleName"
                }
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
        const val titleName = " - PL0 Compiler & Editor"
        private var file: File? = null
        var isCompileSuccess = false
        var fileString: String? = null


        var isEdited = false
        val frame = CompilerFrame()

        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                frame.isVisible = true
            }
        }
    }
}
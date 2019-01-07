package compiler

import compiler.error.Err

/**
 * 　　语法分析器。这是PL/0分析器中最重要的部分，在语法分析的过程中穿插着语法错误检查和目标代码生成。
 */
class Parser(val scanner: Scanner, val table: Table, val interpreter: Interpreter) {

    companion object {
        private val SYMBOL_NUM = Symbol.values().size
    }

    // 表示声明开始的符号集合、表示语句开始的符号集合、表示因子开始的符号集合
    // 实际上这就是声明、语句和因子的FIRST集合
    private val declarationBeginSet: SymSet
    private val statementBeginSet: SymSet
    private val factorBeginSet: SymSet

    /**
     * 当前符号，由nextSymbol()读入
     *
     * @see .nextSymbol
     */
    private var currentSymbol: Symbol? = null

    /**
     * 当前作用域的堆栈帧大小，或者说数据大小（data size）
     */
    private var dataSize = 0

    init {

        // 设置声明First集
        declarationBeginSet = SymSet(SYMBOL_NUM)
        declarationBeginSet.set(Symbol.constSym)
        declarationBeginSet.set(Symbol.varSym)
        declarationBeginSet.set(Symbol.procSym)

        // 设置语句First集
        statementBeginSet = SymSet(SYMBOL_NUM)
        statementBeginSet.set(Symbol.beginSym)
        statementBeginSet.set(Symbol.callSym)
        statementBeginSet.set(Symbol.ifSym)
        statementBeginSet.set(Symbol.whileSym)
        statementBeginSet.set(Symbol.forSym)
        statementBeginSet.set(Symbol.readSym)            // thanks to elu
        statementBeginSet.set(Symbol.plusplus)
        statementBeginSet.set(Symbol.minusminus)
        statementBeginSet.set(Symbol.writeSym)
        statementBeginSet.set(Symbol.writelnSym)
        statementBeginSet.set(Symbol.printSym)
        statementBeginSet.set(Symbol.printlnSym)

        // 设置因子First集
        factorBeginSet = SymSet(SYMBOL_NUM)
        factorBeginSet.set(Symbol.ident)
        factorBeginSet.set(Symbol.number)
        factorBeginSet.set(Symbol.lParen)
        factorBeginSet.set(Symbol.sqrtSym)

    }

    /**
     * 启动语法分析过程，此前必须先调用一次nextSymbol()
     *
     * @see .nextSymbol
     */
    fun parse() {
        //        SymSet nxtlev = new SymSet(SYMBOL_NUM);
        val nextLevel = SymSet(SYMBOL_NUM)
        nextLevel.or(declarationBeginSet)//并操作
        nextLevel.or(statementBeginSet)
        nextLevel.set(Symbol.period)
        parseBlock(0, nextLevel)

        if (currentSymbol != Symbol.period)
            Err.report(9)
    }

    /**
     * 获得下一个语法符号，这里只是简单调用一下getSymbol()
     */
    fun nextSymbol() {
        scanner.getSymbol()
        currentSymbol = scanner.currentSymbol
        isComment()
    }

    /**
     * 测试当前符号是否合法
     *
     * @param s1      我们需要的符号
     * @param s2      如果不是我们需要的，则需要一个补救用的集合
     * @param errCode 错误号
     */
    private fun test(s1: SymSet, s2: SymSet, errCode: Int) {
        // 在某一部分（如一条语句，一个表达式）将要结束时时我们希望下一个符号属于某集合
        //（该部分的后跟符号），test负责这项检测，并且负责当检测不通过时的补救措施，程
        // 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符
        // 号），以及检测不通过时的错误号。
        if (!s1[currentSymbol!!]) {
            Err.report(errCode)
            // 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
            while (!s1[currentSymbol!!] && !s2[currentSymbol!!])
                nextSymbol()
        }
    }

    /**
     * 分析<分程序>
     *
     * @param level 当前分程序所在层
     * @param fsys  当前模块后跟符号集
    </分程序> */
    private fun parseBlock(level: Int, fsys: SymSet) {
        // <分程序> := [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>

        val dx0: Int
        val tx0: Int
        val cx0: Int                // 保留初始dx，tx和cx
        var nextLevel: SymSet

        dx0 = dataSize                        // 记录本层之前的数据量（以便恢复）
        dataSize = 3
        tx0 = table.tableSize                    // 记录本层名字的初始位置（以便恢复）
        table[table.tableSize]!!.adr = interpreter.cx

        interpreter.generatePCode(Fct.JMP, 0, 0)

        if (level > PL0.LEVEL_MAX)
            Err.report(32)

        // 分析<说明部分>
        do {
            // <常量说明部分>
            if (currentSymbol == Symbol.constSym) {
                nextSymbol()
                // the original do...while(currentSymbol == ident) is problematic, thanks to calculous
                // do
                parseConstDeclaration(level)
                while (currentSymbol == Symbol.comma) {
                    nextSymbol()
                    parseConstDeclaration(level)
                }

                if (currentSymbol == Symbol.semicolon)
                    nextSymbol()
                else
                    Err.report(5)                // 漏掉了逗号或者分号
                // } while (currentSymbol == ident);
            }

            // <变量说明部分>
            if (currentSymbol == Symbol.varSym) {
                nextSymbol()
                // the original do...while(currentSymbol == ident) is problematic, thanks to calculous
                // do {
                parseVarDeclaration(level)
                while (currentSymbol == Symbol.comma) {
                    nextSymbol()
                    parseVarDeclaration(level)
                }

                if (currentSymbol == Symbol.semicolon)
                    nextSymbol()
                else
                    Err.report(5)                // 漏掉了逗号或者分号
                // } while (currentSymbol == ident);

            }

            // <数组说明部分>
            if (currentSymbol == Symbol.arraySym) {
                nextSymbol()
                parseArrayDeclaration(level)
                while (currentSymbol == Symbol.comma) {
                    nextSymbol()
                    parseArrayDeclaration(level)
                }

                if (currentSymbol == Symbol.semicolon)
                    nextSymbol()
                else
                    Err.report(5)                // 漏掉了逗号或者分号
            }

            // <过程说明部分>
            while (currentSymbol == Symbol.procSym) {
                nextSymbol()
                if (currentSymbol == Symbol.ident) {
                    table.enter(Object.procedure, level, dataSize)
                    nextSymbol()
                } else {
                    Err.report(4)                // procedure后应为标识符
                }

                if (currentSymbol == Symbol.semicolon) {
                    nextSymbol()
                } else {
                    Err.report(5)                // 漏掉了分号
                }
                nextLevel = fsys.clone() as SymSet
                nextLevel.set(Symbol.semicolon)
                parseBlock(level + 1, nextLevel)

                if (currentSymbol == Symbol.semicolon) {
                    nextSymbol()
                    nextLevel = statementBeginSet.clone() as SymSet
                    nextLevel.set(Symbol.ident)
                    nextLevel.set(Symbol.procSym)
                    test(nextLevel, fsys, 6)
                } else {
                    Err.report(5)                // 漏掉了分号
                }
            }


            nextLevel = statementBeginSet.clone() as SymSet
            nextLevel.set(Symbol.ident)
            test(nextLevel, declarationBeginSet, 7)
        } while (declarationBeginSet[currentSymbol!!])        // 直到没有声明符号

        // 开始生成当前过程代码
        val item = table[tx0]
        if (item != null) {
            interpreter.code[item.adr]!!.a = interpreter.cx
            item.adr = interpreter.cx                    // 当前过程代码地址
            item.size = dataSize                            // 声明部分中每增加一条声明都会给dx增加1，
        }
        // 声明部分已经结束，dx就是当前过程的堆栈帧大小
        cx0 = interpreter.cx
        interpreter.generatePCode(Fct.INT, 0, dataSize)            // 生成分配内存代码

        table.debugTable(tx0)

        // 分析<语句>
        nextLevel = fsys.clone() as SymSet        // 每个后跟符号集和都包含上层后跟符号集和，以便补救
        nextLevel.set(Symbol.semicolon)        // 语句后跟符号为分号或end
        nextLevel.set(Symbol.endSym)
        parseStatement(nextLevel, level)
        interpreter.generatePCode(Fct.OPR, 0, 0)        // 每个过程出口都要使用的释放数据段指令

        nextLevel = SymSet(SYMBOL_NUM)    // 分程序没有补救集合
        test(fsys, nextLevel, 8)// 检测后跟符号正确性

        interpreter.listCode(cx0)

        dataSize = dx0                            // 恢复堆栈帧计数器
        table.tableSize = tx0                        // 回复名字表位置
    }

    /**
     * 分析<常量说明部分>
     *
     * @param level 当前所在的层次
    </常量说明部分> */
    private fun parseConstDeclaration(level: Int) {
        if (currentSymbol == Symbol.ident) {
            nextSymbol()
            if (currentSymbol == Symbol.equal || currentSymbol == Symbol.becomes) {
                if (currentSymbol == Symbol.becomes)
                    Err.report(1)            // 把 = 写成了 :=
                nextSymbol()
                if (currentSymbol == Symbol.number) {
                    table.enter(Object.constant, level, dataSize)
                    nextSymbol()
                } else {
                    Err.report(2)            // 常量说明 = 后应是数字
                }
            } else {
                Err.report(3)                // 常量说明标识后应是 =
            }
        } else {
            Err.report(4)                    // const 后应是标识符
        }
    }

    /**
     * 分析<变量说明部分>
     *
     * @param level 当前层次
    </变量说明部分> */
    private fun parseVarDeclaration(level: Int) {
        if (currentSymbol == Symbol.ident) {
            // 填写名字表并改变堆栈帧计数器
            table.enter(Object.variable, level, dataSize)
            dataSize++
            nextSymbol()
        } else {
            Err.report(4)                    // var 后应是标识
        }
    }

    /**
     * 分析<语句>
     *
     * @param fsys  后跟符号集
     * @param level 当前层次
    </语句> */
    private fun parseStatement(fsys: SymSet, level: Int) {
        val nxtlev: SymSet
        when (currentSymbol) {
            Symbol.ident -> parseAssignStatement(fsys, level)
            Symbol.readSym -> parseReadStatement(fsys, level)
            Symbol.callSym -> parseCallStatement(fsys, level)
            Symbol.ifSym -> parseIfStatement(fsys, level)
            Symbol.beginSym -> parseBeginStatement(fsys, level)
            Symbol.whileSym -> parseWhileStatement(fsys, level)
            Symbol.forSym -> parseForStatement(fsys, level)
            Symbol.plusplus -> parsePlusMinusAssign(fsys, level)
            Symbol.minusminus -> parsePlusMinusAssign(fsys, level)
            Symbol.sqrtSym -> parseSqrtStatement(fsys, level)
            Symbol.writeSym -> parseWriteStatement(fsys, level, false)
            Symbol.writelnSym -> parseWriteStatement(fsys, level)
            Symbol.printSym -> parsePrintStatement(fsys, level, false)
            Symbol.printlnSym -> parsePrintStatement(fsys, level)
            else -> {
                nxtlev = SymSet(SYMBOL_NUM)
                test(fsys, nxtlev, 19)
            }
        }
    }

    /**
     * 分析<当型循环语句>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </当型循环语句> */
    private fun parseWhileStatement(fsys: SymSet, lev: Int) {
        val cx1 = interpreter.cx
        val nxtlev: SymSet = fsys.clone() as SymSet

        // 保存判断条件操作的位置
        nextSymbol()
        nxtlev.set(Symbol.doSym)// 后跟符号为do
        parseCondition(nxtlev, lev)// 分析<条件>
        val cx2 = interpreter.cx// 保存循环体的结束的下一个位置
        interpreter.generatePCode(Fct.JPC, 0, 0)// 生成条件跳转，但跳出循环的地址未知
        if (currentSymbol == Symbol.doSym)
            nextSymbol()
        else
            Err.report(18)                        // 缺少do
        parseStatement(fsys, lev)                // 分析<语句>
        interpreter.generatePCode(Fct.JMP, 0, cx1)            // 回头重新判断条件
        interpreter.code[cx2]!!.a = interpreter.cx            // 反填跳出循环的地址，与<条件语句>类似
    }

    private fun parseForStatement(fsys: SymSet, lev: Int) {
        nextSymbol()
        val i = parseAssignStatement(fsys, lev)

        val changeSymbols = arrayOf(Symbol.toSym, Symbol.untilSym, Symbol.downtoSym)
        if (currentSymbol in changeSymbols) {
            val changeSymbol = currentSymbol

            val cx1 = interpreter.cx//记录循环开始

            nextSymbol()
            parseExpression(fsys, lev)
            loadVar(lev, table[i]!!)
            when (changeSymbol) {
                Symbol.toSym -> interpreter.generatePCode(Fct.OPR, 0, 11)//比较次栈顶是否大于等于栈顶 1
                Symbol.untilSym -> interpreter.generatePCode(Fct.OPR, 0, 12)//比较次栈顶是否大于栈顶 1
                Symbol.downtoSym -> interpreter.generatePCode(Fct.OPR, 0, 13)//比较次栈顶是否小于等于栈顶 1
                else -> Err.report(42)
            }

            val cx2 = interpreter.cx// 保存循环体的结束的下一个位置
            interpreter.generatePCode(Fct.JPC, 0, 0)// 生成条件跳转，但跳出循环的地址未知

            var isStep = false
            var step: Int = 0
            if(currentSymbol == Symbol.stepSym){
                isStep = true
                nextSymbol()
                if (currentSymbol == Symbol.number) {
                    step = scanner.num
                    nextSymbol()
                } else {
                    Err.report(43)
                }
            }

            if (currentSymbol == Symbol.doSym)
                nextSymbol()
            else
                Err.report(18)// 缺少do
            parseStatement(fsys, lev)// 分析<语句>

            loadVar(lev, table[i]!!)
            if (changeSymbol != Symbol.downtoSym) {
                if (!isStep) {
                    interpreter.generatePCode(Fct.OPR, 0, 17)//+1
                } else {
                    interpreter.generatePCode(Fct.LIT, 0, step)
                    interpreter.generatePCode(Fct.OPR, 0, 2)//相加
                }
            } else {
                if (!isStep) {
                    interpreter.generatePCode(Fct.OPR, 0, 18)//+1
                } else {
                    interpreter.generatePCode(Fct.LIT, 0, step)
                    interpreter.generatePCode(Fct.OPR, 0, 3)//相减
                }
            }
            storeVar(lev, table[i]!!)//循环标记变量递加/递减

            interpreter.generatePCode(Fct.JMP, 0, cx1)// 回头重新判断条件
            interpreter.code[cx2]!!.a = interpreter.cx// 反填跳出循环的地址，与<条件语句>类似
        } else
            Err.report(42)
    }

    /**
     * 分析<复合语句>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </复合语句> */
    private fun parseBeginStatement(fsys: SymSet, lev: Int) {
        val nxtlev: SymSet

        nextSymbol()

        //        isComment();

        nxtlev = fsys.clone() as SymSet
        nxtlev.set(Symbol.semicolon)
        nxtlev.set(Symbol.endSym)
        parseStatement(nxtlev, lev)
        // 循环分析{; <语句>}，直到下一个符号不是语句开始符号或收到end
        while (statementBeginSet[currentSymbol!!] || currentSymbol == Symbol.semicolon) {
            if (currentSymbol == Symbol.semicolon)
                nextSymbol()
            else
                Err.report(10)                    // 缺少分号
            parseStatement(nxtlev, lev)
        }
        if (currentSymbol == Symbol.endSym) {
            nextSymbol()
        } else {
            Err.report(17)                        // 缺少end或分号
        }
    }

    /**
     * 分析<条件语句>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </条件语句> */
    private fun parseIfStatement(fsys: SymSet, lev: Int) {
        val cx1: Int
        val nxtlev: SymSet

        nextSymbol()
        nxtlev = fsys.clone() as SymSet
        nxtlev.set(Symbol.thenSym)                // 后跟符号为then或do ???
        nxtlev.set(Symbol.doSym)
        nxtlev.set(Symbol.elseSym)
        parseCondition(nxtlev, lev)            // 分析<条件>
        if (currentSymbol == Symbol.thenSym)
            nextSymbol()
        else
            Err.report(16)                        // 缺少then
        cx1 = interpreter.cx                        // 保存当前指令地址
        interpreter.generatePCode(Fct.JPC, 0, 0)                // 生成条件跳转指令，跳转地址未知，暂时写0
        parseStatement(fsys, lev)                // 处理then后的语句
        interpreter.code[cx1]!!.a = interpreter.cx            // 经statement处理后，cx为then后语句执行
        // 完的位置，它正是前面未定的跳转地址
        if (currentSymbol == Symbol.semicolon) {
            nextSymbol()
            if (currentSymbol == Symbol.elseSym) {
                nextSymbol()
                val cx2 = interpreter.cx
                interpreter.generatePCode(Fct.JMP, 0, 0)
                parseStatement(fsys, lev)
                interpreter.code[cx2]!!.a = interpreter.cx
                interpreter.code[cx1]!!.a = interpreter.code[cx1]!!.a + 1
            } else {
                parseStatement(fsys, lev)
            }
        }
    }

    /**
     * 分析<过程调用语句>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </过程调用语句> */
    private fun parseCallStatement(fsys: SymSet, lev: Int) {
        val i: Int
        nextSymbol()
        if (currentSymbol == Symbol.ident) {
            i = table.position(scanner.id)
            if (i == 0) {
                Err.report(11)                    // 过程未找到
            } else {
                val item = table[i]
                if (item != null) {
                    if (item.kind == Object.procedure)
                        interpreter.generatePCode(Fct.CAL, lev - item.level, item.adr)
                    else
                        Err.report(15)
                }                // call后标识符应为过程
            }
            nextSymbol()
        } else {
            Err.report(14)                        // call后应为标识符
        }
    }

    /**
     * 分析<写语句>
     *
     * @param fsys  后跟符号集
     * @param level 当前层次
    </写语句> */
    private fun parseWriteStatement(fsys: SymSet, level: Int, isNewLine: Boolean = true) {
        var nxtlev: SymSet

        if (isNewLine) {
            interpreter.generatePCode(Fct.OPR, 0, 15)//生成换行指令
        }

        nextSymbol()
        if (currentSymbol == Symbol.lParen) {
            do {
                nextSymbol()
                nxtlev = fsys.clone() as SymSet//后跟符号集的拷贝 用于传入表达式分析
                nxtlev.set(Symbol.rParen)//添加后跟符号 右括号
                nxtlev.set(Symbol.comma)//添加后跟符号 逗号
                parseExpression(nxtlev, level)
                interpreter.generatePCode(Fct.OPR, 0, 14)
            } while (currentSymbol == Symbol.comma)

            if (currentSymbol == Symbol.rParen) {
                nextSymbol()
            } else {
                Err.report(33)
            }                // write()中应为完整表达式
        }
    }

    /**
     * 分析<读语句>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </读语句> */
    private fun parseReadStatement(fsys: SymSet, lev: Int) {
        var i: Int

        nextSymbol()
        if (currentSymbol == Symbol.lParen) {
            do {
                nextSymbol()
                i = if (currentSymbol == Symbol.ident)
                    table.position(scanner.id)
                else
                    0

                if (i == 0) {
                    Err.report(35)            // read()中应是声明过的变量名
                } else {
                    val item = table[i]
                    if (item!!.kind != Object.variable) {
                        Err.report(32)        // read()中的标识符不是变量, thanks to amd
                    } else {
                        interpreter.generatePCode(Fct.OPR, 0, 16)
                        interpreter.generatePCode(Fct.STO, lev - item.level, item.adr)
                    }
                }

                nextSymbol()
            } while (currentSymbol == Symbol.comma)
        } else {
            Err.report(34)                    // 格式错误，应是左括号
        }

        if (currentSymbol == Symbol.rParen) {
            nextSymbol()

        } else {
            Err.report(33)                    // 格式错误，应是右括号
            while (!fsys[currentSymbol!!])
                nextSymbol()
        }
    }

    /**
     * 分析<赋值语句>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </赋值语句> */
    private fun parseAssignStatement(fsys: SymSet, lev: Int): Int {
        val i = table.position(scanner.id)
        if (i > 0) {
            val item = table[i]
            if (item != null) {
                when {
                    item.kind == Object.variable -> {
                        nextSymbol()
                        assign(fsys, lev, item)
                    }
                    item.kind == Object.array -> {
                        nextSymbol()
                        if (getArrayDiff(fsys, lev, item.name!!)) {
                            assign(fsys, lev, item)
                        } else {
                            val clone = table.copyItem(item)
                            clone.kind = Object.variable
                            assign(fsys, lev, clone)
                        }
                    }
                    else -> Err.report(12)                        // 赋值语句格式错误
                }
            }
        } else {
            Err.report(11)                            // 变量未找到
        }
        return i
    }

    /**
     * @description: 应对各种不同赋值操作
     * @param: [fsys, lev, item]
     * @author: KanModel
     * @create: 2018/11/19 8:44
     */
    private fun assign(fsys: SymSet, lev: Int, item: Table.Item) {
        val nxtlev: SymSet

        when (currentSymbol) {
            Symbol.becomes -> {
                nextSymbol()

                nxtlev = fsys.clone() as SymSet
                parseExpression(nxtlev, lev)
                // parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
                storeVar(lev, item)
            }
            Symbol.plusAssSym -> {
                nextSymbol()

                nxtlev = fsys.clone() as SymSet
                loadVar(lev, item)
                parseExpression(nxtlev, lev)
                // parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
                interpreter.generatePCode(Fct.OPR, 0, 2)
                storeVar(lev, item)
            }
            Symbol.minusAssSym -> {
                nextSymbol()

                nxtlev = fsys.clone() as SymSet
                loadVar(lev, item)
                parseExpression(nxtlev, lev)
                // parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
                interpreter.generatePCode(Fct.OPR, 0, 3)
                storeVar(lev, item)
            }
            Symbol.timesAssSym -> {
                nextSymbol()

                nxtlev = fsys.clone() as SymSet
                loadVar(lev, item)
                parseExpression(nxtlev, lev)
                // parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
                interpreter.generatePCode(Fct.OPR, 0, 4)
                storeVar(lev, item)
            }
            Symbol.slashAssSym -> {
                nextSymbol()

                nxtlev = fsys.clone() as SymSet
                loadVar(lev, item)
                parseExpression(nxtlev, lev)
                // parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
                interpreter.generatePCode(Fct.OPR, 0, 5)
                storeVar(lev, item)
            }
            Symbol.modAssSym -> {
                nextSymbol()

                nxtlev = fsys.clone() as SymSet
                loadVar(lev, item)
                parseExpression(nxtlev, lev)
                // parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
                interpreter.generatePCode(Fct.OPR, 0, 21)
                storeVar(lev, item)
            }
            else -> Err.report(13)                // 没有检测到赋值符号
        }
    }

    /**
     * @description: 加载对应数据到栈顶
     * @param: [lev, item]
     * @return: void
     * @author: KanModel
     * @create: 2018/11/20 13:35
     */
    private fun loadVar(lev: Int, item: Table.Item) {
        if (item.kind == Object.variable) {
            interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr)
        } else {
            interpreter.code[interpreter.cx] = interpreter.code[interpreter.cx - 1]
            interpreter.cx = interpreter.cx + 1
            interpreter.generatePCode(Fct.LAD, lev - item.level, item.adr)
        }
    }

    /**
     * @description: 保存栈顶数据到对应变量
     * @param: [lev, item]
     * @return: void
     * @author: KanModel
     * @create: 2018/11/20 13:34
     */
    private fun storeVar(lev: Int, item: Table.Item) {
        if (item.kind == Object.variable) {
            interpreter.generatePCode(Fct.STO, lev - item.level, item.adr)
        } else {
            interpreter.generatePCode(Fct.STA, lev - item.level, item.adr)
        }
    }

    /**
     * @description: 获取数组偏移量
     * @return: boolean
     * @author: KanModel
     * @create: 2018/11/19 9:11
     */
    private fun getArrayDiff(fsys: SymSet, level: Int, name: String): Boolean {
        val (_, dim, seq) = ArrayStore.get(name) ?: return false
        if (currentSymbol == Symbol.lSquBra) {
            var dimCount = 0
            do {
                if (dimCount > 0) {
                    for (i in dimCount until dim) {
                        interpreter.generatePCode(Fct.LIT, 0, seq[i])
                        interpreter.generatePCode(Fct.OPR, 0, 4)
                    }
                }
                nextSymbol()
                parseExpression(fsys, level)
                if (currentSymbol == Symbol.rSquBra) {
                    nextSymbol()
                } else {
                    Err.report(22)
                }
                dimCount++
            } while (currentSymbol == Symbol.lSquBra)
            while (dimCount-- > 1) {
                interpreter.generatePCode(Fct.OPR, 0, 2)
            }
            return true
        } else {
            return false
        }
    }

    /**
     * 分析<表达式>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </表达式> */
    private fun parseExpression(fsys: SymSet, lev: Int) {
        var addop: Symbol
        var nxtlev: SymSet

        // 分析{[+|-|++|--]<项>}
        if (currentSymbol == Symbol.plusplus || currentSymbol == Symbol.minusminus || currentSymbol == Symbol.plus || currentSymbol == Symbol.minus) {
            addop = currentSymbol!!
            nextSymbol()

            nxtlev = fsys.clone() as SymSet
            nxtlev.set(Symbol.plusplus)
            nxtlev.set(Symbol.minusminus)
            nxtlev.set(Symbol.plus)
            nxtlev.set(Symbol.minus)
            parseTerm(nxtlev, lev)

            if (addop == Symbol.plusplus || addop == Symbol.minusminus) {
                if (addop == Symbol.plusplus) {
                    interpreter.generatePCode(Fct.OPR, 0, 17)
                } else {
                    interpreter.generatePCode(Fct.OPR, 0, 18)
                }

                val i = table.position(scanner.id)
                if (i > 0) {
                    val item = table[i]
                    if (item != null) {
                        if (item.kind == Object.variable) {
                            interpreter.generatePCode(Fct.STO, lev - item.level, item.adr)//保存栈顶到变量值
                            interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr)//取出变量值到栈顶
                        } else {
                            Err.report(12)                        // 赋值语句格式错误
                        }
                    }
                } else {
                    Err.report(11)                            // 变量未找到
                }
            } else if (addop == Symbol.minus) {
                interpreter.generatePCode(Fct.OPR, 0, 1)
            }
        } else {
            nxtlev = fsys.clone() as SymSet
            nxtlev.set(Symbol.plusplus)
            nxtlev.set(Symbol.minusminus)
            nxtlev.set(Symbol.plus)
            nxtlev.set(Symbol.minus)
            nxtlev.set(Symbol.sqrtSym)
            nxtlev.set(Symbol.mod)

            parseTerm(nxtlev, lev)
        }


        // 分析{<加法运算符><项> | <取模><项>}
        while (currentSymbol == Symbol.plus || currentSymbol == Symbol.minus || currentSymbol == Symbol.mod) {
            addop = currentSymbol!!
            nextSymbol()
            nxtlev = fsys.clone() as SymSet
            nxtlev.set(Symbol.plus)
            nxtlev.set(Symbol.minus)
            nxtlev.set(Symbol.mod)
            parseTerm(nxtlev, lev)
            when (addop) {
                Symbol.plus -> interpreter.generatePCode(Fct.OPR, 0, 2)
                Symbol.minus -> interpreter.generatePCode(Fct.OPR, 0, 3)
                else -> interpreter.generatePCode(Fct.OPR, 0, 21)
            }
        }
    }

    /**
     * 分析<项>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </项> */
    private fun parseTerm(fsys: SymSet, lev: Int) {
        var mulop: Symbol
        val nxtlev: SymSet

        // 分析<因子>
        nxtlev = fsys.clone() as SymSet
        nxtlev.set(Symbol.times)
        nxtlev.set(Symbol.slash)
        parseFactor(nxtlev, lev)

        // 分析{<乘法运算符><因子>}
        while (currentSymbol == Symbol.times || currentSymbol == Symbol.slash) {
            mulop = currentSymbol!!
            nextSymbol()
            parseFactor(nxtlev, lev)
            if (mulop == Symbol.times)
                interpreter.generatePCode(Fct.OPR, 0, 4)
            else
                interpreter.generatePCode(Fct.OPR, 0, 5)
        }
    }

    /**
     * 分析<因子>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </因子> */
    private fun parseFactor(fsys: SymSet, lev: Int) {
        val nxtlev: SymSet

        test(factorBeginSet, fsys, 24)            // 检测因子的开始符号
        // the original while... is problematic: var1(var2+var3)
        // thanks to macross
        // while(inset(currentSymbol, factorBeginSet))
        if (factorBeginSet[currentSymbol!!]) {
            if (currentSymbol == Symbol.ident) {            // 因子为常量或变量
                val i = table.position(scanner.id)
                if (i > 0) {
                    val item = table[i]
                    if (item != null) {
                        when (item.kind) {
                            Object.constant            // 名字为常量
                            -> {
                                interpreter.generatePCode(Fct.LIT, 0, item.`val`)
                                nextSymbol()
                            }
                            Object.variable            // 名字为变量
                            -> {
                                interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr)
                                nextSymbol()
                            }
                            Object.array            // 名字为数组
                            -> {
                                nextSymbol()
                                if (getArrayDiff(fsys, lev, item.name!!)) {
                                    interpreter.generatePCode(Fct.LAD, lev - item.level, item.adr)
                                } else {
                                    interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr)
                                }
                            }
                            Object.procedure            // 名字为过程
                            -> {
                                Err.report(21)                // 不能为过程
                                nextSymbol()
                            }
                        }
                    }
                } else {
                    Err.report(11)                    // 标识符未声明
                    nextSymbol()
                }
            } else if (currentSymbol == Symbol.number) {    // 因子为数
                var num = scanner.num
                if (num > PL0.MAX_NUM) {
                    Err.report(31)//超过数值范围
                    num = 0
                }
                interpreter.generatePCode(Fct.LIT, 0, num)
                nextSymbol()
            } else if (currentSymbol == Symbol.sqrtSym) {   //因子为sqrt运算
                parseSqrtStatement(fsys, lev)
            } else if (currentSymbol == Symbol.lParen) {    // 因子为表达式
                nextSymbol()
                nxtlev = fsys.clone() as SymSet
                nxtlev.set(Symbol.rParen)
                parseExpression(nxtlev, lev)
                if (currentSymbol == Symbol.rParen)
                    nextSymbol()
                else
                    Err.report(22)                    // 缺少右括号
            } else {
                // 做补救措施
                test(fsys, factorBeginSet, 23)
            }
        }
    }

    /**
     * 分析<条件>
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
    </条件> */
    private fun parseCondition(fsys: SymSet, lev: Int) {
        val relop: Symbol
        val nxtlev: SymSet

        if (currentSymbol == Symbol.oddSym) {
            // 分析 ODD<表达式>
            nextSymbol()
            parseExpression(fsys, lev)
            interpreter.generatePCode(Fct.OPR, 0, 6)
        } else if (currentSymbol == Symbol.not) {
            nextSymbol()
            if (currentSymbol == Symbol.lParen) {
                nextSymbol()
                parseCondition(fsys, lev)
                if (currentSymbol == Symbol.rParen) {
                    nextSymbol()
                } else {
                    Err.report(22)
                }
            } else
                parseExpression(fsys, lev)
            interpreter.generatePCode(Fct.OPR, 0, 22)
        } else {
            // 分析<表达式><关系运算符><表达式>
            nxtlev = fsys.clone() as SymSet
            nxtlev.set(Symbol.equal)
            nxtlev.set(Symbol.neq)
            nxtlev.set(Symbol.lss)
            nxtlev.set(Symbol.leq)
            nxtlev.set(Symbol.gtr)
            nxtlev.set(Symbol.geq)
            parseExpression(nxtlev, lev)
            if (currentSymbol == Symbol.equal || currentSymbol == Symbol.neq
                    || currentSymbol == Symbol.lss || currentSymbol == Symbol.leq
                    || currentSymbol == Symbol.gtr || currentSymbol == Symbol.geq) {
                relop = currentSymbol!!
                nextSymbol()
                parseExpression(fsys, lev)
                when (relop) {
                    Symbol.equal -> interpreter.generatePCode(Fct.OPR, 0, 8)
                    Symbol.neq -> interpreter.generatePCode(Fct.OPR, 0, 9)
                    Symbol.lss -> interpreter.generatePCode(Fct.OPR, 0, 10)
                    Symbol.geq -> interpreter.generatePCode(Fct.OPR, 0, 11)
                    Symbol.gtr -> interpreter.generatePCode(Fct.OPR, 0, 12)
                    Symbol.leq -> interpreter.generatePCode(Fct.OPR, 0, 13)
                }
            } else {
                Err.report(20)
            }
        }
    }

    /**
     * 跳过注释
     *
     * @author: KanModel
     */
    private fun isComment() {
        while (currentSymbol == Symbol.comment) {
            nextSymbol()
        }
    }

    /**
     * 分析<++-->
     *
     * @param fsys 后跟符号集
     * @param lev  当前层次
     */
    private fun parsePlusMinusAssign(fsys: SymSet, lev: Int) {
        val addop: Symbol
        val nxtlev: SymSet

        // 分析{[++|--]<项>}
        if (currentSymbol == Symbol.plusplus || currentSymbol == Symbol.minusminus) {
            addop = currentSymbol!!
            nextSymbol()

            nxtlev = fsys.clone() as SymSet
            nxtlev.set(Symbol.plusplus)
            nxtlev.set(Symbol.minusminus)
            parseTerm(nxtlev, lev)//分析因子

            if (addop == Symbol.plusplus) {
                interpreter.generatePCode(Fct.OPR, 0, 17)
            } else {
                interpreter.generatePCode(Fct.OPR, 0, 18)
            }

            val i = table.position(scanner.id)
            if (i > 0) {
                val item = table[i]
                if (item != null) {
                    if (item.kind == Object.variable) {
                        interpreter.generatePCode(Fct.STO, lev - item.level, item.adr)//保存栈顶到变量值
                    } else {
                        Err.report(12)                        // 赋值语句格式错误
                    }
                }
            } else {
                Err.report(11)                            // 变量未找到
            }
        }
    }

    /**
     * 分析<开方语句>
     *
     * @param fsys  后跟符号集
     * @param level 当前层次
    </开方语句> */
    private fun parseSqrtStatement(fsys: SymSet, level: Int) {
        val nxtlev: SymSet

        nextSymbol()
        if (currentSymbol == Symbol.lParen) {
            nextSymbol()
            nxtlev = fsys.clone() as SymSet//后跟符号集的拷贝 用于传入表达式分析
            nxtlev.set(Symbol.rParen)//添加后跟符号 右括号
            parseExpression(nxtlev, level)
            interpreter.generatePCode(Fct.OPR, 0, 19)

            if (currentSymbol == Symbol.rParen) {
                nextSymbol()
            } else {
                Err.report(33)
            }
        }
    }

    /**
     * 分析<数组说明部分>
     *
     * @param level 当前层次
    </数组说明部分> */
    private fun parseArrayDeclaration(level: Int) {
        var arraySize = 1

        if (currentSymbol == Symbol.ident) {
            // 填写名字表并改变堆栈帧计数器
            nextSymbol()
            do {
                if (currentSymbol == Symbol.lSquBra) {
                    nextSymbol()
                    if (currentSymbol == Symbol.number) {
                        ArrayStore.add(scanner.id, scanner.num)
                        arraySize *= scanner.num
                        nextSymbol()
                    }
                }
                if (currentSymbol == Symbol.rSquBra) {
                    nextSymbol()
                } else {
                    Err.report(22)
                }
            } while (currentSymbol == Symbol.lSquBra)
            //为数组开辟空间
            for (i in 0 until arraySize) {
                table.enter(Object.array, level, dataSize)
                dataSize++
            }
        } else {
            Err.report(4)                    // var 后应是标识
        }
    }

    /**
     * 分析<打印语句>
     *
     * @param fsys  后跟符号集
     * @param level 当前层次
    </打印语句> */
    private fun parsePrintStatement(fsys: SymSet, level: Int, isNewLine: Boolean = true) {
        var nxtlev: SymSet

        if (isNewLine) {
            interpreter.generatePCode(Fct.OPR, 0, 15)
        }
        nextSymbol()
        if (currentSymbol == Symbol.lParen) {
            do {
                nextSymbol()
                if (currentSymbol == Symbol.rParen) break
                nxtlev = fsys.clone() as SymSet//后跟符号集的拷贝 用于传入表达式分析
                nxtlev.set(Symbol.rParen)//添加后跟符号 右括号
                nxtlev.set(Symbol.comma)//添加后跟符号 逗号
                if (currentSymbol == Symbol.string) {
                    for (i in scanner.strList.indices) {
                        interpreter.generatePCode(Fct.LIT, 0, scanner.strList[i])
                        interpreter.generatePCode(Fct.OPR, 0, 20)
                    }
                    nextSymbol()
                } else {
                    parseExpression(nxtlev, level)
                    interpreter.generatePCode(Fct.OPR, 0, 20)
                }
            } while (currentSymbol == Symbol.comma)

            if (currentSymbol == Symbol.rParen) {
                nextSymbol()
            } else {
                Err.report(33)
            }                // write()中应为完整表达式
        }
    }
}

package compiler;

import compiler.error.Err;

/**
 * �����﷨������������PL/0������������Ҫ�Ĳ��֣����﷨�����Ĺ����д������﷨�������Ŀ��������ɡ�
 */
public class Parser {
    private Scanner scanner;                    // �Դʷ�������������
    private Table identTable;                    // �Է��ű������
    private Interpreter interpreter;                // ��Ŀ�����������������

    private final int SYMBOL_NUM = Symbol.values().length;

    // ��ʾ������ʼ�ķ��ż��ϡ���ʾ��俪ʼ�ķ��ż��ϡ���ʾ���ӿ�ʼ�ķ��ż���
    // ʵ����������������������ӵ�FIRST����
    private SymSet declarationBeginSet, statementBeginSet, factorBeginSet;

    /**
     * ��ǰ���ţ���nextSymbol()����
     *
     * @see #nextSymbol()
     */
    private Symbol currentSymbol;

    /**
     * ��ǰ������Ķ�ջ֡��С������˵���ݴ�С��data size��
     */
    private int dataSize = 0;

    private int arrayDiff = 0;

    /**
     * ���첢��ʼ���﷨�����������������C���԰汾��init()������һ���ִ���
     *
     * @param l �������Ĵʷ�������
     * @param t �������ķ��ű�
     * @param i ��������Ŀ�����������
     */
    public Parser(Scanner l, Table t, Interpreter i) {
        scanner = l;
        identTable = t;
        interpreter = i;

        // ��������First��
        declarationBeginSet = new SymSet(SYMBOL_NUM);
        declarationBeginSet.set(Symbol.constSym);
        declarationBeginSet.set(Symbol.varSym);
        declarationBeginSet.set(Symbol.procSym);

        // �������First��
        statementBeginSet = new SymSet(SYMBOL_NUM);
        statementBeginSet.set(Symbol.beginSym);
        statementBeginSet.set(Symbol.callSym);
        statementBeginSet.set(Symbol.ifSym);
        statementBeginSet.set(Symbol.whileSym);
        statementBeginSet.set(Symbol.readSym);            // thanks to elu
        statementBeginSet.set(Symbol.writeSym);
        statementBeginSet.set(Symbol.plusplus);
        statementBeginSet.set(Symbol.minusminus);

        // ��������First��
        factorBeginSet = new SymSet(SYMBOL_NUM);
        factorBeginSet.set(Symbol.ident);
        factorBeginSet.set(Symbol.number);
        factorBeginSet.set(Symbol.lParen);
        factorBeginSet.set(Symbol.sqrtSym);

    }

    /**
     * �����﷨�������̣���ǰ�����ȵ���һ��nextSymbol()
     *
     * @see #nextSymbol()
     */
    public void parse() {
//        SymSet nxtlev = new SymSet(SYMBOL_NUM);
        SymSet nextLevel = new SymSet(SYMBOL_NUM);
        nextLevel.or(declarationBeginSet);//������
        nextLevel.or(statementBeginSet);
        nextLevel.set(Symbol.period);
        parseBlock(0, nextLevel);

        if (currentSymbol != Symbol.period)
            Err.report(9);
    }

    /**
     * �����һ���﷨���ţ�����ֻ�Ǽ򵥵���һ��getSymbol()
     */
    public void nextSymbol() {
        scanner.getSymbol();
        currentSymbol = scanner.currentSymbol;
        isComment();
    }

    /**
     * ���Ե�ǰ�����Ƿ�Ϸ�
     *
     * @param s1      ������Ҫ�ķ���
     * @param s2      �������������Ҫ�ģ�����Ҫһ�������õļ���
     * @param errCode �����
     */
    void test(SymSet s1, SymSet s2, int errCode) {
        // ��ĳһ���֣���һ����䣬һ�����ʽ����Ҫ����ʱʱ����ϣ����һ����������ĳ����
        //���ò��ֵĺ�����ţ���test���������⣬���Ҹ��𵱼�ⲻͨ��ʱ�Ĳ��ȴ�ʩ����
        // ������Ҫ���ʱָ����ǰ��Ҫ�ķ��ż��ϺͲ����õļ��ϣ���֮ǰδ��ɲ��ֵĺ����
        // �ţ����Լ���ⲻͨ��ʱ�Ĵ���š�
        if (!s1.get(currentSymbol)) {
            Err.report(errCode);
            // ����ⲻͨ��ʱ����ͣ��ȡ���ţ�ֱ����������Ҫ�ļ��ϻ򲹾ȵļ���
            while (!s1.get(currentSymbol) && !s2.get(currentSymbol))
                nextSymbol();
        }
    }

    /**
     * ����<�ֳ���>
     *
     * @param level ��ǰ�ֳ������ڲ�
     * @param fsys  ��ǰģ�������ż�
     */
    public void parseBlock(int level, SymSet fsys) {
        // <�ֳ���> := [<����˵������>][<����˵������>][<����˵������>]<���>

        int dx0, tx0, cx0;                // ������ʼdx��tx��cx
        SymSet nextLevel = new SymSet(SYMBOL_NUM);

        dx0 = dataSize;                        // ��¼����֮ǰ�����������Ա�ָ���
        dataSize = 3;
        tx0 = identTable.tableSize;                    // ��¼�������ֵĳ�ʼλ�ã��Ա�ָ���
        identTable.get(identTable.tableSize).adr = interpreter.cx;

        interpreter.generatePCode(Fct.JMP, 0, 0);

        if (level > PL0.LEVEL_MAX)
            Err.report(32);

        // ����<˵������>
        do {
            // <����˵������>
            if (currentSymbol == Symbol.constSym) {
                nextSymbol();
                // the original do...while(currentSymbol == ident) is problematic, thanks to calculous
                // do
                parseConstDeclaration(level);
                while (currentSymbol == Symbol.comma) {
                    nextSymbol();
                    parseConstDeclaration(level);
                }

                if (currentSymbol == Symbol.semicolon)
                    nextSymbol();
                else
                    Err.report(5);                // ©���˶��Ż��߷ֺ�
                // } while (currentSymbol == ident);
            }

            // <����˵������>
            if (currentSymbol == Symbol.varSym) {
                nextSymbol();
                // the original do...while(currentSymbol == ident) is problematic, thanks to calculous
                // do {
                parseVarDeclaration(level);
                while (currentSymbol == Symbol.comma) {
                    nextSymbol();
                    parseVarDeclaration(level);
                }

                if (currentSymbol == Symbol.semicolon)
                    nextSymbol();
                else
                    Err.report(5);                // ©���˶��Ż��߷ֺ�
                // } while (currentSymbol == ident);

            }

            // <����˵������>
            if (currentSymbol == Symbol.arraySym) {
                nextSymbol();
                parseArrayDeclaration(level);
                while (currentSymbol == Symbol.comma) {
                    nextSymbol();
                    parseArrayDeclaration(level);
                }

                if (currentSymbol == Symbol.semicolon)
                    nextSymbol();
                else
                    Err.report(5);                // ©���˶��Ż��߷ֺ�
            }

            // <����˵������>
            while (currentSymbol == Symbol.procSym) {
                nextSymbol();
                if (currentSymbol == Symbol.ident) {
                    identTable.enter(Objekt.procedure, level, dataSize);
                    nextSymbol();
                } else {
                    Err.report(4);                // procedure��ӦΪ��ʶ��
                }

                if (currentSymbol == Symbol.semicolon) {
                    nextSymbol();
                } else {
                    Err.report(5);                // ©���˷ֺ�
                }
                nextLevel = (SymSet) fsys.clone();
                nextLevel.set(Symbol.semicolon);
                parseBlock(level + 1, nextLevel);

                if (currentSymbol == Symbol.semicolon) {
                    nextSymbol();
                    nextLevel = (SymSet) statementBeginSet.clone();
                    nextLevel.set(Symbol.ident);
                    nextLevel.set(Symbol.procSym);
                    test(nextLevel, fsys, 6);
                } else {
                    Err.report(5);                // ©���˷ֺ�
                }
            }


            nextLevel = (SymSet) statementBeginSet.clone();
            nextLevel.set(Symbol.ident);
            test(nextLevel, declarationBeginSet, 7);
        } while (declarationBeginSet.get(currentSymbol));        // ֱ��û����������

        // ��ʼ���ɵ�ǰ���̴���
        Table.Item item = identTable.get(tx0);
        interpreter.code[item.adr].a = interpreter.cx;
        item.adr = interpreter.cx;                    // ��ǰ���̴����ַ
        item.size = dataSize;                            // ����������ÿ����һ�����������dx����1��
        // ���������Ѿ�������dx���ǵ�ǰ���̵Ķ�ջ֡��С
        cx0 = interpreter.cx;
        interpreter.generatePCode(Fct.INT, 0, dataSize);            // ���ɷ����ڴ����

        identTable.debugTable(tx0);

        // ����<���>
        nextLevel = (SymSet) fsys.clone();        // ÿ��������ż��Ͷ������ϲ������ż��ͣ��Ա㲹��
        nextLevel.set(Symbol.semicolon);        // ���������Ϊ�ֺŻ�end
        nextLevel.set(Symbol.endSym);
        parseStatement(nextLevel, level);
        interpreter.generatePCode(Fct.OPR, 0, 0);        // ÿ�����̳��ڶ�Ҫʹ�õ��ͷ����ݶ�ָ��

        nextLevel = new SymSet(SYMBOL_NUM);    // �ֳ���û�в��ȼ���
        test(fsys, nextLevel, 8);                // �����������ȷ��

        interpreter.listCode(cx0);

        dataSize = dx0;                            // �ָ���ջ֡������
        identTable.tableSize = tx0;                        // �ظ����ֱ�λ��
    }

    /**
     * ����<����˵������>
     *
     * @param level ��ǰ���ڵĲ��
     */
    void parseConstDeclaration(int level) {
        if (currentSymbol == Symbol.ident) {
            nextSymbol();
            if (currentSymbol == Symbol.equal || currentSymbol == Symbol.becomes) {
                if (currentSymbol == Symbol.becomes)
                    Err.report(1);            // �� = д���� :=
                nextSymbol();
                if (currentSymbol == Symbol.number) {
                    identTable.enter(Objekt.constant, level, dataSize);
                    nextSymbol();
                } else {
                    Err.report(2);            // ����˵�� = ��Ӧ������
                }
            } else {
                Err.report(3);                // ����˵����ʶ��Ӧ�� =
            }
        } else {
            Err.report(4);                    // const ��Ӧ�Ǳ�ʶ��
        }
    }

    /**
     * ����<����˵������>
     *
     * @param level ��ǰ���
     */
    void parseVarDeclaration(int level) {
        if (currentSymbol == Symbol.ident) {
            // ��д���ֱ��ı��ջ֡������
            identTable.enter(Objekt.variable, level, dataSize);
            dataSize++;
            nextSymbol();
        } else {
            Err.report(4);                    // var ��Ӧ�Ǳ�ʶ
        }
    }

    /**
     * ����<���>
     *
     * @param fsys  ������ż�
     * @param level ��ǰ���
     */
    void parseStatement(SymSet fsys, int level) {
        SymSet nxtlev;
        // Wirth �� PL/0 ������ʹ��һϵ�е�if...else...������
        // �������������Ϊ�����д���ܹ���������ؿ�����������Ĵ����߼�
        switch (currentSymbol) {
            case ident:
                parseAssignStatement(fsys, level);
                break;
            case readSym:
                parseReadStatement(fsys, level);
                break;
            case writeSym:
                parseWriteStatement(fsys, level);
                break;
            case callSym:
                parseCallStatement(fsys, level);
                break;
            case ifSym:
                parseIfStatement(fsys, level);
                break;
            case beginSym:
                parseBeginStatement(fsys, level);
                break;
            case whileSym:
                parseWhileStatement(fsys, level);
                break;
            case plusplus:
                parsePlusMinusAssign(fsys, level);
                break;
            case minusminus:
                parsePlusMinusAssign(fsys, level);
                break;
            case sqrtSym:
                parseSqrtStatement(fsys, level);
                break;
            default:
                nxtlev = new SymSet(SYMBOL_NUM);
                test(fsys, nxtlev, 19);
                break;
        }
    }

    /**
     * ����<����ѭ�����>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseWhileStatement(SymSet fsys, int lev) {
        int cx1, cx2;
        SymSet nxtlev;

        cx1 = interpreter.cx;                        // �����ж�����������λ��
        nextSymbol();
        nxtlev = (SymSet) fsys.clone();
        nxtlev.set(Symbol.doSym);                // �������Ϊdo
        parseCondition(nxtlev, lev);            // ����<����>
        cx2 = interpreter.cx;                        // ����ѭ����Ľ�������һ��λ��
        interpreter.generatePCode(Fct.JPC, 0, 0);                // ����������ת��������ѭ���ĵ�ַδ֪
        if (currentSymbol == Symbol.doSym)
            nextSymbol();
        else
            Err.report(18);                        // ȱ��do
        parseStatement(fsys, lev);                // ����<���>
        interpreter.generatePCode(Fct.JMP, 0, cx1);            // ��ͷ�����ж�����
        interpreter.code[cx2].a = interpreter.cx;            // ��������ѭ���ĵ�ַ����<�������>����
    }

    /**
     * ����<�������>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseBeginStatement(SymSet fsys, int lev) {
        SymSet nxtlev;

        nextSymbol();

//        isComment();

        nxtlev = (SymSet) fsys.clone();
        nxtlev.set(Symbol.semicolon);
        nxtlev.set(Symbol.endSym);
        parseStatement(nxtlev, lev);
        // ѭ������{; <���>}��ֱ����һ�����Ų�����俪ʼ���Ż��յ�end
        while (statementBeginSet.get(currentSymbol) || currentSymbol == Symbol.semicolon) {
            if (currentSymbol == Symbol.semicolon)
                nextSymbol();
            else
                Err.report(10);                    // ȱ�ٷֺ�
            parseStatement(nxtlev, lev);
        }
        if (currentSymbol == Symbol.endSym) {
            nextSymbol();

//            isComment();

        } else {
            Err.report(17);                        // ȱ��end��ֺ�
        }
    }

    /**
     * ����<�������>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseIfStatement(SymSet fsys, int lev) {
        int cx1;
        SymSet nxtlev;

        nextSymbol();
        nxtlev = (SymSet) fsys.clone();
        nxtlev.set(Symbol.thenSym);                // �������Ϊthen��do ???
        nxtlev.set(Symbol.doSym);
        parseCondition(nxtlev, lev);            // ����<����>
        if (currentSymbol == Symbol.thenSym)
            nextSymbol();
        else
            Err.report(16);                        // ȱ��then
        cx1 = interpreter.cx;                        // ���浱ǰָ���ַ
        interpreter.generatePCode(Fct.JPC, 0, 0);                // ����������תָ���ת��ַδ֪����ʱд0
        parseStatement(fsys, lev);                // ����then������
        interpreter.code[cx1].a = interpreter.cx;            // ��statement�����cxΪthen�����ִ��
        // ���λ�ã�������ǰ��δ������ת��ַ
    }

    /**
     * ����<���̵������>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseCallStatement(SymSet fsys, int lev) {
        int i;
        nextSymbol();
        if (currentSymbol == Symbol.ident) {
            i = identTable.position(scanner.id);
            if (i == 0) {
                Err.report(11);                    // ����δ�ҵ�
            } else {
                Table.Item item = identTable.get(i);
                if (item.kind == Objekt.procedure)
                    interpreter.generatePCode(Fct.CAL, lev - item.level, item.adr);
                else
                    Err.report(15);                // call���ʶ��ӦΪ����
            }
            nextSymbol();
        } else {
            Err.report(14);                        // call��ӦΪ��ʶ��
        }
    }

    /**
     * ����<д���>
     *
     * @param fsys  ������ż�
     * @param level ��ǰ���
     */
    private void parseWriteStatement(SymSet fsys, int level) {
        SymSet nxtlev;

        nextSymbol();
        if (currentSymbol == Symbol.lParen) {
            do {
                nextSymbol();
                nxtlev = (SymSet) fsys.clone();//������ż��Ŀ��� ���ڴ�����ʽ����
                nxtlev.set(Symbol.rParen);//��Ӻ������ ������
                nxtlev.set(Symbol.comma);//��Ӻ������ ����
                parseExpression(nxtlev, level);
                interpreter.generatePCode(Fct.OPR, 0, 14);
            } while (currentSymbol == Symbol.comma);

            if (currentSymbol == Symbol.rParen) {
                nextSymbol();
            } else {
                Err.report(33);
            }                // write()��ӦΪ�������ʽ
        }
        interpreter.generatePCode(Fct.OPR, 0, 15);
    }

    /**
     * ����<�����>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseReadStatement(SymSet fsys, int lev) {
        int i;

        nextSymbol();
        if (currentSymbol == Symbol.lParen) {
            do {
                nextSymbol();
                if (currentSymbol == Symbol.ident)
                    i = identTable.position(scanner.id);
                else
                    i = 0;

                if (i == 0) {
                    Err.report(35);            // read()��Ӧ���������ı�����
                } else {
                    Table.Item item = identTable.get(i);
                    if (item.kind != Objekt.variable) {
                        Err.report(32);        // read()�еı�ʶ�����Ǳ���, thanks to amd
                    } else {
                        interpreter.generatePCode(Fct.OPR, 0, 16);
                        interpreter.generatePCode(Fct.STO, lev - item.level, item.adr);
                    }
                }

                nextSymbol();
            } while (currentSymbol == Symbol.comma);
        } else {
            Err.report(34);                    // ��ʽ����Ӧ��������
        }

        if (currentSymbol == Symbol.rParen) {
            nextSymbol();

        } else {
            Err.report(33);                    // ��ʽ����Ӧ��������
            while (!fsys.get(currentSymbol))
                nextSymbol();
        }
    }

    /**
     * ����<��ֵ���>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseAssignStatement(SymSet fsys, int lev) {
        int i = identTable.position(scanner.id);
        if (i > 0) {
            Table.Item item = identTable.get(i);
            if (item.kind == Objekt.variable) {
                nextSymbol();
                assign(fsys, lev, item);
            } else if (item.kind == Objekt.array) {
                nextSymbol();
                if (getArrayDiff(fsys, lev)) {
                    assign(fsys, lev, item);
                } else {
                    Table.Item clone = identTable.copyItem(item);
                    clone.kind = Objekt.variable;
                    assign(fsys, lev, clone);
                }
            } else {
                Err.report(12);                        // ��ֵ����ʽ����
            }
        } else {
            Err.report(11);                            // ����δ�ҵ�
        }
    }

    /**
     * @description: Ӧ�Ը��ֲ�ͬ��ֵ����
     * @param: [fsys, lev, item]
     * @author: KanModel
     * @create: 2018/11/19 8:44
     */
    void assign(SymSet fsys, int lev, Table.Item item) {
        SymSet nxtlev;

        if (currentSymbol == Symbol.becomes) {
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            parseExpression(nxtlev, lev);
            // parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
            storeVar(lev, item);
        } else if (currentSymbol == Symbol.plusAssSym) {
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            loadVar(lev, item);
            parseExpression(nxtlev, lev);
            // parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
            interpreter.generatePCode(Fct.OPR, 0, 2);
            storeVar(lev, item);
        } else if (currentSymbol == Symbol.minusAssSym) {
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            loadVar(lev, item);
            parseExpression(nxtlev, lev);
            // parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
            interpreter.generatePCode(Fct.OPR, 0, 3);
            storeVar(lev, item);
        } else if (currentSymbol == Symbol.timesAssSym) {
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            loadVar(lev, item);
            parseExpression(nxtlev, lev);
            // parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
            interpreter.generatePCode(Fct.OPR, 0, 4);
            storeVar(lev, item);
        } else if (currentSymbol == Symbol.slashAssSym) {
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            loadVar(lev, item);
            parseExpression(nxtlev, lev);
            // parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
            interpreter.generatePCode(Fct.OPR, 0, 5);
            storeVar(lev, item);
        } else {
            Err.report(13);                // û�м�⵽��ֵ����
        }
    }

    /**
     * @description: ���ض�Ӧ���ݵ�ջ��
     * @param: [lev, item]
     * @return: void
     * @author: KanModel
     * @create: 2018/11/20 13:35
     */
    private void loadVar(int lev, Table.Item item) {
        if (item.kind == Objekt.variable) {
            interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr);
        } else {
            interpreter.code[interpreter.cx] = interpreter.code[interpreter.cx - 1];
            interpreter.cx++;
            interpreter.generatePCode(Fct.LAD, lev - item.level, item.adr);
        }
    }

    /**
     * @description: ����ջ�����ݵ���Ӧ����
     * @param: [lev, item]
     * @return: void
     * @author: KanModel
     * @create: 2018/11/20 13:34
     */
    private void storeVar(int lev, Table.Item item) {
        if (item.kind == Objekt.variable) {
            interpreter.generatePCode(Fct.STO, lev - item.level, item.adr);
        } else {
            interpreter.generatePCode(Fct.STA, lev - item.level, item.adr);
        }
    }

    /**
     * @description: ��ȡ����ƫ����
     * @return: boolean
     * @author: KanModel
     * @create: 2018/11/19 9:11
     */
    boolean getArrayDiff(SymSet fsys, int level) {
        if (currentSymbol == Symbol.lSquBra) {
            nextSymbol();
            parseExpression(fsys, level);
//            if (currentSymbol == Symbol.number) {
//                nextSymbol();
//            } else {
//                Err.report(40);
//            }

            if (currentSymbol == Symbol.rSquBra) {
                nextSymbol();
            } else {
                Err.report(22);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * ����<���ʽ>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseExpression(SymSet fsys, int lev) {
        Symbol addop;
        SymSet nxtlev;

        // ����{[+|-|++|--]<��>}
        if (currentSymbol == Symbol.plusplus || currentSymbol == Symbol.minusminus || currentSymbol == Symbol.plus || currentSymbol == Symbol.minus) {
            addop = currentSymbol;
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.plusplus);
            nxtlev.set(Symbol.minusminus);
            nxtlev.set(Symbol.plus);
            nxtlev.set(Symbol.minus);
            parseTerm(nxtlev, lev);

            if (addop == Symbol.plusplus || addop == Symbol.minusminus) {
                if (addop == Symbol.plusplus) {
                    interpreter.generatePCode(Fct.OPR, 0, 17);
                } else {
                    interpreter.generatePCode(Fct.OPR, 0, 18);
                }

                int i = identTable.position(scanner.id);
                if (i > 0) {
                    Table.Item item = identTable.get(i);
                    if (item.kind == Objekt.variable) {
                        interpreter.generatePCode(Fct.STO, lev - item.level, item.adr);//����ջ��������ֵ
                        interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr);//ȡ������ֵ��ջ��
                    } else {
                        Err.report(12);                        // ��ֵ����ʽ����
                    }
                } else {
                    Err.report(11);                            // ����δ�ҵ�
                }
            } else if (addop == Symbol.minus) {
                interpreter.generatePCode(Fct.OPR, 0, 1);
            }
        } else {
            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.plusplus);
            nxtlev.set(Symbol.minusminus);
            nxtlev.set(Symbol.plus);
            nxtlev.set(Symbol.minus);
            nxtlev.set(Symbol.sqrtSym);

            parseTerm(nxtlev, lev);
//            if (currentSymbol == Symbol.sqrtSym) {
//                parseSqrtStatement(fsys, lev);
//            } else {
//                parseTerm(nxtlev, lev);
//            }
        }


        // ����{<�ӷ������><��>}
        while (currentSymbol == Symbol.plus || currentSymbol == Symbol.minus) {
            addop = currentSymbol;
            nextSymbol();
            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.plus);
            nxtlev.set(Symbol.minus);
            parseTerm(nxtlev, lev);
            if (addop == Symbol.plus)
                interpreter.generatePCode(Fct.OPR, 0, 2);
            else
                interpreter.generatePCode(Fct.OPR, 0, 3);
        }
    }

    /**
     * ����<��>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseTerm(SymSet fsys, int lev) {
        Symbol mulop;
        SymSet nxtlev;

        // ����<����>
        nxtlev = (SymSet) fsys.clone();
        nxtlev.set(Symbol.times);
        nxtlev.set(Symbol.slash);
        parseFactor(nxtlev, lev);

        // ����{<�˷������><����>}
        while (currentSymbol == Symbol.times || currentSymbol == Symbol.slash) {
            mulop = currentSymbol;
            nextSymbol();
            parseFactor(nxtlev, lev);
            if (mulop == Symbol.times)
                interpreter.generatePCode(Fct.OPR, 0, 4);
            else
                interpreter.generatePCode(Fct.OPR, 0, 5);
        }
    }

    /**
     * ����<����>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseFactor(SymSet fsys, int lev) {
        SymSet nxtlev;

        test(factorBeginSet, fsys, 24);            // ������ӵĿ�ʼ����
        // the original while... is problematic: var1(var2+var3)
        // thanks to macross
        // while(inset(currentSymbol, factorBeginSet))
        if (factorBeginSet.get(currentSymbol)) {
            if (currentSymbol == Symbol.ident) {            // ����Ϊ���������
                int i = identTable.position(scanner.id);
                if (i > 0) {
                    Table.Item item = identTable.get(i);
                    switch (item.kind) {
                        case constant:            // ����Ϊ����
                            interpreter.generatePCode(Fct.LIT, 0, item.val);
                            nextSymbol();
                            break;
                        case variable:            // ����Ϊ����
                            interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr);
                            nextSymbol();
                            break;
                        case array:            // ����Ϊ����
                            nextSymbol();
                            if (getArrayDiff(fsys, lev)) {
                                interpreter.generatePCode(Fct.LAD, lev - item.level, item.adr);
                            } else {
                                interpreter.generatePCode(Fct.LOD, lev - item.level, item.adr);
                            }
                            break;
                        case procedure:            // ����Ϊ����
                            Err.report(21);                // ����Ϊ����
                            nextSymbol();
                            break;
                    }
                } else {
                    Err.report(11);                    // ��ʶ��δ����
                    nextSymbol();
                }
            } else if (currentSymbol == Symbol.number) {    // ����Ϊ��
                int num = scanner.num;
                if (num > PL0.MAX_NUM) {
                    Err.report(31);//������ֵ��Χ
                    num = 0;
                }
                interpreter.generatePCode(Fct.LIT, 0, num);
                nextSymbol();
            } else if (currentSymbol == Symbol.sqrtSym) {
                parseSqrtStatement(fsys, lev);
            } else if (currentSymbol == Symbol.lParen) {    // ����Ϊ���ʽ
                nextSymbol();
                nxtlev = (SymSet) fsys.clone();
                nxtlev.set(Symbol.rParen);
                parseExpression(nxtlev, lev);
                if (currentSymbol == Symbol.rParen)
                    nextSymbol();
                else
                    Err.report(22);                    // ȱ��������
            } else {
                // �����ȴ�ʩ
                test(fsys, factorBeginSet, 23);
            }
        }
    }

    /**
     * ����<����>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseCondition(SymSet fsys, int lev) {
        Symbol relop;
        SymSet nxtlev;

        if (currentSymbol == Symbol.oddSym) {
            // ���� ODD<���ʽ>
            nextSymbol();
            parseExpression(fsys, lev);
            interpreter.generatePCode(Fct.OPR, 0, 6);
        } else {
            // ����<���ʽ><��ϵ�����><���ʽ>
            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.equal);
            nxtlev.set(Symbol.neq);
            nxtlev.set(Symbol.lss);
            nxtlev.set(Symbol.leq);
            nxtlev.set(Symbol.gtr);
            nxtlev.set(Symbol.geq);
            parseExpression(nxtlev, lev);
            if (currentSymbol == Symbol.equal || currentSymbol == Symbol.neq
                    || currentSymbol == Symbol.lss || currentSymbol == Symbol.leq
                    || currentSymbol == Symbol.gtr || currentSymbol == Symbol.geq) {
                relop = currentSymbol;
                nextSymbol();
                parseExpression(fsys, lev);
                switch (relop) {
                    case equal:
                        interpreter.generatePCode(Fct.OPR, 0, 8);
                        break;
                    case neq:
                        interpreter.generatePCode(Fct.OPR, 0, 9);
                        break;
                    case lss:
                        interpreter.generatePCode(Fct.OPR, 0, 10);
                        break;
                    case geq:
                        interpreter.generatePCode(Fct.OPR, 0, 11);
                        break;
                    case gtr:
                        interpreter.generatePCode(Fct.OPR, 0, 12);
                        break;
                    case leq:
                        interpreter.generatePCode(Fct.OPR, 0, 13);
                        break;
                }
            } else {
                Err.report(20);
            }
        }
    }

    /**
     * ����ע��
     *
     * @author: KanModel
     */
    private void isComment() {
        while (currentSymbol == Symbol.comment) {
            nextSymbol();
        }
    }

    /**
     * ����<���ʽ>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parsePlusMinusAssign(SymSet fsys, int lev) {
        Symbol addop;
        SymSet nxtlev;

        // ����{[+|-|++|--]<��>}
        if (currentSymbol == Symbol.plusplus || currentSymbol == Symbol.minusminus) {
            addop = currentSymbol;
            nextSymbol();

            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.plusplus);
            nxtlev.set(Symbol.minusminus);
            parseTerm(nxtlev, lev);//��������

            if (addop == Symbol.plusplus) {
                interpreter.generatePCode(Fct.OPR, 0, 17);
            } else {
                interpreter.generatePCode(Fct.OPR, 0, 18);
            }

            int i = identTable.position(scanner.id);
            if (i > 0) {
                Table.Item item = identTable.get(i);
                if (item.kind == Objekt.variable) {
                    interpreter.generatePCode(Fct.STO, lev - item.level, item.adr);//����ջ��������ֵ
                } else {
                    Err.report(12);                        // ��ֵ����ʽ����
                }
            } else {
                Err.report(11);                            // ����δ�ҵ�
            }
        }
    }

    /**
     * ����<�������>
     *
     * @param fsys  ������ż�
     * @param level ��ǰ���
     */
    private void parseSqrtStatement(SymSet fsys, int level) {
        SymSet nxtlev;

        nextSymbol();
        if (currentSymbol == Symbol.lParen) {
            nextSymbol();
            nxtlev = (SymSet) fsys.clone();//������ż��Ŀ��� ���ڴ�����ʽ����
            nxtlev.set(Symbol.rParen);//��Ӻ������ ������
            parseFactor(nxtlev, level);
            interpreter.generatePCode(Fct.OPR, 0, 19);

            if (currentSymbol == Symbol.rParen) {
                nextSymbol();
            } else {
                Err.report(33);
            }
        }
    }

    /**
     * ����<����˵������>
     *
     * @param level ��ǰ���
     */
    private void parseArrayDeclaration(int level) {
        if (currentSymbol == Symbol.ident) {
            // ��д���ֱ��ı��ջ֡������
            nextSymbol();
            if (currentSymbol == Symbol.lSquBra) {
                nextSymbol();
                if (currentSymbol == Symbol.number) {
                    nextSymbol();
                    for (int i = 0; i < scanner.num; i++) {
                        identTable.enter(Objekt.array, level, dataSize);
                        dataSize++;
                    }
                }
            }
            if (currentSymbol == Symbol.rSquBra) {
                nextSymbol();
            } else {
                Err.report(22);
            }
        } else {
            Err.report(4);                    // var ��Ӧ�Ǳ�ʶ
        }
    }
}

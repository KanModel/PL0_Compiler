package compiler;

/**
 * �����﷨������������PL/0������������Ҫ�Ĳ��֣����﷨�����Ĺ����д������﷨�������Ŀ��������ɡ�
 */
public class Parser {
    private Scanner scanner;                    // �Դʷ�������������
    private Table table;                    // �Է��ű�������
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

    /**
     * ���첢��ʼ���﷨�����������������C���԰汾��init()������һ���ִ���
     *
     * @param l �������Ĵʷ�������
     * @param t �������ķ��ű�
     * @param i ��������Ŀ�����������
     */
    public Parser(Scanner l, Table t, Interpreter i) {
        scanner = l;
        table = t;
        interpreter = i;

        // ����������ʼ���ż�
        declarationBeginSet = new SymSet(SYMBOL_NUM);
        declarationBeginSet.set(Symbol.constSym);
        declarationBeginSet.set(Symbol.varSym);
        declarationBeginSet.set(Symbol.procSym);

        // ������俪ʼ���ż�
        statementBeginSet = new SymSet(SYMBOL_NUM);
        statementBeginSet.set(Symbol.beginSym);
        statementBeginSet.set(Symbol.callSym);
        statementBeginSet.set(Symbol.ifSym);
        statementBeginSet.set(Symbol.whileSym);
        statementBeginSet.set(Symbol.readSym);            // thanks to elu
        statementBeginSet.set(Symbol.writeSym);

        // �������ӿ�ʼ���ż�
        factorBeginSet = new SymSet(SYMBOL_NUM);
        factorBeginSet.set(Symbol.ident);
        factorBeginSet.set(Symbol.number);
        factorBeginSet.set(Symbol.lparen);

    }

    /**
     * �����﷨�������̣���ǰ�����ȵ���һ��nextSymbol()
     *
     * @see #nextSymbol()
     */
    public void parse() {
//        SymSet nxtlev = new SymSet(SYMBOL_NUM);
        SymSet nextLevel = new SymSet(SYMBOL_NUM);
        nextLevel.or(declarationBeginSet);
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
     * @param errcode �����
     */
    void test(SymSet s1, SymSet s2, int errcode) {
        // ��ĳһ���֣���һ����䣬һ������ʽ����Ҫ����ʱʱ����ϣ����һ����������ĳ����
        //���ò��ֵĺ�����ţ���test���������⣬���Ҹ��𵱼�ⲻͨ��ʱ�Ĳ��ȴ�ʩ����
        // ������Ҫ���ʱָ����ǰ��Ҫ�ķ��ż��ϺͲ����õļ��ϣ���֮ǰδ��ɲ��ֵĺ����
        // �ţ����Լ���ⲻͨ��ʱ�Ĵ���š�
        if (!s1.get(currentSymbol)) {
            Err.report(errcode);
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
        tx0 = table.tableSize;                    // ��¼�������ֵĳ�ʼλ�ã��Ա�ָ���
        table.get(table.tableSize).adr = interpreter.cx;

        interpreter.gen(Fct.JMP, 0, 0);

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

//                isComment();
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

//                isComment();
            }

////             <ע��˵������>
//            while (currentSymbol == Symbol.comment) {
//                nextSymbol();
//            }

            // <����˵������>
            while (currentSymbol == Symbol.procSym) {
                nextSymbol();
                if (currentSymbol == Symbol.ident) {
                    table.enter(Objekt.procedure, level, dataSize);
                    nextSymbol();
                } else {
                    Err.report(4);                // procedure��ӦΪ��ʶ��
                }

                if (currentSymbol == Symbol.semicolon) {
                    nextSymbol();
                } else {
                    Err.report(5);                // ©���˷ֺ�
                }

//                isComment();

                nextLevel = (SymSet) fsys.clone();
                nextLevel.set(Symbol.semicolon);
                parseBlock(level + 1, nextLevel);

                if (currentSymbol == Symbol.semicolon) {
                    nextSymbol();

//                    isComment();

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
        Table.Item item = table.get(tx0);
        interpreter.code[item.adr].a = interpreter.cx;
        item.adr = interpreter.cx;                    // ��ǰ���̴����ַ
        item.size = dataSize;                            // ����������ÿ����һ�����������dx����1��
        // ���������Ѿ�������dx���ǵ�ǰ���̵Ķ�ջ֡��С
        cx0 = interpreter.cx;
        interpreter.gen(Fct.INT, 0, dataSize);            // ���ɷ����ڴ����

        table.debugTable(tx0);

        // ����<���>
        nextLevel = (SymSet) fsys.clone();        // ÿ��������ż��Ͷ������ϲ������ż��ͣ��Ա㲹��
        nextLevel.set(Symbol.semicolon);        // ���������Ϊ�ֺŻ�end
        nextLevel.set(Symbol.endSym);
        parseStatement(nextLevel, level);
        interpreter.gen(Fct.OPR, 0, 0);        // ÿ�����̳��ڶ�Ҫʹ�õ��ͷ����ݶ�ָ��

        nextLevel = new SymSet(SYMBOL_NUM);    // �ֳ���û�в��ȼ���
        test(fsys, nextLevel, 8);                // �����������ȷ��

        interpreter.listCode(cx0);

        dataSize = dx0;                            // �ָ���ջ֡������
        table.tableSize = tx0;                        // �ظ����ֱ�λ��
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
                    table.enter(Objekt.constant, level, dataSize);
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
            // ��д���ֱ����ı��ջ֡������
            table.enter(Objekt.variable, level, dataSize);
            dataSize++;
            nextSymbol();
        } else {
            Err.report(4);                    // var ��Ӧ�Ǳ�ʶ
        }
    }

    /**
     * ����<���>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    void parseStatement(SymSet fsys, int lev) {
        SymSet nxtlev;
        // Wirth �� PL/0 ������ʹ��һϵ�е�if...else...������
        // �������������Ϊ�����д���ܹ���������ؿ�����������Ĵ����߼�
        switch (currentSymbol) {
            case ident:
                parseAssignStatement(fsys, lev);
                break;
            case readSym:
                parseReadStatement(fsys, lev);
                break;
            case writeSym:
                parseWriteStatement(fsys, lev);
                break;
            case callSym:
                parseCallStatement(fsys, lev);
                break;
            case ifSym:
                parseIfStatement(fsys, lev);
                break;
            case beginSym:
                parseBeginStatement(fsys, lev);
                break;
            case whileSym:
                parseWhileStatement(fsys, lev);
                break;
            default:
                nxtlev = new SymSet(SYMBOL_NUM);
                test(fsys, nxtlev, 19);
                break;
        }
//        isComment();
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
        interpreter.gen(Fct.JPC, 0, 0);                // ����������ת��������ѭ���ĵ�ַδ֪
        if (currentSymbol == Symbol.doSym)
            nextSymbol();
        else
            Err.report(18);                        // ȱ��do
        parseStatement(fsys, lev);                // ����<���>
        interpreter.gen(Fct.JMP, 0, cx1);            // ��ͷ�����ж�����
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
        interpreter.gen(Fct.JPC, 0, 0);                // ����������תָ���ת��ַδ֪����ʱд0
        parseStatement(fsys, lev);                // ����then������
        interpreter.code[cx1].a = interpreter.cx;            // ��statement������cxΪthen�����ִ��
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
            i = table.position(scanner.id);
            if (i == 0) {
                Err.report(11);                    // ����δ�ҵ�
            } else {
                Table.Item item = table.get(i);
                if (item.kind == Objekt.procedure)
                    interpreter.gen(Fct.CAL, lev - item.level, item.adr);
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
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseWriteStatement(SymSet fsys, int lev) {
        SymSet nxtlev;

        nextSymbol();
        if (currentSymbol == Symbol.lparen) {
            do {
                nextSymbol();
                nxtlev = (SymSet) fsys.clone();
                nxtlev.set(Symbol.rparen);
                nxtlev.set(Symbol.comma);
                parseExpression(nxtlev, lev);
                interpreter.gen(Fct.OPR, 0, 14);
            } while (currentSymbol == Symbol.comma);

            if (currentSymbol == Symbol.rparen) {
                nextSymbol();
//                isComment();
            } else {
                Err.report(33);
            }                // write()��ӦΪ��������ʽ
        }
        interpreter.gen(Fct.OPR, 0, 15);
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
        if (currentSymbol == Symbol.lparen) {
            do {
                nextSymbol();
                if (currentSymbol == Symbol.ident)
                    i = table.position(scanner.id);
                else
                    i = 0;

                if (i == 0) {
                    Err.report(35);            // read()��Ӧ���������ı�����
                } else {
                    Table.Item item = table.get(i);
                    if (item.kind != Objekt.variable) {
                        Err.report(32);        // read()�еı�ʶ�����Ǳ���, thanks to amd
                    } else {
                        interpreter.gen(Fct.OPR, 0, 16);
                        interpreter.gen(Fct.STO, lev - item.level, item.adr);
                    }
                }

                nextSymbol();
            } while (currentSymbol == Symbol.comma);
        } else {
            Err.report(34);                    // ��ʽ����Ӧ��������
        }

        if (currentSymbol == Symbol.rparen) {
            nextSymbol();

//            isComment();

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
        int i;
        SymSet nxtlev;

        i = table.position(scanner.id);
        if (i > 0) {
            Table.Item item = table.get(i);
            if (item.kind == Objekt.variable) {
                nextSymbol();
                if (currentSymbol == Symbol.becomes)
                    nextSymbol();
                else
                    Err.report(13);                    // û�м�⵽��ֵ����
                nxtlev = (SymSet) fsys.clone();
                parseExpression(nxtlev, lev);
                // parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
                interpreter.gen(Fct.STO, lev - item.level, item.adr);
            } else {
                Err.report(12);                        // ��ֵ����ʽ����
            }
        } else {
            Err.report(11);                            // ����δ�ҵ�
        }
    }

    /**
     * ����<����ʽ>
     *
     * @param fsys ������ż�
     * @param lev  ��ǰ���
     */
    private void parseExpression(SymSet fsys, int lev) {
        Symbol addop;
        SymSet nxtlev;

        // ����[+|-]<��>
        if (currentSymbol == Symbol.plus || currentSymbol == Symbol.minus) {
            addop = currentSymbol;
            nextSymbol();
            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.plus);
            nxtlev.set(Symbol.minus);
            parseTerm(nxtlev, lev);
            if (addop == Symbol.minus)
                interpreter.gen(Fct.OPR, 0, 1);
        } else {
            nxtlev = (SymSet) fsys.clone();
            nxtlev.set(Symbol.plus);
            nxtlev.set(Symbol.minus);
            parseTerm(nxtlev, lev);
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
                interpreter.gen(Fct.OPR, 0, 2);
            else
                interpreter.gen(Fct.OPR, 0, 3);
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
                interpreter.gen(Fct.OPR, 0, 4);
            else
                interpreter.gen(Fct.OPR, 0, 5);
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
                int i = table.position(scanner.id);
                if (i > 0) {
                    Table.Item item = table.get(i);
                    switch (item.kind) {
                        case constant:            // ����Ϊ����
                            interpreter.gen(Fct.LIT, 0, item.val);
                            break;
                        case variable:            // ����Ϊ����
                            interpreter.gen(Fct.LOD, lev - item.level, item.adr);
                            break;
                        case procedure:            // ����Ϊ����
                            Err.report(21);                // ����Ϊ����
                            break;
                    }
                } else {
                    Err.report(11);                    // ��ʶ��δ����
                }
                nextSymbol();
            } else if (currentSymbol == Symbol.number) {    // ����Ϊ��
                int num = scanner.num;
                if (num > PL0.MAX_NUM) {
                    Err.report(31);//������ֵ��Χ
                    num = 0;
                }
                interpreter.gen(Fct.LIT, 0, num);
                nextSymbol();
            } else if (currentSymbol == Symbol.lparen) {    // ����Ϊ����ʽ
                nextSymbol();
                nxtlev = (SymSet) fsys.clone();
                nxtlev.set(Symbol.rparen);
                parseExpression(nxtlev, lev);
                if (currentSymbol == Symbol.rparen)
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
            // ���� ODD<����ʽ>
            nextSymbol();
            parseExpression(fsys, lev);
            interpreter.gen(Fct.OPR, 0, 6);
        } else {
            // ����<����ʽ><��ϵ�����><����ʽ>
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
                        interpreter.gen(Fct.OPR, 0, 8);
                        break;
                    case neq:
                        interpreter.gen(Fct.OPR, 0, 9);
                        break;
                    case lss:
                        interpreter.gen(Fct.OPR, 0, 10);
                        break;
                    case geq:
                        interpreter.gen(Fct.OPR, 0, 11);
                        break;
                    case gtr:
                        interpreter.gen(Fct.OPR, 0, 12);
                        break;
                    case leq:
                        interpreter.gen(Fct.OPR, 0, 13);
                        break;
                }
            } else {
                Err.report(20);
            }
        }
    }

    /**
     * ����ע��
     * @author: KanModel
     */
    void isComment() {
        while (currentSymbol == Symbol.comment) {
            nextSymbol();
        }
    }
}
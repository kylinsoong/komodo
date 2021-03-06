/*
 * JBoss, Home of Professional Open Source.
* See the COPYRIGHT.txt file distributed with this work for information
* regarding copyright ownership. Some portions may be licensed
* to Red Hat, Inc. under one or more contributor license agreements.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
* 02110-1301 USA.
*/
package org.teiid.query.sql.v8;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.teiid.core.types.DefaultDataTypeManager;
import org.komodo.spi.runtime.version.TeiidVersion;
import org.komodo.spi.runtime.version.DefaultTeiidVersion.Version;
import org.teiid.language.SQLConstants.NonReserved;
import org.teiid.query.parser.TeiidNodeFactory.ASTNodes;
import org.teiid.query.sql.AbstractTestSQLStringVisitor;
import org.teiid.query.sql.lang.DeleteImpl;
import org.teiid.query.sql.lang.FromImpl;
import org.teiid.query.sql.lang.InsertImpl;
import org.teiid.query.sql.lang.QueryImpl;
import org.teiid.query.sql.lang.SelectImpl;
import org.teiid.query.sql.proc.AssignmentStatementImpl;
import org.teiid.query.sql.proc.BlockImpl;
import org.teiid.query.sql.proc.CommandStatementImpl;
import org.teiid.query.sql.proc.CreateProcedureCommandImpl;
import org.teiid.query.sql.proc.RaiseStatementImpl;
import org.teiid.query.sql.proc.StatementImpl;
import org.teiid.query.sql.symbol.BaseAggregateSymbol;
import org.teiid.query.sql.symbol.ArraySymbolImpl;
import org.teiid.query.sql.symbol.ConstantImpl;
import org.teiid.query.sql.symbol.ElementSymbolImpl;
import org.teiid.query.sql.symbol.BaseExpression;

/**
 *
 */
@SuppressWarnings( {"nls", "javadoc"} )
public class Test8SQLStringVisitor extends AbstractTestSQLStringVisitor {

    private Test8Factory factory;

    protected Test8SQLStringVisitor(TeiidVersion teiidVersion) {
        super(teiidVersion);
    }

    public Test8SQLStringVisitor() {
        this(Version.TEIID_8_0.get());
    }

    @Override
    protected Test8Factory getFactory() {
        if (factory == null)
            factory = new Test8Factory(parser);

        return factory;
    }


    @Test
    public void testMerge1() {
        InsertImpl insert = getFactory().newInsert();
        insert.setMerge(true);
        insert.setGroup(getFactory().newGroupSymbol("m.g1"));

        List<ElementSymbolImpl> vars = new ArrayList<ElementSymbolImpl>();
        vars.add(getFactory().newElementSymbol("e1"));
        vars.add(getFactory().newElementSymbol("e2"));
        insert.setVariables(vars);
        List<ConstantImpl> values = new ArrayList<ConstantImpl>();
        values.add(getFactory().newConstant(new Integer(5)));
        values.add(getFactory().newConstant("abc"));
        insert.setValues(values);

        helpTest(insert, "MERGE INTO m.g1 (e1, e2) VALUES (5, 'abc')");
    }

    @Test
    public void testAggregateSymbol1() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.COUNT, false, getFactory().newConstant("abc"));
        helpTest(agg, "COUNT('abc')");
    }

    @Test
    public void testAggregateSymbol2() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.COUNT, true, getFactory().newConstant("abc"));
        helpTest(agg, "COUNT(DISTINCT 'abc')");
    }

    @Test
    public void testAggregateSymbol3() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.COUNT, false, null);
        helpTest(agg, "COUNT(*)");
    }

    @Test
    public void testAggregateSymbol4() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.AVG, false, getFactory().newConstant("abc"));
        helpTest(agg, "AVG('abc')");
    }

    @Test
    public void testAggregateSymbol5() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.SUM, false, getFactory().newConstant("abc"));
        helpTest(agg, "SUM('abc')");
    }

    @Test
    public void testAggregateSymbol6() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.MIN, false, getFactory().newConstant("abc"));
        helpTest(agg, "MIN('abc')");
    }

    @Test
    public void testAggregateSymbol7() {
        BaseAggregateSymbol agg = getFactory().newAggregateSymbol(NonReserved.MAX, false, getFactory().newConstant("abc"));
        helpTest(agg, "MAX('abc')");
    }

    @Test
    public void testRaiseErrorStatement() {
        StatementImpl errStmt = getFactory().newRaiseStatement(getFactory().newConstant("My Error"));
        helpTest(errStmt, "RAISE 'My Error';");
    }

    @Test
    public void testRaiseErrorStatementWithExpression() {
        StatementImpl errStmt = getFactory().newRaiseStatement(getFactory().newElementSymbol("a"));
        helpTest(errStmt, "RAISE a;");
    }

    @Test
    public void testCommandStatement1a() {
        QueryImpl q1 = getFactory().newQuery();
        SelectImpl select = getFactory().newSelect();
        select.addSymbol(getFactory().newElementSymbol("x"));
        q1.setSelect(select);
        FromImpl from = getFactory().newFrom();
        from.addGroup(getFactory().newGroupSymbol("g"));
        q1.setFrom(from);

        CommandStatementImpl cmdStmt = getFactory().newCommandStatement(q1);
        cmdStmt.setReturnable(false);
        helpTest(cmdStmt, "SELECT x FROM g WITHOUT RETURN;");
    }

    @Test
    public void testBlock1() {
        DeleteImpl d1 = getFactory().newNode(ASTNodes.DELETE);
        d1.setGroup(getFactory().newGroupSymbol("g"));
        CommandStatementImpl cmdStmt = getFactory().newCommandStatement(d1);
        AssignmentStatementImpl assigStmt = getFactory().newAssignmentStatement(getFactory().newElementSymbol("a"), getFactory().newConstant(new Integer(1)));
        StatementImpl errStmt = getFactory().newRaiseStatement(getFactory().newConstant("My Error"));
        BlockImpl b = getFactory().newBlock();
        b.addStatement(cmdStmt);
        b.addStatement(assigStmt);
        b.addStatement(errStmt);
        helpTest(b, "BEGIN\nDELETE FROM g;\na = 1;\nRAISE 'My Error';\nEND");
    }

    @Test
    public void testCreateUpdateProcedure1() {
        DeleteImpl d1 = getFactory().newNode(ASTNodes.DELETE);
        d1.setGroup(getFactory().newGroupSymbol("g"));
        CommandStatementImpl cmdStmt = getFactory().newCommandStatement(d1);
        AssignmentStatementImpl assigStmt = getFactory().newAssignmentStatement(getFactory().newElementSymbol("a"), getFactory().newConstant(new Integer(1)));
        RaiseStatementImpl errStmt = getFactory().newRaiseStatement(getFactory().newConstant("My Error"));
        BlockImpl b = getFactory().newBlock();
        b.addStatement(cmdStmt);
        b.addStatement(assigStmt);
        b.addStatement(errStmt);
        CreateProcedureCommandImpl cup = getFactory().newCreateProcedureCommand(b);
        helpTest(cup, "CREATE VIRTUAL PROCEDURE\nBEGIN\nDELETE FROM g;\na = 1;\nRAISE 'My Error';\nEND");
    }

    @Test
    public void testCreateUpdateProcedure2() {
        DeleteImpl d1 = getFactory().newNode(ASTNodes.DELETE);
        d1.setGroup(getFactory().newGroupSymbol("g"));
        CommandStatementImpl cmdStmt = getFactory().newCommandStatement(d1);
        AssignmentStatementImpl assigStmt = getFactory().newAssignmentStatement(getFactory().newElementSymbol("a"), getFactory().newConstant(new Integer(1)));
        RaiseStatementImpl errStmt = getFactory().newRaiseStatement(getFactory().newConstant("My Error"));
        BlockImpl b = getFactory().newBlock();
        b.addStatement(cmdStmt);
        b.addStatement(assigStmt);
        b.addStatement(errStmt);
        CreateProcedureCommandImpl cup = getFactory().newCreateProcedureCommand(b);
        helpTest(cup, "CREATE VIRTUAL PROCEDURE\nBEGIN\nDELETE FROM g;\na = 1;\nRAISE 'My Error';\nEND");
    }

    @Test
    public void testCreateUpdateProcedure3() {
        DeleteImpl d1 = getFactory().newNode(ASTNodes.DELETE);
        d1.setGroup(getFactory().newGroupSymbol("g"));
        CommandStatementImpl cmdStmt = getFactory().newCommandStatement(d1);
        AssignmentStatementImpl assigStmt = getFactory().newAssignmentStatement(getFactory().newElementSymbol("a"), getFactory().newConstant(new Integer(1)));
        StatementImpl errStmt = getFactory().newRaiseStatement(getFactory().newConstant("My Error"));
        BlockImpl b = getFactory().newBlock();
        b.addStatement(cmdStmt);
        b.addStatement(assigStmt);
        b.addStatement(errStmt);
        CreateProcedureCommandImpl cup = getFactory().newCreateProcedureCommand(b);
        helpTest(cup, "CREATE VIRTUAL PROCEDURE\nBEGIN\nDELETE FROM g;\na = 1;\nRAISE 'My Error';\nEND");
    }

    @Test
    public void testArray() {
        List<BaseExpression> expr = new ArrayList<BaseExpression>();
        expr.add(getFactory().newElementSymbol("e1"));
        expr.add(getFactory().newConstant(1));
        ArraySymbolImpl array = getFactory().newArray(
                                            DefaultDataTypeManager.DefaultDataTypes.INTEGER.getTypeClass(),
                                            expr);
        helpTest(array, "(e1, 1)");
    }

    @Test
    public void testReturnStatement() throws Exception {
        helpTest(parser.parseProcedure("begin if (true) return 1; return; end", false),
                 "CREATE VIRTUAL PROCEDURE\nBEGIN\nIF(TRUE)\nBEGIN\nRETURN 1;\nEND\nRETURN;\nEND");
    }

    @Test
    public void testConditionNesting() throws Exception {
        String sql = "select (intkey = intnum) is null, (intkey < intnum) in (true, false) from bqt1.smalla";

        helpTest(parser.parseCommand(sql),
                 "SELECT (intkey = intnum) IS NULL, (intkey < intnum) IN (TRUE, FALSE) FROM bqt1.smalla");
    }

    @Test
    public void testSubqueryNameEscaping() throws Exception {
        helpTest(getFactory().newSubqueryFromClause("user", parser.parseCommand("select 1")),
                 "(SELECT 1) AS \"user\"");
    }

}

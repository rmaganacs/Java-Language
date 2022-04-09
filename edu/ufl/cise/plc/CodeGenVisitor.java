package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.PLCRuntimeException;

import java.util.*;

public class CodeGenVisitor implements ASTVisitor {


    private final String packageName;
    private final Set<String> imports;

    private void GenTypeConversion(Types.Type type, Types.Type coerceTo, CodeGenStringBuilder sb)
    {
        if(type != coerceTo && coerceTo != null)
            sb.lparen().append(coerceTo.name().toLowerCase(Locale.ROOT)).rparen();
    }
    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
        imports = new HashSet<>();
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append(booleanLitExpr.getText());
        return sb;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

        String value = stringLitExpr.getValue();
        sb.append("\"\"\"").newline().append(value).append("\"\"\"");
        return sb;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if(intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT){
            sb.lparen().append(intLitExpr.getCoerceTo().name().toLowerCase(Locale.ROOT)).rparen();
            sb.append(String.valueOf((intLitExpr.getValue())));
        }else{
            sb.append(String.valueOf((intLitExpr.getValue())));
        }
        return sb;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

        if(floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT){
            sb.lparen().append(floatLitExpr.getCoerceTo().name().toLowerCase(Locale.ROOT)).rparen();
            sb.append(String.valueOf((floatLitExpr.getValue()))).append("f");
        }else{
            sb.append(String.valueOf((floatLitExpr.getValue()))).append("f");
        }
        return sb;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) {
        return null;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) {
        imports.add("import edu.ufl.cise.plc.runtime.ConsoleIO;");
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String type = "";
        switch (consoleExpr.getCoerceTo().name().toLowerCase(Locale.ROOT)) {
            case "int" -> {
                sb.append("(Integer)").space();
                type = "\"INT\", \"Enter integer:\"";
            }
            case "float" -> {
                sb.append("(Float)").space();
                type = "\"FLOAT\", \"Enter float:\"";
            }
            case "string" -> {
                sb.append("(String)").space();
                type = "\"STRING\", \"Enter string:\"";
            }
            case "boolean" -> {
                sb.append("(Boolean)").space();
                type = "\"BOOLEAN\", \"Enter boolean:\"";
            }
        }
        sb.append("ConsoleIO.readValueFromConsole(").append(type).rparen();
        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.lparen().space();

        if(unaryExpression.getOp().getKind() == IToken.Kind.MINUS)
        {
            sb.append("-").space();
        }
        else if(unaryExpression.getOp().getKind() == IToken.Kind.BANG)
        {
            sb.append("!").space();
        }
        unaryExpression.getExpr().visit(this, sb);
        sb.space().rparen();
        return sb;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        IToken.Kind op = binaryExpr.getOp().getKind();

        if(op == IToken.Kind.COLOR_OP || op == IToken.Kind.COLOR_CONST || op == IToken.Kind.IMAGE_OP) {
            throw new PLCRuntimeException("Binary Expr not implemented yet");
        }
        else {

            sb.lparen();
            if((binaryExpr.getOp().getKind() == IToken.Kind.EQUALS || binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS) && binaryExpr.getRight().getType() == Types.Type.STRING){

                if(binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS)
                    sb.append("!");

                binaryExpr.getLeft().visit(this, sb);
                sb.append(".equals(");
                binaryExpr.getRight().visit(this, sb);
                sb.rparen();
            }
            else{
                binaryExpr.getLeft().visit(this, sb);
                sb.space().append(binaryExpr.getOp().getText()).space();
                binaryExpr.getRight().visit(this, sb);
            }
            sb.rparen();
        }

        return sb;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if(identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()){
            sb.lparen().append(identExpr.getCoerceTo().name().toLowerCase(Locale.ROOT)).rparen();
            sb.append(identExpr.getText());
        }else{
            sb.append(identExpr.getText());
        }
        return sb;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.space();
        sb.lparen();
        conditionalExpr.getCondition().visit(this, sb);
        sb.space().append(" ? ");
        conditionalExpr.getTrueCase().visit(this, sb);
        sb.space().append(": ");
        conditionalExpr.getFalseCase().visit(this, sb);
        sb.rparen();
        return sb;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = assignmentStatement.getName();
        Expr expr = assignmentStatement.getExpr();

        sb.append(name);
        sb.append(" = ");
        GenTypeConversion(assignmentStatement.getExpr().getType(), assignmentStatement.getExpr().getCoerceTo(), sb);
        expr.visit(this, sb);
        sb.semi().newline();

        return sb;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("ConsoleIO.console.println(");
        writeStatement.getSource().visit(this, sb);
        sb.rparen().semi().newline();
        return sb;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = readStatement.getName();
        Expr expr = readStatement.getSource();

        if(expr.getType() != Types.Type.CONSOLE) {
            throw new PLCRuntimeException("Read only implemented for CONSOLE");
        }
        else{
            sb.append(name);
            sb.append(" = ");
            expr.visit(this, sb);
            sb.semi().newline();
        }

        return sb;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        CodeGenStringBuilder sb =  new CodeGenStringBuilder();
        List<NameDef> params = program.getParams();
        List<ASTNode> decsAndStatements = program.getDecsAndStatements();
        sb.append("package ").append(packageName).semi().newline();

        CodeGenStringBuilder sb1 =  new CodeGenStringBuilder();
        sb1.append("public class ");
        sb1.append(program.getName());
        sb1.append(" {").newline();
        if(program.getReturnType() == Types.Type.STRING){
            sb1.append("public static ").append("String").append(" apply");
        }else{
            sb1.append("public static ").append(program.getReturnType().name().toLowerCase(Locale.ROOT)).append(" apply");
        }
        sb1.lparen();
        for (int i = 0; i < params.size(); i++) {
            if(i == (params.size() - 1)){
                params.get(i).visit(this, sb1);
            }else {
                params.get(i).visit(this, sb1);
                sb1.comma().space();
            }
        }
        sb1.rparen().append("{").newline();
        for (ASTNode decsAndStatement : decsAndStatements) {
            decsAndStatement.visit(this, sb1);
        }
        sb1.append("}").newline().append("}");

        for(String stock: imports){
            sb.append(stock).newline();
        }
        //System.out.println(sb1);
        String temp = sb1.delegate.toString();
        sb.append(temp);
        return sb.delegate.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if(nameDef.getType() == Types.Type.STRING){
            sb.append("String");
        }else{
            sb.append(nameDef.getType().name().toLowerCase(Locale.ROOT));
        }
        sb.append(" ");
        String name = nameDef.getName();
        sb.append(name);

        return sb;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr expr = returnStatement.getExpr();

        sb.append("return ");
        expr.visit(this, sb);
        sb.semi().newline();

        return sb;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if(declaration.getOp() == null){
            declaration.getNameDef().visit(this, sb);
        }else if(declaration.getOp().getKind() == IToken.Kind.ASSIGN){
            declaration.getNameDef().visit(this, sb);
            sb.append(" = ");
            GenTypeConversion(declaration.getExpr().getType(), declaration.getExpr().getCoerceTo(), sb);
            declaration.getExpr().visit(this, sb);
        }else if(declaration.getOp().getKind() == IToken.Kind.LARROW){
            declaration.getNameDef().visit(this, sb);
            sb.append(" = ");
            declaration.getExpr().visit(this, sb);
        }
        sb.semi().newline();
        return sb;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        return null;
    }
}

package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.PLCRuntimeException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        imports.add("import java.awt.Color;");
        String color = colorConstExpr.getText();
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("ColorTuple.unpack(Color.").append(color).append(".getRGB())");
        return sb;
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
        //TODO COLOR INPUT
        sb.append("ConsoleIO.readValueFromConsole(").append(type).rparen();
        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        imports.add("import edu.ufl.cise.plc.runtime.ColorTuple;");
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("new ColorTuple(");
        colorExpr.getRed().visit(this, sb);
        sb.append(", ");
        colorExpr.getGreen().visit(this, sb);
        sb.append(", ");
        colorExpr.getBlue().visit(this, sb);
        sb.rparen();
        return sb;
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
        //TODO
        return sb;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        IToken.Kind op = binaryExpr.getOp().getKind();
        String operator = binaryExpr.getOp().getKind().name();
        String type = binaryExpr.getLeft().getType().toString();
        if(binaryExpr.getLeft().getType() == Types.Type.IMAGE) {
            String left = binaryExpr.getLeft().getText();
            String right = binaryExpr.getRight().getText();
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            if(binaryExpr.getRight().getType() == Types.Type.IMAGE){
                sb.append("ImageOps.binaryImageImageOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append(right).rparen();
            }else if(binaryExpr.getRight().getType() == Types.Type.INT){
                sb.append("ImageOps.binaryImageScalarOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append(right).rparen();
            }
        }else if(binaryExpr.getLeft().getType() == Types.Type.COLOR) {
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            String left = binaryExpr.getLeft().getText();
            String right = binaryExpr.getRight().getText();
            if(binaryExpr.getRight().getType() == Types.Type.COLOR){
                sb.append("ImageOps.binaryTupleOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append(right).rparen();
            }
            //TODO COLORFLOAT
            //TODO LAST CONDITION IN BINARY
        }else {
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
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String width = dimension.getWidth().getText();
        String height = dimension.getHeight().getText();
        sb.append(width).comma().space().append(height);
        return sb;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        return sb;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = assignmentStatement.getName();
        String exprName = assignmentStatement.getExpr().getText();
        Expr expr = assignmentStatement.getExpr();
        if(assignmentStatement.getSelector() != null){
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            String x = assignmentStatement.getSelector().getX().getText();
            String y = assignmentStatement.getSelector().getY().getText();
            if(assignmentStatement.getExpr().getType() == Types.Type.IMAGE) {
                sb.append("ImageOps.resize(").append(exprName);
                sb.space().append(x).comma().space().append(y).rparen();
            }else if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.COLOR){
                //TODO If <expr>.coerceTo is int, the int is used as a single color component in a ColorTuple where all three color components have the value of the int.  (The value is truncated, so values outside of [0, 256) will be either white or black.)
                imports.add("import edu.ufl.cise.plc.runtime.ColorTuple;");
                imports.add("import edu.ufl.cise.plc.runtime.ColorTupleFloat;");
                sb.append("for(int x = 0; x < ").append(name).append(".getWidth(); x++)").newline();
                sb.append("for(int y = 0; y < ").append(name).append(".getWidth(); y++)").newline();
                sb.append("ImageOps.setColor(").append(name).comma().append(" x, y, ");
                assignmentStatement.getExpr().visit(this, sb);
                sb.rparen();
            }else if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.INT){

            }
        }else if(assignmentStatement.getSelector() == null && assignmentStatement.getExpr().getType() == Types.Type.IMAGE){
            //TODO
            sb.append("ImageOps.resize(").append(exprName);
            sb.append("ImageOps.clone(").append(exprName).rparen();
        }else{
            sb.append(name);
            sb.append(" = ");
            GenTypeConversion(assignmentStatement.getExpr().getType(), assignmentStatement.getExpr().getCoerceTo(), sb);
            expr.visit(this, sb);
        }
        sb.semi().newline();

        return sb;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String sourceName = writeStatement.getSource().getText();
        //TODO If target is a file, use writeImage in FileURLIO for image types and writeValue for other types.
        if(writeStatement.getSource().getType() == Types.Type.IMAGE && writeStatement.getDest().getType() == Types.Type.CONSOLE){
            imports.add("import edu.ufl.cise.plc.runtime.ConsoleIO;");
            sb.append("ConsoleIO.displayImageOnScreen(").append(sourceName);
        }else{
            sb.append("ConsoleIO.console.println(");
            writeStatement.getSource().visit(this, sb);
        }
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
        //TODO
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
        }else if(program.getReturnType() == Types.Type.IMAGE) {
            sb1.append("public static BufferedImage apply");
        }else if(program.getReturnType() == Types.Type.COLOR) {
            sb1.append("public static ColorTuple apply");
        }else {
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
        }else if(nameDef.getType() == Types.Type.IMAGE){
            sb.append("BufferedImage");
        }else if(nameDef.getType() == Types.Type.COLOR){
            sb.append("ColorTuple");
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
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = nameDefWithDim.getName();
        sb.append("BufferedImage ").append(name);
        return sb;
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
            if(declaration.getNameDef().getType() == Types.Type.IMAGE){
                imports.add("import java.awt.image.BufferedImage;");
                imports.add("import edu.ufl.cise.plc.runtime.FileURLIO;");
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                sb.append("new BufferedImage(");
                if(declaration.getDim() != null){
                    declaration.getDim().visit(this, sb);
                    sb.append(", BufferedImage.TYPE_INT_RGB)");
                }
            }else{
                declaration.getNameDef().visit(this, sb);
            }
        }else if(declaration.getOp().getKind() == IToken.Kind.ASSIGN){
            if(declaration.getNameDef().getType() == Types.Type.IMAGE){
                imports.add("import java.awt.image.BufferedImage;");
            }
            declaration.getNameDef().visit(this, sb);
            sb.append(" = ");
            GenTypeConversion(declaration.getExpr().getType(), declaration.getExpr().getCoerceTo(), sb);
            declaration.getExpr().visit(this, sb);
        }else if(declaration.getOp().getKind() == IToken.Kind.LARROW){
            if(declaration.getNameDef().getType() == Types.Type.IMAGE){
                imports.add("import java.awt.image.BufferedImage;");
                imports.add("import edu.ufl.cise.plc.runtime.FileURLIO;");
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                sb.append("FileURLIO.readImage(");
                declaration.getExpr().visit(this, sb);
                if(declaration.getDim() != null){
                    sb.comma().space();
                    declaration.getDim().visit(this, sb);
                    sb.rparen();
                }else{
                    sb.rparen();
                }
            }else{
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                declaration.getExpr().visit(this, sb);
            }
        }
        sb.semi().newline();
        return sb;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        //TODO
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String exprName = unaryExprPostfix.getExpr().getText();
        String x = unaryExprPostfix.getSelector().getX().getText();
        String y = unaryExprPostfix.getSelector().getY().getText();
        //left.getRGB(x, y)
        sb.append("ColorTuple.unpack(").append(exprName).append(".getRGB(");
        sb.append(x).comma().space();
        sb.append(y).rparen().rparen();
        return sb;
    }
}

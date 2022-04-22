package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

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
            case "color" -> {
                sb.append("(ColorTuple)").space();
                type = "\"COLOR\", \"Enter red, green, and blue components separated with space:\"";
            }
        }
        sb.append("ConsoleIO.readValueFromConsole(").append(type).rparen();
        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        imports.add("import edu.ufl.cise.plc.runtime.ColorTuple;");
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Types.Type expr_type = colorExpr.getType();
        Types.Type expr_Coerce = colorExpr.getCoerceTo();
        if(colorExpr.getType() == Types.Type.COLORFLOAT){
            sb.append("new ColorTupleFloat(");
        }else{
            sb.append("new ColorTuple(");
        }

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
            unaryExpression.getExpr().visit(this, sb);
            sb.space().rparen();
        }
        else if(unaryExpression.getOp().getKind() == IToken.Kind.BANG)
        {
            sb.append("!").space();
            unaryExpression.getExpr().visit(this, sb);
            sb.space().rparen();
        }else if(unaryExpression.getOp().getKind() == IToken.Kind.COLOR_OP)
        {
            String value = unaryExpression.getExpr().getText();
            String colorMethod = unaryExpression.getOp().getText();
            if(unaryExpression.getExpr().getType() == Types.Type.INT){
                sb.append("ColorTuple.unpack(").append(value).rparen();
            }else if(unaryExpression.getExpr().getType() == Types.Type.COLOR){
                if(Objects.equals(colorMethod, "getRed")){
                    sb.append("ColorTuple.getRed(").append(value).rparen().space().rparen();
                }else if(Objects.equals(colorMethod, "getGreen")){
                    sb.append("ColorTuple.getGreen(").append(value).rparen().space().rparen();
                }else if(Objects.equals(colorMethod, "getBlue")){
                    sb.append("ColorTuple.getBlue(").append(value).rparen().space().rparen();
                }
            }else if(unaryExpression.getExpr().getType() == Types.Type.IMAGE){
                if(Objects.equals(colorMethod, "getRed")){
                    sb.append("ImageOps.extractRed(").append(value).rparen().space().rparen();
                }else if(Objects.equals(colorMethod, "getGreen")){
                    sb.append("ImageOps.extractGreen(").append(value).rparen().space().rparen();
                }else if(Objects.equals(colorMethod, "getBlue")){
                    sb.append("ImageOps.extractBlue(").append(value).rparen().space().rparen();
                }
            }
        }
        return sb;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        //IToken.Kind op = binaryExpr.getOp().getKind();
        String operator = binaryExpr.getOp().getKind().name();
        //String type = binaryExpr.getLeft().getType().toString();
        if(binaryExpr.getLeft().getType() == Types.Type.IMAGE) {
            String left = binaryExpr.getLeft().getText();
            String right = binaryExpr.getRight().getText();
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            if(binaryExpr.getRight().getType() == Types.Type.IMAGE){
                if(operator.equals("EQUALS") || operator.equals("NOT_EQUALS")){
                    if(operator.equals("EQUALS")){
                        sb.append("ImageOpsADD.equals(").append(left).comma().space().append(right).rparen();
                    }else if(operator.equals("NOT_EQUALS")){
                        sb.append("!(ImageOpsADD.equals(").append(left).comma().space().append(right).rparen().rparen();
                    }
                }else{
                    sb.append("ImageOps.binaryImageImageOp(ImageOps.OP.").append(operator).comma().space();
                    sb.append(left).comma().space().append(right).rparen();
                }
            }else if(binaryExpr.getRight().getType() == Types.Type.INT){
                sb.append("ImageOps.binaryImageScalarOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append(right).rparen();
            }
        }else if(binaryExpr.getLeft().getType() == Types.Type.COLOR || binaryExpr.getLeft().getType() == Types.Type.COLORFLOAT) {
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            String left = binaryExpr.getLeft().getText();
            String right = binaryExpr.getRight().getText();
            if(binaryExpr.getRight().getType() == Types.Type.COLOR){
                if(operator.equals("EQUALS") || operator.equals("NOT_EQUALS")){
                    sb.append("ImageOps.binaryTupleOp(ImageOps.BoolOP.").append(operator).comma().space();
                }else{
                    sb.append("ImageOps.binaryTupleOp(ImageOps.OP.").append(operator).comma().space();
                }
                sb.append(left).comma().space().append(right).rparen();
            }else if(binaryExpr.getRight().getType() == Types.Type.COLORFLOAT){
                sb.append("ImageOps.binaryTupleOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append(right).rparen();
            }else if(binaryExpr.getRight().getType() == Types.Type.INT){
                sb.append("ImageOps.binaryTupleOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append("new ColorTuple(").append(binaryExpr.getRight().getText()).rparen().rparen();
            }else if(binaryExpr.getRight().getType() == Types.Type.FLOAT){
                sb.append("ImageOps.binaryTupleOp(ImageOps.OP.").append(operator).comma().space();
                sb.append(left).comma().space().append("new ColorTupleFloat(").append(binaryExpr.getRight().getText()).rparen().rparen();
            }
            //TODO LAST CONDITION IN BINARY
        }else if(binaryExpr.getLeft().getCoerceTo() == Types.Type.COLOR && binaryExpr.getLeft().getType() == Types.Type.INT){
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            //String left = binaryExpr.getLeft().getText();
            //String right = binaryExpr.getRight().getText();
            sb.append("ImageOps.binaryTupleOp(ImageOps.OP.").append(operator).comma().space();
            binaryExpr.getLeft().visit(this, sb);
            sb.comma().space();
            binaryExpr.getRight().visit(this, sb);
            sb.rparen();
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
        if(pixelSelector.getX() instanceof IntLitExpr && pixelSelector.getY() instanceof IntLitExpr){
            sb.append(pixelSelector.getX().getText()).comma().space().append(pixelSelector.getY().getText());
        }
        return sb;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = assignmentStatement.getName();
        String exprName = assignmentStatement.getExpr().getText();
        Expr expr = assignmentStatement.getExpr();
        Types.Type expr_type = expr.getType();
        Types.Type expr_Coerce = expr.getCoerceTo();

        if(assignmentStatement.getTargetDec().getType() == Types.Type.IMAGE){
            imports.add("import edu.ufl.cise.plc.runtime.ImageOps;");
            if(assignmentStatement.getExpr().getType() == Types.Type.IMAGE){
                if(assignmentStatement.getTargetDec().getDim() != null){
                    String x = assignmentStatement.getTargetDec().getDim().getWidth().getText();
                    String y = assignmentStatement.getTargetDec().getDim().getHeight().getText();
                    sb.append(name).append(" = ").append("ImageOps.resize(").append(exprName);
                    sb.comma().space().append(x).comma().space().append(y).rparen();
                }else if(assignmentStatement.getTargetDec().getDim() == null){
                    if(assignmentStatement.getExpr() instanceof IdentExpr){
                        sb.append(name).append(" = ").append("ImageOps.clone(").append(exprName).rparen();
                    }
                }
            }else if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.COLOR){
                imports.add("import edu.ufl.cise.plc.runtime.ColorTuple;");
                imports.add("import edu.ufl.cise.plc.runtime.ColorTupleFloat;");
                if(assignmentStatement.getSelector() != null){
                    String x = assignmentStatement.getSelector().getX().getText();
                    String y = assignmentStatement.getSelector().getY().getText();
                    sb.append("for(int ").append(x).append("= 0; ").append(x).append(" < ").append(name).append(".getWidth(); ").append(x).append("++)").newline();
                    sb.append("for(int ").append(y).append("= 0; ").append(y).append(" < ").append(name).append(".getHeight(); ").append(y).append("++)").newline();
                    if(assignmentStatement.getExpr().getType() == Types.Type.COLOR){
                        sb.append("ImageOps.setColor(").append(name).comma().space().append(x).comma().space().append(y).comma().space();
                    }else if(assignmentStatement.getExpr().getType() == Types.Type.COLORFLOAT){
                        sb.append("ImageOpsADD.setColor(").append(name).comma().space().append(x).comma().space().append(y).comma().space();
                    }else{
                        sb.append("ImageOps.setColor(").append(name).comma().append(" x, y, ");
                    }
                }else if(assignmentStatement.getSelector() == null){
                    sb.append("for(int x = 0; x < ").append(name).append(".getWidth(); x++)").newline();
                    sb.append("for(int y = 0; y < ").append(name).append(".getHeight(); y++)").newline();
                    if(assignmentStatement.getExpr().getType() == Types.Type.COLOR){
                        sb.append("ImageOps.setColor(").append(name).comma().append(" x, y, ");
                    }else if(assignmentStatement.getExpr().getType() == Types.Type.COLORFLOAT){
                        sb.append("ImageOpsADD.setColor(").append(name).comma().append(" x, y, ");
                    }else{
                        sb.append("ImageOps.setColor(").append(name).comma().append(" x, y, ");
                    }
                }
                if(expr.getType() == Types.Type.INT){
                    sb.append("new ColorTuple(").append(exprName).rparen();
                }else{
                    assignmentStatement.getExpr().visit(this, sb);
                }
                sb.rparen();
            }else if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.INT){
                sb.append("new ColorTuple(").append(exprName).rparen();
            }
        }
        else{
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
        if(writeStatement.getSource().getType() == Types.Type.IMAGE && writeStatement.getDest().getType() == Types.Type.CONSOLE){
            imports.add("import edu.ufl.cise.plc.runtime.ConsoleIO;");
            sb.append("ConsoleIO.displayImageOnScreen(").append(sourceName);
        }else if(writeStatement.getDest().getType() == Types.Type.STRING){
            String fileName = writeStatement.getDest().getText();
            imports.add("import edu.ufl.cise.plc.runtime.ConsoleIO;");
            imports.add("import edu.ufl.cise.plc.runtime.FileURLIO;");
            if(writeStatement.getSource().getType() == Types.Type.IMAGE){
                sb.append("FileURLIO.writeImage(").append(sourceName).comma().space().append(fileName);
            }else{
                sb.append("FileURLIO.writeValue(").append(sourceName).comma().space().append(fileName);
            }
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

        if(expr.getType() == Types.Type.IMAGE) {
            imports.add("import edu.ufl.cise.plc.runtime.FileURLIO;");
            sb.append("FileURLIO.readImage(");
            if(readStatement.getSelector() != null){
                readStatement.getSource().visit(this, sb);
                sb.comma().space();
                readStatement.getSelector().visit(this, sb);
            }else if(readStatement.getSelector() == null){
                readStatement.getSource().visit(this, sb);
            }
            sb.rparen().semi().newline();
            sb.append("FileURLIO.closeFiles();");
            //TODO maybe add closFiles()
        }
        else if(expr.getType() == Types.Type.CONSOLE){
            sb.append(name);
            sb.append(" = ");
            expr.visit(this, sb);
            sb.semi().newline();
        }else if(expr.getType() == Types.Type.COLOR){
            sb.append("(ColorTuple)FileURLIO.readValueFromFile(").append(readStatement.getName());
        }
        //TODO ADD MORE TYPES MAYBE IDK WE'LL SEE...
        return sb;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        //TODO fix imports
        imports.add("import edu.ufl.cise.plc.runtime.*;");
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
            imports.add("import java.awt.image.BufferedImage;");
            sb.append("BufferedImage");
        }else if(nameDef.getType() == Types.Type.COLOR){
            imports.add("import edu.ufl.cise.plc.runtime.ColorTuple;");
            sb.append("ColorTuple");
        }else if(nameDef.getType() == Types.Type.COLORFLOAT){
            imports.add("import edu.ufl.cise.plc.runtime.ColorTupleFloat;");
            sb.append("ColorTupleFloat");
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
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                if(declaration.getExpr().getType() == Types.Type.INT){
                    sb.append("ImageOpsADD.makeConstantImage(");
                    declaration.getNameDef().getDim().visit(this, sb);
                    sb.comma().space().append(declaration.getExpr().getText()).rparen();
                }else if(Objects.equals(declaration.getExpr().getText(), "RED") || Objects.equals(declaration.getExpr().getText(), "BLUE") || Objects.equals(declaration.getExpr().getText(), "GREEN")){
                    sb.append("ImageOpsADD.makeConstantImage(");
                    declaration.getNameDef().getDim().visit(this, sb);
                    sb.comma().space();
                    declaration.getExpr().visit(this, sb);
                    sb.rparen();
                }else if(declaration.getExpr().getType() == Types.Type.COLORFLOAT){
                    sb.append("ImageOpsADD.makeConstantImage(");
                    declaration.getNameDef().getDim().visit(this, sb);
                    sb.comma().space();
                    declaration.getExpr().visit(this, sb);
                    sb.rparen();
                }else{
                    declaration.getExpr().visit(this, sb);
                }
            }else if(declaration.getNameDef().getType() == Types.Type.COLOR){
                imports.add("import edu.ufl.cise.plc.runtime.ColorTuple;");
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                if(declaration.getExpr().getType() == Types.Type.INT){
                    sb.append("new ColorTuple(").append(declaration.getExpr().getText()).rparen();
                }else{
                    declaration.getExpr().visit(this, sb);
                }
            }else{
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                GenTypeConversion(declaration.getExpr().getType(), declaration.getExpr().getCoerceTo(), sb);
                declaration.getExpr().visit(this, sb);
            }
        }else if(declaration.getOp().getKind() == IToken.Kind.LARROW){
            if(declaration.getNameDef().getType() == Types.Type.IMAGE){
                imports.add("import java.awt.image.BufferedImage;");
                imports.add("import edu.ufl.cise.plc.runtime.FileURLIO;");
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                sb.append("FileURLIO.readImage(");
                declaration.getExpr().visit(this, sb);
                //HAS DIM
                if(declaration.getDim() != null){
                    sb.comma().space();
                    declaration.getDim().visit(this, sb);
                    sb.rparen();
                }else{ // NO DIM
                    sb.rparen();
                }
            }else if(declaration.getNameDef().getType() == Types.Type.COLOR){
                declaration.getNameDef().visit(this, sb);
                sb.append(" = ");
                sb.append("(ColorTuple)FileURLIO.readValueFromFile(").append(declaration.getExpr().getText()).rparen();
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
        sb.append("ColorTuple.unpack(");
        sb.append(exprName).append(".getRGB(");
        sb.append(x).comma().space();
        sb.append(y).rparen().rparen();
        return sb;
    }
}

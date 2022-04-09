package edu.ufl.cise.plc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.Expr;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.NameDef;
import edu.ufl.cise.plc.ast.NameDefWithDim;
import edu.ufl.cise.plc.ast.PixelSelector;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.ast.ReadStatement;
import edu.ufl.cise.plc.ast.ReturnStatement;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}

	private boolean assignmentCompatible(Type targetType, Type rhsType) {
		return (
				targetType == rhsType ||
				targetType==Type.INT && rhsType==Type.FLOAT
				|| targetType==Type.FLOAT && rhsType==Type.INT
				|| targetType==Type.INT && rhsType==Type.COLOR
				|| targetType==Type.COLOR && rhsType==Type.INT
				|| targetType==Type.IMAGE && rhsType == Type.INT
				|| targetType==Type.IMAGE && rhsType == Type.FLOAT
				|| targetType==Type.IMAGE && rhsType == Type.COLOR
				|| targetType==Type.IMAGE && rhsType == COLORFLOAT

		);
	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}

	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type leftType = (Type)binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type)binaryExpr.getRight().visit(this, arg);
		Type resultType = null;
		switch(op){
			case AND, OR -> {
				check(leftType == BOOLEAN && rightType == BOOLEAN, binaryExpr, "incompatible AND OR Type");
				resultType = BOOLEAN;
			}
			case EQUALS, NOT_EQUALS -> {
				check(leftType == rightType, binaryExpr, "incompatible EQUALS NOT_EQUALS Type");
				resultType = BOOLEAN;
			}
			case PLUS, MINUS -> {
				if(leftType == INT && rightType == INT) resultType = INT;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if(leftType == INT && rightType == FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == FLOAT && rightType == INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if(leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if(leftType == COLORFLOAT && rightType == COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == COLORFLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
			}
			case TIMES, DIV, MOD -> {
				if(leftType == INT && rightType == INT) resultType = INT;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if(leftType == INT && rightType == FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == FLOAT && rightType == INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if(leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if(leftType == COLORFLOAT && rightType == COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == COLORFLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
				else if(leftType == IMAGE && rightType == INT) resultType = IMAGE;
				else if(leftType == IMAGE && rightType == FLOAT) resultType = IMAGE;
				else if(leftType == INT && rightType == COLOR) {
					binaryExpr.getLeft().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if(leftType == COLOR && rightType == INT){
					binaryExpr.getRight().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if(leftType == FLOAT && rightType == COLOR){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
			}
			case LT, LE, GT, GE -> {
				if(leftType == INT && rightType == INT) resultType = BOOLEAN;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = BOOLEAN;
				else if(leftType == INT && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = BOOLEAN;
				}
				else if(leftType == FLOAT && rightType == INT)
				{
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = BOOLEAN;
				}
			}
			default -> {
				throw new UnsupportedOperationException("incompatible types for binaryExpr");
			}
		}
		binaryExpr.setType(resultType);
		return resultType;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String name = identExpr.getText();
		Declaration dec = symbolTable.lookup(name);

		//Check if ident is in the Symbol Table
		check(dec != null, identExpr, "undefined Identifier " + name);
		//Check if declaration is initialized
		check(dec.isInitialized(), identExpr, "using uninitialized variable");
		identExpr.setDec(dec); // Save declaration
		Type type = dec.getType();
		identExpr.setType(type); // Set the type
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {

		Type condition_type = (Type) conditionalExpr.getCondition().visit(this, arg);
		Type trueCase_type = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCase_type = (Type) conditionalExpr.getFalseCase().visit(this, arg);

		check(condition_type == BOOLEAN, conditionalExpr, "Condition must be of type BOOLEAN");
		check(trueCase_type == falseCase_type, conditionalExpr, "trueCase and FalseCase must be of the same type");

		conditionalExpr.setType(trueCase_type);
		return trueCase_type;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		Type widthType = (Type) dimension.getWidth().visit(this, arg);
		check(widthType == Type.INT, dimension.getWidth(), "only ints as dimension components");
		Type heightType = (Type) dimension.getHeight().visit(this, arg);
		check(heightType == Type.INT, dimension.getHeight(), "only ints as dimension components");

		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	//Lookup table for assignment statement
	Map<Pair<Type,Type>, Type> assignmentStatements = Map.of(
			new Pair<Type,Type>(IMAGE, INT), COLOR,
			new Pair<Type,Type>(IMAGE, FLOAT), COLORFLOAT,
			new Pair<Type,Type>(IMAGE, COLOR), COLOR,
			new Pair<Type,Type>(IMAGE, COLORFLOAT), COLORFLOAT
	);


	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		String name = assignmentStatement.getName();
		Declaration target = symbolTable.lookup(name);
		check(target != null, assignmentStatement, "undeclared variable" + name);
		assignmentStatement.setTargetDec(target);
		target.setInitialized(true);

		if(target.getType() != IMAGE)
		{
			Type assignmentType = (Type) assignmentStatement.getExpr().visit(this, arg);
			check(assignmentStatement.getSelector() == null, assignmentStatement, "There must not PixelSelector on left side");
			check(assignmentCompatible(target.getType(), assignmentType), assignmentStatement, "incompatible types in assignment");
			assignmentStatement.getExpr().setCoerceTo(target.getType());
		}
		else if(target.getType() == IMAGE)
		{
			if(assignmentStatement.getSelector() == null)
			{
				Type assignmentType = (Type) assignmentStatement.getExpr().visit(this, arg);
				check(assignmentCompatible(target.getType(), assignmentType), assignmentStatement, "incompatible types in assignment");

				Type resultType = assignmentStatements.get(new Pair<Type,Type>(target.getType(), assignmentType));
				assignmentStatement.getExpr().setCoerceTo(resultType);
			}
			else
			{

				check(assignmentStatement.getSelector().getX() instanceof IdentExpr
						&& assignmentStatement.getSelector().getY() instanceof IdentExpr, assignmentStatement, "Left variables must be an IdentExpr");

				//assignmentStatement.getSelector().getX().setType(INT);
				//assignmentStatement.getSelector().getY().setType(INT);

				NameDef x = new NameDef(assignmentStatement.getFirstToken(), "int", assignmentStatement.getSelector().getX().getText());
				NameDef y = new NameDef(assignmentStatement.getFirstToken(), "int", assignmentStatement.getSelector().getY().getText());
				VarDeclaration forX = new VarDeclaration(assignmentStatement.getFirstToken(), x, null, assignmentStatement.getSelector().getX());
				VarDeclaration forY = new VarDeclaration(assignmentStatement.getFirstToken(), y, null, assignmentStatement.getSelector().getY());

				forX.getExpr().setType(INT);
				forY.getExpr().setType(INT);
				check(symbolTable.insert(x.getName(), forX), assignmentStatement, "Could not insert left var in Pixel Selector");
				check(symbolTable.insert(y.getName(), forY), assignmentStatement, "Could not insert left var in Pixel Selector");
				forX.setInitialized(true);
				forY.setInitialized(true);

				Type assignmentType = (Type) assignmentStatement.getExpr().visit(this, arg);
				check(assignmentType == COLOR || assignmentType == COLORFLOAT || assignmentType == FLOAT || assignmentType == INT, assignmentStatement, "Invalid rhs type for assignment statement");
				assignmentStatement.getExpr().setCoerceTo(COLOR);

				symbolTable.remove(assignmentStatement.getSelector().getX().getText());
				symbolTable.remove(assignmentStatement.getSelector().getY().getText());
			}
		}
		return null;
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		String name = readStatement.getName();
		Declaration target = symbolTable.lookup(name);
		check(target != null, readStatement, "undeclared variable" + name);
		readStatement.setTargetDec(target);

		check(readStatement.getSelector() == null, readStatement, "A read statement cannot have a PixelSelector");
		Type rhsType = (Type) readStatement.getSource().visit(this, arg);
		Type lhsType = target.getType();

		check(rhsType == CONSOLE || rhsType == STRING, readStatement, "The right hand side type must be CONSOLE or STRING");

		if(rhsType == CONSOLE){
			readStatement.getSource().setCoerceTo(lhsType);
		}
		readStatement.getTargetDec().setInitialized(true);
		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		Type lhs = declaration.getNameDef().getType();
		Type rhs = null;
		if(declaration.getNameDef().getType() == IMAGE && declaration.getDim() != null){
			if(declaration.getDim().getWidth().getType() != INT || declaration.getDim().getHeight().getType() != INT){
				String left = declaration.getDim().getWidth().getText();
				String right = declaration.getDim().getHeight().getText();
				boolean leftInt = true;
				boolean rightInt = true;
				for (int i = 0; i < left.length(); i++) {
					if(!Character.isDigit(left.charAt(i))) {
						leftInt = false;
					}
				}
				for (int i = 0; i < right.length(); i++) {
					if(!Character.isDigit(right.charAt(i))) {
						rightInt = false;
					}
				}
				if(leftInt = false && rightInt == false){
					Declaration leftType = symbolTable.lookup(left);
					Declaration rightType = symbolTable.lookup(right);
					check(leftType != null || rightType != null, declaration, "Dimension both must be INT's");
					check(leftType.getType() == INT && rightType.getType() == INT, declaration, "Dimension both must be INT's");
				}
				declaration.getDim().getWidth().setType(INT);
				declaration.getDim().getHeight().setType(INT);
			}
		}else if(declaration.getNameDef().getType() == IMAGE){
			check(declaration.getDim() != null || declaration.getOp() != null, declaration, "Image needs an initializer or a dimension.");
		}

		if(declaration.getExpr() != null){
			rhs = (Type) declaration.getExpr().visit(this, arg);
			declaration.getExpr().setType(rhs);
			declaration.getNameDef().setInitialized(true);
		}
		check(symbolTable.insert(declaration.getNameDef().getName(), declaration.getNameDef()), declaration, "Error inserting lhs of decl/statements");

		if(declaration.getOp() != null){
			if(declaration.getOp().getKind() == Kind.ASSIGN){
				check(assignmentCompatible(lhs, rhs), declaration, "incompatible types in varDeclaration (ASSIGN)");
				declaration.getExpr().setCoerceTo(lhs);
			}else if(declaration.getOp().getKind() == Kind.LARROW){
				if(rhs == CONSOLE){
					declaration.getExpr().setCoerceTo(lhs);
					rhs = declaration.getExpr().getCoerceTo();
					check(assignmentCompatible(declaration.getNameDef().getType(), declaration.getExpr().getCoerceTo()), declaration, "incompatible types in varDeclaration (LARROW)");
				}else{
					declaration.getExpr().setCoerceTo(lhs);
					declaration.getExpr().setType(rhs);
					check(assignmentCompatible(declaration.getNameDef().getType(), declaration.getExpr().getCoerceTo()), declaration, "incompatible types in varDeclaration (LARROW)");
					declaration.getExpr().setCoerceTo(rhs);
				}
			}else if(declaration.getNameDef().getType() == IMAGE){
				check(declaration.getExpr().getType() == IMAGE || declaration.getDim() != null,  declaration, "incompatible image or dimension type");
				Type resultType = assignmentStatements.get(new Pair<Type,Type>(lhs, rhs));
				declaration.getExpr().setCoerceTo(resultType);
			}
		}
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		List<NameDef> params = program.getParams();
		NameDef programName = new NameDef(program.getFirstToken(), String.valueOf(program.getReturnType()).toLowerCase(Locale.ROOT), program.getName());
		//check(symbolTable.insert(programName.getName(), programName), program, "Program insert not valid");
		for (NameDef node : params) {
			check(symbolTable.insert(node.getName(), node), node, "Program insert not valid");
			node.setInitialized(true);
		}

		//Save root of AST so return type can be accessed in return statements
		root = program;

		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {

			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		String name = nameDef.getName();
		check(symbolTable.insert(name, nameDef), nameDef, "NameDef insert failed");
		return null;
	}
	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		String name = nameDefWithDim.getName();
		Type width = (Type) nameDefWithDim.getDim().getWidth().getType();
		Type height = (Type) nameDefWithDim.getDim().getHeight().getType();
		check(width == INT && height == INT, nameDefWithDim, "Dimension types are not INT's");
		check(symbolTable.insert(name, nameDefWithDim), nameDefWithDim, "NameDefWithDim insert failed");
		return null;
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}

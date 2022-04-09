package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser {
    ILexer lexer;
    IToken token;
    int counter = 0;

    public Parser(String input) {
        this.lexer = CompilerComponentFactory.getLexer(input);
    }

    Program Program() throws PLCException{
        Types.Type type; //IDK if types work
        List<NameDef> params = new ArrayList<>();
        List<ASTNode> decsAndStatements = new ArrayList<>();
        if(token.getKind() == IToken.Kind.TYPE || token.getKind() == IToken.Kind.KW_VOID){
            type = Types.Type.toType(token.getText());
            token = lexer.next();
            if(token.getKind() == IToken.Kind.IDENT){
                String name = token.getText();
                token = lexer.next();
                match(IToken.Kind.LPAREN);
                if(token.getKind() == IToken.Kind.RPAREN){
                    match(IToken.Kind.RPAREN);
                }else{
                    params.add(NameDef());
                    while(token.getKind() == IToken.Kind.COMMA){
                        token = lexer.next();
                        params.add(NameDef());
                    }
                    match(IToken.Kind.RPAREN);
                }
                while(token.getKind() == IToken.Kind.TYPE || token.getKind() == IToken.Kind.IDENT || token.getKind() == IToken.Kind.KW_WRITE
                        || token.getKind() == IToken.Kind.RETURN){
                    counter++;
                    if(token.getKind() == IToken.Kind.TYPE){
                        decsAndStatements.add(Declaration());
                        match(IToken.Kind.SEMI);
                    }else{
                        decsAndStatements.add(Statement());
                        match(IToken.Kind.SEMI);
                    }
                }
                if(token.getKind() == IToken.Kind.EOF){
                    return new Program(token, type, name, params, decsAndStatements);
                }else{
                    throw new SyntaxException("Not a proper Program.", token.getSourceLocation());
                }
            }else{
                throw new SyntaxException("Token Not Found for Program() ", token.getSourceLocation());
            }
        }else{
            throw new SyntaxException("Token Not Found for Program() ", token.getSourceLocation());
        }
    }
    NameDef NameDef() throws PLCException{
        Dimension dim;
        if(token.getKind() == IToken.Kind.TYPE){
            String type = token.getText();
            token = lexer.next();
            if(token.getKind() == IToken.Kind.IDENT){
                String name = token.getText();
                token = lexer.next();
                return new NameDef(token, type, name);
            }else if(token.getKind() == IToken.Kind.LSQUARE) {
                dim = Dimension();
                String name = token.getText();
                match(IToken.Kind.IDENT);
                return new NameDefWithDim(token, type, name, dim);
            }else{
                throw new SyntaxException("Token Not Found for NameDef() ", token.getSourceLocation());
            }
        }else{
            throw new SyntaxException("Token Not Found for NameDef() ", token.getSourceLocation());
        }
    }
    Declaration Declaration() throws PLCException{
        NameDef left;
        Expr exp = null;
        left = NameDef();
        IToken op = null;
        if(token.getKind() == IToken.Kind.ASSIGN || token.getKind() == IToken.Kind.LARROW){
            op = token;
            token = lexer.next();
            exp = Expr();
            return new VarDeclaration(token, left, op, exp);
        }
        return new VarDeclaration(token, left, op, exp);
    }

    Expr Expr() throws PLCException {
        Expr exp;
        if(token.getKind() == IToken.Kind.KW_IF){
            exp = ConditionalExpr();
        }else {
            exp = LogicalOrExpr();
        }
        return exp;
    }

    Expr ConditionalExpr() throws PLCException {
        Expr exp0;
        Expr exp1;
        Expr exp2;
        if(token.getKind() == IToken.Kind.KW_IF){
            token = lexer.next();
            match(IToken.Kind.LPAREN);
            exp0 = Expr();
            match(IToken.Kind.RPAREN);
            exp1 = Expr();
            match(IToken.Kind.KW_ELSE);
            exp2 = Expr();
            match(IToken.Kind.KW_FI);
        }else{
            throw new SyntaxException("Token Not Found for ConditionalExpr() ", token.getSourceLocation());
        }
        return new ConditionalExpr(token,exp0, exp1,exp2);
    }

    Expr LogicalOrExpr() throws PLCException {
        Expr left;
        Expr right;
        left = LogicalAndExpr();
        while(token.getKind() == IToken.Kind.OR){
            IToken op = token;
            token = lexer.next();
            right = LogicalAndExpr();
            left = new BinaryExpr(token,left, op, right);
        }
        return left;
    }

    Expr LogicalAndExpr() throws PLCException {
        Expr left;
        Expr right;
        left = ComparisonExpr();
        while(token.getKind() == IToken.Kind.AND){
            IToken op = token;
            token = lexer.next();
            right = ComparisonExpr();
            left = new BinaryExpr(token,left, op, right);
        }
        return left;
    }

    Expr ComparisonExpr() throws PLCException {
        Expr left;
        Expr right;
        left = AdditiveExpr();
        while(token.getKind() == IToken.Kind.LT || token.getKind() == IToken.Kind.GT || token.getKind() == IToken.Kind.EQUALS || token.getKind() == IToken.Kind.NOT_EQUALS
                || token.getKind() == IToken.Kind.LE || token.getKind() == IToken.Kind.GE){
            IToken op = token;
            token = lexer.next();
            right = AdditiveExpr();
            left = new BinaryExpr(token,left, op, right);
        }
        return left;
    }

    Expr AdditiveExpr() throws PLCException {
        Expr left;
        Expr right;
        left = MultiplicativeExpr();
        while(token.getKind() == IToken.Kind.PLUS || token.getKind() == IToken.Kind.MINUS){
            IToken op = token;
            token = lexer.next();
            right = MultiplicativeExpr();
            left = new BinaryExpr(token,left, op, right);
        }
        return left;
    }

    Expr MultiplicativeExpr() throws PLCException {
        Expr left;
        Expr right;
        left = UnaryExpr();
        while(token.getKind() == IToken.Kind.TIMES || token.getKind() == IToken.Kind.DIV || token.getKind() == IToken.Kind.MOD){
            IToken op = token;
            token = lexer.next();
            right = UnaryExpr();
            left = new BinaryExpr(token,left, op, right);
        }
        return left;
    }

    Expr UnaryExpr() throws PLCException{
        Expr exp;
        if(token.getKind() == IToken.Kind.BANG || token.getKind() == IToken.Kind.MINUS || token.getKind() == IToken.Kind.COLOR_OP || token.getKind() == IToken.Kind.IMAGE_OP){
            IToken op = token;
            token = lexer.next();
            exp = UnaryExpr();
            return new UnaryExpr(token, op, exp);
        }else if(token.getKind() == IToken.Kind.BOOLEAN_LIT ||token.getKind() == IToken.Kind.STRING_LIT || token.getKind() == IToken.Kind.INT_LIT
                || token.getKind() == IToken.Kind.FLOAT_LIT || token.getKind() == IToken.Kind.IDENT || token.getKind() == IToken.Kind.LPAREN
                || token.getKind() == IToken.Kind.COLOR_CONST || token.getKind() == IToken.Kind.LANGLE || token.getKind() == IToken.Kind.KW_CONSOLE) {
            exp = UnaryExprPostfix();
        }else{
            throw new SyntaxException("Not a valid condition for UnaryExpr() ", token.getSourceLocation());
        }
        return exp;
    }

    Expr UnaryExprPostfix() throws PLCException{
        Expr exp;
        PixelSelector pix;
        if(token.getKind() == IToken.Kind.BOOLEAN_LIT ||token.getKind() == IToken.Kind.STRING_LIT || token.getKind() == IToken.Kind.INT_LIT
                || token.getKind() == IToken.Kind.FLOAT_LIT || token.getKind() == IToken.Kind.IDENT || token.getKind() == IToken.Kind.LPAREN
                || token.getKind() == IToken.Kind.COLOR_CONST || token.getKind() == IToken.Kind.LANGLE || token.getKind() == IToken.Kind.KW_CONSOLE){
            exp = PrimaryExpr();
            if(token.getKind() == IToken.Kind.LSQUARE){
                pix = PixelSelector();
                return new UnaryExprPostfix(token, exp, pix);
            }
        }else{
            throw new SyntaxException("Not a valid condition for UnaryExpPostFix() ", token.getSourceLocation());
        }
        return exp;
    }

    Expr PrimaryExpr() throws PLCException {
        Expr exp;
        Expr exp1;
        Expr exp2;
        if(token.getKind() == IToken.Kind.BOOLEAN_LIT) {
            exp = new BooleanLitExpr(token);
            token = lexer.next();
        }else if(token.getKind() == IToken.Kind.STRING_LIT) {
            exp = new StringLitExpr(token);
            token = lexer.next();
        }else if(token.getKind() == IToken.Kind.INT_LIT) {
            exp = new IntLitExpr(token);
            token = lexer.next();
        }else if(token.getKind() == IToken.Kind.FLOAT_LIT) {
            exp = new FloatLitExpr(token);
            token = lexer.next();
        }else if(token.getKind() == IToken.Kind.IDENT) {
            exp = new IdentExpr(token);
            token = lexer.next();
        }else if (token.getKind() == IToken.Kind.LPAREN) {
            token = lexer.next();
            exp = Expr();
            match(IToken.Kind.RPAREN);
        }else if(token.getKind() == IToken.Kind.COLOR_CONST) {
            exp = new ColorConstExpr(token);
            token = lexer.next();
        }else if(token.getKind() == IToken.Kind.LANGLE) {
            token = lexer.next();
            exp = Expr();
            match(IToken.Kind.COMMA);
            exp1 = Expr();
            match(IToken.Kind.COMMA);
            exp2 = Expr();
            match(IToken.Kind.RANGLE);
            return new ColorExpr(token, exp, exp1, exp2);
        }else if(token.getKind() == IToken.Kind.KW_CONSOLE) {
            exp = new ConsoleExpr(token);
            token = lexer.next();
        }else {
            throw new SyntaxException("Token Not Found for PrimaryExpr() ", token.getSourceLocation());
        }
        return exp;
    }

    PixelSelector PixelSelector() throws PLCException {
        Expr left;
        Expr right;
        if(token.getKind() == IToken.Kind.LSQUARE){
            token = lexer.next();
            left = Expr();
            match(IToken.Kind.COMMA);
            right = Expr();
            match(IToken.Kind.RSQUARE);
        }else{
            throw new SyntaxException("Token Not Found for PixelSelector() ", token.getSourceLocation());
        }
        return new PixelSelector(token, left, right);
    }

    Dimension Dimension() throws PLCException{
        Expr left;
        Expr right;
        if(token.getKind() == IToken.Kind.LSQUARE){
            token = lexer.next();
            left = Expr();
            match(IToken.Kind.COMMA);
            right = Expr();
            match(IToken.Kind.RSQUARE);
        }else{
            throw new SyntaxException("Token Not Found for Dimension() ", token.getSourceLocation());
        }
        return new Dimension(token, left, right);
    }

    Statement Statement() throws PLCException {
        Statement temp = null;
        Expr left;
        Expr right;
        PixelSelector pix = null;
        if(token.getKind() == IToken.Kind.IDENT){
            String name = token.getText();
            token = lexer.next();
            if(token.getKind() == IToken.Kind.LSQUARE){
                pix = PixelSelector();
                if(token.getKind() == IToken.Kind.ASSIGN){
                    token = lexer.next();
                    left = Expr();
                    return new AssignmentStatement(token, name, pix, left);
                }else if(token.getKind() == IToken.Kind.LARROW){
                    token = lexer.next();
                    left = Expr();
                    return new ReadStatement(token, name, pix, left);
                }
            }
            if(token.getKind() == IToken.Kind.ASSIGN){
                token = lexer.next();
                left = Expr();
                return new AssignmentStatement(token, name, pix, left);
            }else if(token.getKind() == IToken.Kind.LARROW){
                token = lexer.next();
                left = Expr();
                return new ReadStatement(token, name, pix, left);
            }
        }else if(token.getKind() == IToken.Kind.KW_WRITE){
            token = lexer.next();
            left = Expr();
            match(IToken.Kind.RARROW);
            right = Expr();
            return new WriteStatement(token, left, right);
        }else if(token.getKind() == IToken.Kind.RETURN){
            token = lexer.next();
            left = Expr();
            return new ReturnStatement(token,left);
        }else{
            throw new SyntaxException("Token Not Found for Statement() ", token.getSourceLocation());
        }
        return temp;
    }

    void match(IToken.Kind c) throws PLCException {
        if(token.getKind() == c){
            token = lexer.next();
        }else{
            throw new SyntaxException("Current Token and Desired Token Kind are different. ", token.getSourceLocation());
        }
    }

    @Override
    public ASTNode parse() throws PLCException {
        token = lexer.next();
        return Program();
    }
}

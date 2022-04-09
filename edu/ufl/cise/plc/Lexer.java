package edu.ufl.cise.plc;
import java.util.ArrayList;

public class Lexer implements ILexer {
    // ARRAY CONTAINS TOKENS
    ArrayList<Token> tokenList = new ArrayList<>();
    IToken.Kind kind;
    int token_index = 0;
    String tokenName;
    State state = State.START;
    StringBuilder string_lit_text = new StringBuilder("");

    // Initial position of the current token
    int start_pos = 0;
    int curr_pos = 0;
    int column = 0;
    int curr_line = 0;
    boolean multiString = false;
    int multiLineCounter = 0;

    private enum State {
        START,
        IN_IDENT,
        HAVE_ZERO,
        HAVE_DOT,
        IN_FLOAT,
        IN_NUM,
        HAVE_EQ,
        HAVE_MINUS,
        IN_LESS_THAN,
        IN_GREATER_THAN,
        IN_EXCLAMATION,
        IN_COMMENT,
        IN_STRING_LIT,
        IN_ESCAPE_SEQUENCE
    }
    public Lexer(String input) {
        // NEW CHAR ARRAY INPUT IN CHAR ARRAY INCLUDING FOR LOOP TO CONVERT
        String temp = input + " ";
        char[] newInput = new char[temp.length()];

        for(int i = 0; i < temp.length(); i++){
            newInput[i] = temp.charAt(i);
        }
        // Loop over characters, increment pos with each iteration
        while (true) {
            // Get current char
            if (curr_pos < newInput.length) {
                char curr_char = newInput[curr_pos];
                switch (state) {
                    case START -> {
                        start_pos = curr_pos;
                        switch (curr_char) {
                            case ' ', '\t', '\r', '\n' -> {
                                curr_pos++;
                                if(curr_char == '\n'){
                                    column = 0;
                                    curr_line++;
                                }else{
                                    column++;
                                }
                            }
                            case '+' -> {
                                tokenName = "+";
                                kind = IToken.Kind.PLUS;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '-' -> {
                                state = State.HAVE_MINUS;
                                curr_pos++;
                                column++;
                            }
                            case '*' -> {
                                tokenName = "*";
                                kind = IToken.Kind.TIMES;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '(' -> {
                                tokenName = "(";
                                kind = IToken.Kind.LPAREN;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case ')' -> {
                                tokenName = ")";
                                kind = IToken.Kind.RPAREN;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '[' -> {
                                tokenName = "[";
                                kind = IToken.Kind.LSQUARE;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case ']' -> {
                                tokenName = "]";
                                kind = IToken.Kind.RSQUARE;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '/' -> {
                                tokenName = "/";
                                kind = IToken.Kind.DIV;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '%' -> {
                                tokenName = "%";
                                kind = IToken.Kind.MOD;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '&' -> {
                                tokenName = "&";
                                kind = IToken.Kind.AND;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '|' -> {
                                tokenName = "|";
                                kind = IToken.Kind.OR;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case ',' -> {
                                tokenName = ",";
                                kind = IToken.Kind.COMMA;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case ';' -> {
                                tokenName = ";";
                                kind = IToken.Kind.SEMI;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '^' -> {
                                tokenName = "^";
                                kind = IToken.Kind.RETURN;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                            }
                            case '=' -> {
                                kind = IToken.Kind.ASSIGN;
                                state = State.HAVE_EQ;
                                curr_pos++;
                                column++;
                            }
                            case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                                    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                                    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                                    'w', 'x', 'y', 'z', '_', '$' -> {
                                kind = IToken.Kind.IDENT;
                                state = State.IN_IDENT;
                                curr_pos++;
                                column++;
                            }
                            case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                state = State.IN_NUM;
                                curr_pos++;
                                column++;
                            }
                            case '0' -> {
                                state = State.HAVE_ZERO;
                                curr_pos++;
                                column++;
                            }
                            case '<' -> {
                                state = State.IN_LESS_THAN;
                                curr_pos++;
                                column++;
                            }
                            case '>' -> {
                                state = State.IN_GREATER_THAN;
                                curr_pos++;
                                column++;
                            }
                            case '!' -> {
                                state = State.IN_EXCLAMATION;
                                curr_pos++;
                                column++;
                            }
                            case '#' -> {
                                state =  State.IN_COMMENT;
                                curr_pos++;
                                column++;
                            }
                            case '"' -> {
                                string_lit_text.append("\"");
                                state = State.IN_STRING_LIT;
                                curr_pos++;
                                column++;
                            }
                            default -> {
                                if(curr_char == '.')
                                {
                                    tokenName = ".";
                                    kind = IToken.Kind.ERROR;
                                    tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                    curr_pos++;
                                    column++;
                                }
                                else
                                {
                                    tokenName = "NRT";
                                    kind = IToken.Kind.ERROR;
                                    tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                    curr_pos++;
                                    column++;
                                }
                                return;
                            }

                        }
                    }
                    case IN_IDENT -> {
                        switch (curr_char) {
                            case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                                    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                                    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                                    'w', 'x', 'y', 'z', '_', '$', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                curr_pos++;
                                column++;
                                kind = IToken.Kind.IDENT;

                            }
                            default -> {
                                // CREATE TOKEN
                                tokenName = input.substring(start_pos, curr_pos);
                                switch (tokenName) {
                                    case "string", "int", "float", "boolean", "color", "image" -> {
                                        kind = IToken.Kind.TYPE;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "void" -> {
                                        kind = IToken.Kind.KW_VOID;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "getWidth", "getHeight" -> {
                                        kind = IToken.Kind.IMAGE_OP;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "getRed", "getGreen", "getBlue" -> {
                                        kind = IToken.Kind.COLOR_OP;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "BLACK", "BLUE", "CYAN", "DARK_GRAY", "GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA",
                                            "ORANGE", "PINK", "RED", "WHITE", "YELLOW" -> {
                                        kind = IToken.Kind.COLOR_CONST;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "true", "false" -> {
                                        kind = IToken.Kind.BOOLEAN_LIT;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "if" -> {
                                        kind = IToken.Kind.KW_IF;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "else" -> {
                                        kind = IToken.Kind.KW_ELSE;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "fi" -> {
                                        kind = IToken.Kind.KW_FI;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "write" -> {
                                        kind = IToken.Kind.KW_WRITE;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    case "console" -> {
                                        kind = IToken.Kind.KW_CONSOLE;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                    default -> {
                                        kind = IToken.Kind.IDENT;
                                        tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                        state = State.START;
                                    }
                                }
                            }
                        }
                    }
                    case HAVE_ZERO -> {
                        if (curr_char == '.') {
                            state = State.HAVE_DOT;
                            curr_pos++;
                            column++;
                        } else {
                            // CREATE TOKEN
                            tokenName = "0";
                            kind = IToken.Kind.INT_LIT;
                            tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                            state = State.START;
                        }

                    }
                    case HAVE_DOT -> {
                        switch (curr_char) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                state = State.IN_FLOAT;
                                curr_pos++;
                                column++;
                            }
                            default ->{
                                tokenName = ".";
                                kind = IToken.Kind.ERROR;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                curr_pos++;
                                column++;
                                return;
                            }
                        }

                    }
                    case IN_FLOAT -> {
                        switch (curr_char) {

                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                curr_pos++;
                                column++;
                            }
                            default -> {
                                // CREATE TOKEN
                                tokenName = input.substring(start_pos, curr_pos);
                                kind = IToken.Kind.FLOAT_LIT;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                state = State.START;
                            }
                        }
                    }
                    case IN_NUM -> {
                        switch (curr_char) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                //Increase pos to get next char
                                curr_pos++;
                                column++;
                            }
                            case '.' -> {
                                state = State.HAVE_DOT;
                                curr_pos++;
                                column++;
                            }
                            default -> {
                                // CREATE TOKEN
                                tokenName = input.substring(start_pos, curr_pos);
                                kind = IToken.Kind.INT_LIT;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                state = State.START;
                            }
                        }
                    }
                    case HAVE_EQ -> {
                        if (curr_char == '=') {// CREATE TOKEN
                            tokenName = input.substring(start_pos, curr_pos + 1);
                            kind = IToken.Kind.EQUALS;
                            tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                            state = State.START;
                            curr_pos++;
                            column++;
                        } else {
                            tokenName = "=";
                            kind = IToken.Kind.ASSIGN;
                            tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                            state = State.START;
                        }
                    }
                    case HAVE_MINUS -> {
                        if (curr_char == '>') {
                            tokenName = input.substring(start_pos, curr_pos + 1);
                            kind = IToken.Kind.RARROW;
                            tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                            state = State.START;
                            curr_pos++;
                            column++;
                        } else {
                            tokenName = "-";
                            kind = IToken.Kind.MINUS;
                            tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                            state = State.START;
                        }
                    }
                    case IN_LESS_THAN -> {
                        switch (curr_char) {
                            case '<' -> {
                                tokenName = input.substring(start_pos, curr_pos + 1);
                                kind = IToken.Kind.LANGLE;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                                state = State.START;
                                curr_pos++;
                                column++;
                            }
                            case '=' -> {
                                tokenName = input.substring(start_pos, curr_pos + 1);
                                kind = IToken.Kind.LE;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                                state = State.START;
                                curr_pos++;
                                column++;
                            }
                            case '-' -> {
                                tokenName = input.substring(start_pos, curr_pos + 1);
                                kind = IToken.Kind.LARROW;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                                state = State.START;
                                curr_pos++;
                                column++;
                            }
                            default -> {
                                tokenName = "<";
                                kind = IToken.Kind.LT;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                state = State.START;
                            }
                        }
                    }
                    case IN_GREATER_THAN -> {
                        switch (curr_char) {
                            case '>' -> {
                                tokenName = input.substring(start_pos, curr_pos + 1);
                                kind = IToken.Kind.RANGLE;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                                state = State.START;
                                curr_pos++;
                                column++;
                            }
                            case '=' -> {
                                tokenName = input.substring(start_pos, curr_pos + 1);
                                kind = IToken.Kind.GE;
                                tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                                state = State.START;
                                curr_pos++;
                                column++;
                            }
                            default -> {
                                tokenName = ">";
                                kind = IToken.Kind.GT;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                                state = State.START;
                            }

                        }
                    }
                    case IN_EXCLAMATION -> {
                        if (curr_char == '=') {
                            tokenName = input.substring(start_pos, curr_pos + 1);
                            kind = IToken.Kind.NOT_EQUALS;
                            tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, Math.max(0, (column + 1) - tokenName.length())));
                            state = State.START;
                            curr_pos++;
                            column++;
                        } else {
                            tokenName = "!";
                            kind = IToken.Kind.BANG;
                            tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column - tokenName.length())));
                            state = State.START;
                        }
                    }
                    case IN_COMMENT -> {
                        if(curr_pos + 1 < newInput.length)
                        {
                            char next_char = newInput[curr_pos + 1];
                            if (curr_char == '\r' && next_char == '\n' || curr_char == '\n') {
                                state = State.START;
                            }
                            else {
                                column++;
                                curr_pos++;
                            }
                        }
                        else
                            state = State.START;
                    }
                    case IN_STRING_LIT -> {
                        switch (curr_char) {
                            case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                                    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                                    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                                    'w', 'x', 'y', 'z', '&', '_', '$', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                    ' ', '!', '#', '@', '%', '`', '^', '*', '(', ')', '-', '+', '=', '|', ';', ':', '?',
                                    '>', '.', '<', ',', '~', '{', '}', '[', ']', '\'', '/', '\t', '\b', '\f', '\r' -> {

                                if(curr_char == ' ' && (curr_pos + 1) >= newInput.length){
                                    tokenName = "NoEndQuote";
                                    kind = IToken.Kind.ERROR;
                                    tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                    curr_pos++;
                                    column++;
                                    return;
                                }
                                string_lit_text.append(curr_char);
                                curr_pos++;
                                column++;
                            }
                            case '\\' ->{
                                state = State.IN_ESCAPE_SEQUENCE;
                                curr_pos++;
                                column++;
                            }
                            case '"' -> {
                                string_lit_text.append("\"");
                                tokenName = String.valueOf(string_lit_text);
                                string_lit_text.setLength(0);
                                kind = IToken.Kind.STRING_LIT;

                                if(!multiString){
                                    tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line, start_pos));
                                }else{
                                    tokenList.add(new Token(start_pos, tokenName.length(), tokenName, kind, curr_line - multiLineCounter, start_pos));
                                }
                                state = State.START;
                                curr_pos++;
                                column++;
                            }
                            case '\n' -> {
                                string_lit_text.append("\n");
                                curr_pos++;
                                column = 0;
                                curr_line++;
                                multiString = true;
                                multiLineCounter++;
                            }
                            default -> {
                                tokenName = "NoEndQuote";
                                kind = IToken.Kind.ERROR;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                                return;
                            }
                        }
                    }
                    case IN_ESCAPE_SEQUENCE -> {
                        switch(curr_char)
                        {
                            case 'b','t','n','f','r','\'', '\"', '\\', ' ' -> {
                                if(curr_char == '\"' || curr_char == '\'' || curr_char == '\\'){
                                    string_lit_text.append("\\");
                                    string_lit_text.append(curr_char);
                                    curr_pos++;
                                    column++;
                                }else{
                                    string_lit_text.append("\\");
                                }
                                state = State.IN_STRING_LIT;
                            }
                            default -> {
                                tokenName = "IES";
                                kind = IToken.Kind.ERROR;
                                tokenList.add(new Token(start_pos, 1, tokenName, kind, curr_line, Math.max(0, column)));
                                curr_pos++;
                                column++;
                                return;
                            }
                        }
                    }
                }
            }
            else{
                tokenList.add(new Token(start_pos, 3, "EOF", IToken.Kind.EOF, curr_line, 0));
                break;
            }
        }
    }

    @Override
    public IToken next() throws edu.ufl.cise.plc.LexicalException {
        IToken token;
        try
        {
            token = tokenList.get(token_index);
            token_index++;
        }
        catch(Exception EOF_Reached)
        {
            throw new LexicalException("EOF reached! There are no more tokens", curr_line, column);
        }

        if(token.getKind() == IToken.Kind.ERROR)
        {
            if(token.getText() == "NRT"){
                throw new LexicalException("This Token is not Valid.", token.getSourceLocation());
            }else if(token.getText() == "IES"){
                throw new LexicalException("Illegal escape character in string literal", token.getSourceLocation());
            }if(token.getText() == "."){
            throw new LexicalException("There must be a number before and after a dot.", token.getSourceLocation());
        }else if(token.getText() == "NoEndQuote"){
            throw new LexicalException("String literal needs an ending quote", token.getSourceLocation());
        }
        }
        if(token.getKind() == IToken.Kind.INT_LIT)
        {
            try
            {
                Integer.parseInt(token.getText());
            }
            catch (Exception OUT_OF_BOUND_INT)
            {
                throw new LexicalException("The size of the integer is out of bounds", curr_line, column);
            }
        }
        else if(token.getKind() == IToken.Kind.FLOAT_LIT)
        {
            try
            {
                Float.parseFloat(token.getText());
            }
            catch (Exception OUT_OF_BOUND_FLOAT)
            {
                throw new LexicalException("The size of the float is out of bounds", curr_line, column);
            }
        }
        return token;
    }

    @Override
    public IToken peek() throws edu.ufl.cise.plc.LexicalException {
        try
        {
            return tokenList.get(token_index);
        }
        catch (Exception e)
        {
            throw  new LexicalException("Index is greater than number of tokens available");
        }
    }

}
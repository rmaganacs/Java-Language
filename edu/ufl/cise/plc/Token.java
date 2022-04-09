package edu.ufl.cise.plc;
import java.util.Objects;

public class Token implements IToken{
    final int pos;
    final int length;
    final String token;
    final Kind kind;
    final SourceLocation source;

    public Token(int pos, int length, String token, Kind kind, int line, int column) {
        this.pos = pos;
        this.length = length;
        this.token = token;
        this.kind = kind;
        this.source = new SourceLocation(line, column);
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return token;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return source;
    }

    @Override
    public int getIntValue() {
        return Integer.parseInt(token);
    }

    @Override
    public float getFloatValue() {
        return Float.parseFloat(token);
    }

    @Override
    public boolean getBooleanValue() {
        return Objects.equals(token, "true");
    }

    @Override
    public String getStringValue() {
        String temp = token;
        StringBuilder sLiteral = new StringBuilder("");

        for(int i = 1; i < temp.length(); i++){
            if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == '\\'){
                sLiteral.append('\\');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == 'b'){
                sLiteral.append('\b');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == 'n'){
                sLiteral.append('\n');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == 't'){
                sLiteral.append('\t');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == 'f'){
                sLiteral.append('\f');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == 'r'){
                sLiteral.append('\r');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == '\''){
                sLiteral.append('\'');
                i++;
                continue;
            }else if(temp.charAt(i) == '\\' && temp.charAt(i + 1) == '\"'){
                sLiteral.append('\"');
                i++;
                continue;
            }if(temp.charAt(i) == '\"') {
                continue;
            }else {
                sLiteral.append(temp.charAt(i));
            }
        }
        temp = String.valueOf(sLiteral);
        return temp;
    }

}

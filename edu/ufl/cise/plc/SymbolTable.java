package edu.ufl.cise.plc;
import edu.ufl.cise.plc.ast.Declaration;

import java.util.HashMap;

public class SymbolTable {

    //Store entries of the symbol table
    HashMap<String, Declaration> entries = new HashMap<>();

    //Returns true if the entry was not on the table before, false otherwise
    public boolean insert(String name, Declaration dec)
    {
        return (entries.putIfAbsent(name, dec) == null);
    }

    //Return declaration of a variable, if the name is not on the table returns null
    public Declaration lookup(String name)
    {
        return entries.get(name);
    }

    public void remove(String name)
    {
        entries.remove(name);
    }

}

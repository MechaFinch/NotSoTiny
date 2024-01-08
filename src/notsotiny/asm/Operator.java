package notsotiny.asm;

import java.util.HashMap;
import java.util.Map;

import asmlib.lex.symbols.SpecialCharacterSymbol;

/**
 * Represents an expression-forming operator, usually +
 * 
 * @author Mechafinch
 */
public enum Operator {
    ADD         ('+'),
    SUBTRACT    ('-'),
    MULTIPLY    ('*'),
    DIVIDE      ('/'),
    AND         ('&'),
    OR          ('|'),
    XOR         ('^'),
    NOT         ('~'),
    LEFT        ('<'),
    RIGHT       ('>');
    
    // character for conversion from a SpecialCharacter symbol
    private char c;
    
    private static final Map<Character, Operator> characterMap = new HashMap<>();
    
    static {
        for(Operator op : values()) {
            characterMap.put(op.c, op);
        }
    }
    
    private Operator(char c) {
        this.c = c;
    }
    
    /**
     * Converts a SpecialCharacterSymbol to an operator
     * 
     * @param sc
     * @return
     */
    public static Operator convertSpecialCharacter(SpecialCharacterSymbol sc) {
        return characterMap.get(sc.character());
    }
    
    public char character() { return this.c; }
}

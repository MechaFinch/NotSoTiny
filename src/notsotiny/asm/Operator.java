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
    DIVIDE      ('/');
    
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
    
    public Operator convertSpecialCharacter(SpecialCharacterSymbol sc) {
        return characterMap.get(sc.character());
    }
}

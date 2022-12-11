package notsotiny.asm;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import asmlib.lex.symbols.ConstantSymbol;
import asmlib.lex.symbols.ExpressionSymbol;
import asmlib.lex.symbols.NameSymbol;
import asmlib.lex.symbols.SpecialCharacterSymbol;
import asmlib.lex.symbols.Symbol;
import notsotiny.asm.resolution.ResolvableConstant;
import notsotiny.asm.resolution.ResolvableExpression;
import notsotiny.asm.resolution.ResolvableValue;

/**
 * Class containing methods for parsing memory expressions
 * Allows better organization and saner method names
 * 
 * @author Mechafinch
 */
public class ConstantExpressionParser {
    
    private static Logger LOG = Logger.getLogger(Assembler.class.getName());
    
    /**
     * Parses constants
     * 
     * Applies the following grammar
     * Note that subExpr and Expression don't create left-recursion because Expression is an object demarcating parenthesis
     * Note that the plus and minus prefixes of subExpr are positive/negative, while those of subExprRight are add/subtract
     * Expression   -> subExpr
     * 
     * subExpr      -> Expression [subExprRight]
     *              -> '+' subExpr [subExprRight]
     *              -> '-' subExpr [subExprRight]
     *              -> '~' subExpr [subExprRight]
     *              -> value [subExprRight]
     *
     * subExprRight -> '+' subExpr [subExprRight]
     *              -> '-' subExpr [subExprRight]
     *              -> '*' subExpr [subExprRight]
     *              -> '/' subExpr [subExprRight]
     *              -> '&' subExpr [subExprRight]
     *              -> '|' subExpr [subExprRight]
     *              -> '^' subExpr [subExprRight]
     * 
     * value        -> '$' Constant
     *              -> '@' Constant
     *              -> Constant
     *              -> Name
     * 
     * @param symbols
     * @return
     */
    public static ResolvableValue parse(List<Symbol> symbols) {
        ResolvableValue rv = parseSubExpression(new LinkedList<Symbol>(symbols));
        
        if(rv instanceof ResolvableExpression re) {
            return re.minimize();
        } else return rv;
    }
    
    /**
     * Parses the subExpr of the grammar
     * 
     * subExpr      -> Expression [subExprRight]
     *              -> '~' subExpr [subExprRight]
     *              -> value [subExprRight]
     * 
     * @param queue
     * @param ignoreExtraOperators
     * @return
     */
    private static ResolvableValue parseSubExpression(LinkedList<Symbol> queue) {
        Symbol nextSymbol = queue.peek();
        if(nextSymbol == null) {
            LOG.info("Expression parse replaced missing parameter with zero");
            return new ResolvableConstant(0);
        }
        
        ResolvableValue val = null;
        
        if(nextSymbol instanceof ExpressionSymbol es) {
            // pre-grouped expression. thanks lexer
            queue.poll();
            
            val = parseSubExpression(new LinkedList<Symbol>(es.symbols()));
        } else if(nextSymbol instanceof SpecialCharacterSymbol scs) {
            // some special character
            if(scs.character() == '-') {
                // negative value
                queue.poll();
                ResolvableValue v = parseSubExpression(queue);
                val = new ResolvableExpression(new ResolvableConstant(0), v, Operator.SUBTRACT); // negate
            } else if(scs.character() == '~') {
                // complemented value
                queue.poll();
                ResolvableValue v = parseSubExpression(queue);
                val = new ResolvableExpression(new ResolvableConstant(0), v, Operator.NOT);
            } else if(scs.character() == '+') {
                // positive value (just consume)
                queue.poll();
                val = parseSubExpression(queue);
            } else {
                // defer to parseValue
                val = parseValue(queue);
            }
        } else {
            // defer
            val = parseValue(queue);
        }
        
        if(queue.peek() != null) {
            return parseSubExpressionRight(queue, val);
        } else {
            return val;
        }
    }
    
    /**
     * Parses the subExprRight of the grammar
     * In order to return both operator and operand, the left side is an argument
     * 
     * subExprRight -> '+' subExpr [subExprRight]
     *              -> '-' subExpr [subExprRight]
     *              -> '*' subExpr [subExprRight]
     *              -> '/' subExpr [subExprRight]
     *              -> '&' subExpr [subExprRight]
     *              -> '|' subExpr [subExprRight]
     *              -> '^' subExpr [subExprRight]
     *              
     * @param queue
     * @param left
     * @param ignoreExtraOperators
     * @return
     */
    private static ResolvableExpression parseSubExpressionRight(LinkedList<Symbol> queue, ResolvableValue left) {
        Symbol s = queue.poll();
        
        if(s instanceof SpecialCharacterSymbol scs) {
            Operator op = Operator.convertSpecialCharacter(scs);
            
            // check for dangling operators, just add 0 to do nothing
            if(queue.peek() == null) return new ResolvableExpression(left, new ResolvableConstant(0), Operator.ADD);
            
            ResolvableValue right = parseSubExpression(queue);
            
            ResolvableExpression re = new ResolvableExpression(left, right, op);
            
            // this is either the end or there's another right side
            if(queue.peek() != null) {
                return parseSubExpressionRight(queue, re);
            } else {
                return re;
            }
        } else {
            // we need an operator so oh no
            throw new IllegalArgumentException("Unexpected symbol in expression parse: " + s);
        }
    }
    
    /**
     * Parses the value of the grammar
     * 
     * value        -> Constant
     *              -> Name
     *              
     * @param queue
     * @param ignoreExtraOperators
     * @return
     */
    private static ResolvableValue parseValue(LinkedList<Symbol> queue) {
        Symbol s = queue.peek();
        
        // handle special symbols
        if(s instanceof SpecialCharacterSymbol scs) {
            if(scs.character() == '$' || scs.character() == '@') {
                queue.poll();
                s = queue.peek();
                
                if(s instanceof NameSymbol ns) {
                    queue.poll();
                    
                    // target - $/@
                    return new ResolvableExpression(new ResolvableConstant(ns.name()), new ResolvableConstant(scs.character() + ""), Operator.SUBTRACT);
                } else {
                    return new ResolvableConstant(scs.character() + "");
                }
            }
        } else if(s instanceof ConstantSymbol cs) {
            queue.poll();
            return new ResolvableConstant(cs.value());
        } else if(s instanceof NameSymbol ns) {
            queue.poll();
            return new ResolvableConstant(ns.name());
        }
        
        throw new IllegalArgumentException("Unexpected symbol in expression parse: " + s);
    }
}

/*
 * Created on Jan 26, 2005
 *
 */
package sfdc.formula.commands;

import java.util.*;

import antlr.Token;
import sfdc.formula.BindingObserver;
import sfdc.formula.FormulaEngine;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.InvalidFunctionReferenceException;
import sfdc.formula.parser.gen.SfdcFormulaTokenTypes;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 * TODO SLT FIXME: Rename to be the binding observer/type static stuff only
 */
public class FormulaCommandInfoRegistry {
    public final static String DYNAMIC_REF = "[]";
    public final static String DYNAMIC_REF_IDENT = "DYNAMIC_REF_IDENT"; 

    public static void addBindingObserver(BindingObserver bindingObserver) {
        synchronized (bindingObservers) {
            bindingObservers.add(bindingObserver);
        }
    }

    public static List<BindingObserver> getBindingObservers() {
        synchronized (bindingObservers) {
            return Collections.unmodifiableList(new ArrayList<BindingObserver>(bindingObservers));
        }
    }

    public static FormulaCommandInfo get(FormulaAST node) throws InvalidFunctionReferenceException {
        String name = typeToName.get(node.getType());
        return get((name != null) ? name : node.getText(), node.getToken());
    }
    
    /**
     * @deprecated
     */
    @Deprecated // Use FormulaEngine directly
    public static FormulaCommandInfo[] getCommands() {
    	return FormulaEngine.getFactory().getTypeRegistry().getCommands().toArray(new FormulaCommandInfo[0]);
    }


    private static FormulaCommandInfo get(String name, Token token) throws InvalidFunctionReferenceException {
        FormulaCommandInfo commandInfo = (FormulaCommandInfo) FormulaEngine.getFactory().getTypeRegistry().getAllowNull(name);

        if (commandInfo != null) {
            return commandInfo;
        } else {
            throw new InvalidFunctionReferenceException(name, token != null ? token.getColumn() : -1);
        }
    }
    
    /**
     * @deprecated
     */
    @Deprecated // Use FormulaEngine directly    
    public static FormulaCommandInfo getAllowNull(String name) {
    	return (FormulaCommandInfo) FormulaEngine.getFactory().getTypeRegistry().getAllowNull(name);
    }

    // Note: since the binding observers are registered with the functions are created, we need to do this here.
    private static final List<BindingObserver> bindingObservers = new ArrayList<BindingObserver>();
    private static final Map<Integer, String> typeToName = new HashMap<Integer, String>();

    static {
        // Basic "hardwired" engine functions
        typeToName.put(SfdcFormulaTokenTypes.IDENT, "IDENT");
        typeToName.put(SfdcFormulaTokenTypes.NUMBER, "NUMBER");
        typeToName.put(SfdcFormulaTokenTypes.STRING_LITERAL, "STRING_LITERAL");
        typeToName.put(SfdcFormulaTokenTypes.NOUNESCAPE_STRING_LITERAL, "NOUNESCAPE_STRING_LITERAL");
        typeToName.put(SfdcFormulaTokenTypes.TEMPLATE_STRING_LITERAL, "TEMPLATE_STRING_LITERAL");
        typeToName.put(SfdcFormulaTokenTypes.TRUE, "TRUE");
        typeToName.put(SfdcFormulaTokenTypes.FALSE, "FALSE");
        typeToName.put(SfdcFormulaTokenTypes.NULL, "NULL");
        typeToName.put(SfdcFormulaTokenTypes.DYNAMIC_REF, DYNAMIC_REF);
        typeToName.put(SfdcFormulaTokenTypes.DYNAMIC_REF_IDENT, DYNAMIC_REF_IDENT);
    }

}

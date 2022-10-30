package com.force.formula.commands;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.force.formula.FormulaCommandType;

/**
 * Validator of style of Formula Commands should be immutable
 *
 * @author stamm
 * @since 0.3
 *  */
public class FormulaCommandInvariants {

    public FormulaCommandInvariants() {
        // TODO Auto-generated constructor stub
    }

    public static void validateCommandImmutable(StringBuilder results, FormulaCommandType commandInfo) {
        Class<?> cls = commandInfo.getClass();
        while (cls != null) {
            for (Field field : cls.getDeclaredFields()) {
                // Ignore this. It's not ideal but we made it modifiable for testing purposes.
                if (!Modifier.isFinal(field.getModifiers())) {
                    results.append("CommandInfo[").append(commandInfo.getName()).append(", ").append(cls.getName());
                    results.append("] contains mutable field '").append(field.getName()).append("'\n");
                }
            }
            cls = cls.getSuperclass();
        }
    }
}

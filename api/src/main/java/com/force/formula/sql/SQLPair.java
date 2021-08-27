package com.force.formula.sql;

/**
 * 
 * Helper class for generating SQL (anywhere where you need to pass error checks to an external system)
 *
 * @author djacobs
 * @since 140
 */
public class SQLPair {
    public final String sql;
    public final String guard;

    public SQLPair(String sql, String guard) {
        this.sql = sql;
        this.guard = guard;
    }
    
    public static final String generateGuard(String[] guards, String base) {
        String result = base;
        for (int i = 0; i < guards.length; i++) {
            if (guards[i] == null)
                continue;
            if (result == null)
                result = guards[i];
            else
                result = guards[i] + " OR " + result;
        }
        return result;
    }
}

package com.force.formula.sql;

import java.util.Objects;

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
    
    @Override
	public int hashCode() {
    	return Objects.hash(sql, guard);
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SQLPair sqlPair = (SQLPair) o;
        return Objects.equals(sql, sqlPair.sql) && Objects.equals(guard, sqlPair.guard);
    }

    @Override
    public String toString() {
        return "SQLPair{" +
                "sql='" + sql + '\'' +
                ", guard='" + guard + '\'' +
                '}';
    }
}

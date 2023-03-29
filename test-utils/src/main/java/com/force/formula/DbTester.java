package com.force.formula;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Allows pluggable testing of function evaluation through sql.  If you want fancier stuff, you can
 * override the whole class.
 * @since 0.1.0
 */
public interface DbTester extends AutoCloseable {
    String getDbTypeName();

    /**
     * Evaluate the sql for the formula using sql
     * @param formulaContext the formula context
     * @param entityObject an object representing the values to use for the particular row.  You may want to
     * insert this row into the DB when doing the valuation to make the DB substitution easier
     * @param formulaSource the source of the formula
     * @param nullAsNull whether null is treated as null or as blank/0
     * @return the result of evaluating the formula using a sql engine
     * @throws IOException if there is an IO issue with the sql engine
     * @throws SQLException if there is an issue evaluating the sql
     * @throws FormulaException if there is an issue evaluating the formula
     */
    String evaluateSql(String name, FormulaRuntimeContext formulaContext, Object entityObject, String formulaSource, boolean nullAsNull) throws IOException, SQLException, FormulaException;

    /**
     * Some JDBC drivers include a UUID in all exceptions, so this allows you to "remove" that.
     * @param e the exception thrown during evaluation
     * @return the message to use in GoldFiles for the exception.
     * @throws ArithmeticException if you want to treat this exception as a NULL instead of an error.  Suitable
     * if there is an architectural difference when running on different platforms (Linux vs Mac)
     */
    default String getSqlExceptionMessage(Throwable e) {
        return e.getMessage();
    }

    // Support close, if necessary.
    @Override
    default void close() throws Exception {
    }


}

/**
 * 
 */
package com.force.formula.sql.mysql;

import java.io.IOException;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.force.formula.DisplayField;
import com.force.formula.sql.DbContainerTester;

/**
 * Container tester for MySQL style DBs.
 *
 * @author stamm
 * @since 0.3
 */
public abstract class MySQLStyleContainerTester<DB extends JdbcDatabaseContainer<?>> extends DbContainerTester<DB> {

    /**
     * @throws IOException
     */
    public MySQLStyleContainerTester() throws IOException {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected boolean useBinds() {
        // The sql driver doesn't like control characters in literals it seems.
        return true;
    }

    /**
     * @return the value to use as a null value for the display field.
     * @param df the display field
     */
    @Override
    protected String getNullSqlValue(DisplayField df) {
        return "NULL";
    }

    /**
     * @return the type to use as as text type when testing (text in postgres, varchar in oracle)
     */
    @Override
    protected String getTextType() {
        return "varchar";
    }
    
    /**
     * @param arg the string to convert to a timestamp
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDateTime(String arg) {
        return "TIMESTAMP("+arg+")";
    }
    
    /**
     * @param arg the string to convert to a date
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDate(String arg) {
        return "STR_TO_DATE(" + arg + ",'%D-%m-%Y')";
    }
}

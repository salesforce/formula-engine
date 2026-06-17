package com.force.formula.sql;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Properties;

import com.force.formula.*;
import com.force.formula.util.FormulaI18nUtils;

public class DataCloudTester extends AbstractDbTester {

    private Connection conn = null;

    public DataCloudTester() throws IOException {
    }

    @Override
    public String getDbTypeName() {
        return "datacloud";
    }

    /**
     * Creates a connection to Salesforce DataCloud using JDBC driver.
     * Reads connection properties from application.properties file in the classpath.
     *
     * @return a Connection to DataCloud
     * @throws SQLException if there is an issue creating the connection
     * @throws IOException if there is an issue reading the properties file
     */
    @Override
    protected Connection getConnection() throws SQLException, IOException {
        if (this.conn == null) {
            try {
                Class.forName("com.salesforce.datacloud.jdbc.DataCloudJDBCDriver");
            } catch (ClassNotFoundException e) {
                throw new IOException("DataCloud JDBC driver not found", e);
            }
            Properties props = new Properties();
            try (InputStream is = DataCloudTester.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (is == null) {
                    throw new IOException("application.properties file not found in classpath");
                }
                props.load(is);
            }

            String loginUrl = props.getProperty("loginUrl");
            if (loginUrl == null) {
                throw new IOException("loginUrl property not found in application.properties");
            }
            String hostname = loginUrl.replaceFirst("^https?://", "");

            String url = "jdbc:salesforce-datacloud://" + hostname;

            Properties connectionProperties = new Properties();
            connectionProperties.put("userName", props.get("userName"));
            connectionProperties.put("clientId", props.get("clientId"));
            connectionProperties.put("clientSecret", props.get("clientSecret"));
            connectionProperties.put("refreshToken", props.get("refreshToken"));

            this.conn = DriverManager.getConnection(url, connectionProperties);
        }
        return this.conn;
    }

    /**
     * Don't close the connection per statement, only at the end of the test.
     */
    @Override
    protected void closeConnectionPerStmt(Connection conn) throws SQLException {
        // Do nothing - reuse the connection
    }

    @Override
    protected String getDecimalType() {
        return "numeric";
    }

    @Override
    protected String getTextType() {
        return "text";
    }

    @Override
    protected String getTimestampType() {
        return "timestamp";
    }

    @Override
    protected String stringToDateTime(String arg) {
        return arg + "::timestamp";
    }

    @Override
    protected String convertToDateTime(String arg) {
        return arg + "::timestamp";
    }

    /**
     * Override formatDbResult to handle Hyper DB differences:
     * 1. Timestamps: Hyper returns sub-second precision since we use ::timestamp instead of ::timestamp(0).
     *    Truncate to whole seconds to match PostgreSQL ::timestamp(0) behavior.
     * 2. Decimals: Hyper returns fewer trailing zeros than PostgreSQL for division/multiplication.
     *    Pad CURRENCY/PERCENT to 30 decimal places to match PostgreSQL's numeric scale.
     */
    @Override
    protected String formatDbResult(ResultSet rset, FormulaRuntimeContext formulaContext, Formula formula) throws SQLException {
        MockFormulaDataType returnType = (MockFormulaDataType) formula.getDataType();
        switch (returnType) {
        case DATEONLY:
        case DATETIME:
            try {
                Timestamp d = rset.getTimestamp(1);
                if (d == null)
                    return null;
                // Truncate to whole seconds to match PostgreSQL ::timestamp(0) behavior
                d.setNanos(0);
                return d.toString();
            } catch (IllegalArgumentException | SQLException ex) {
                Date d = rset.getDate(1);
                if (d == null)
                    return null;
                return new Timestamp(d.getTime()).toString();
            }
        case CURRENCY:
        case PERCENT:
            BigDecimal bigDecimal = rset.getBigDecimal(1);
            if (bigDecimal == null)
                return null;
            // PostgreSQL NUMERIC preserves high precision for division/multiplication.
            // Hyper returns fewer decimal places. Pad to at least 30 decimal places
            // to match PostgreSQL's typical NUMERIC scale for currency operations.
            if (bigDecimal.scale() < 30) {
                bigDecimal = bigDecimal.setScale(30);
            }
            return String.valueOf(FormulaI18nUtils.formatResult(formulaContext, formulaContext.getFormulaReturnType(), bigDecimal));
        case INTEGER:
        case DOUBLE:
            BigDecimal number = rset.getBigDecimal(1);
            if (number == null)
                return null;
            return number.stripTrailingZeros().toPlainString();
        default:
            break;
        }
        return super.formatDbResult(rset, formulaContext, formula);
    }

    /**
     * Strip Hyper DB error message wrapper to match PostgreSQL error format.
     * Hyper wraps errors as: "Failed to execute query: <msg> [TraceId:...] SQLSTATE:... QUERY:..."
     * PostgreSQL returns plain: "ERROR: <msg>"
     */
    @Override
    public String getSqlExceptionMessage(Throwable e) {
        String msg = e.getMessage();
        if (msg != null && msg.startsWith("Failed to execute query: ")) {
            msg = msg.substring("Failed to execute query: ".length());
            // Strip trailing metadata: [TraceId:...] SQLSTATE:... QUERY-ID:... DETAIL:... QUERY:...
            int traceIdx = msg.indexOf(" [TraceId:");
            if (traceIdx > 0) {
                msg = msg.substring(0, traceIdx);
            }
            int sqlStateIdx = msg.indexOf(" SQLSTATE:");
            if (sqlStateIdx > 0) {
                msg = msg.substring(0, sqlStateIdx);
            }
            return "ERROR: " + msg;
        }
        return msg;
    }

    @Override
    public void close() throws Exception {
        if (this.conn != null) {
            this.conn.close();
            this.conn = null;
        }
    }

}
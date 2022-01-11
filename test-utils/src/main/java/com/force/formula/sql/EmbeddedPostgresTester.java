/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * a DbTester of formulas that uses embedded postgres for validation.  Use this
 * if you don't want to deal with docker.
 * 
 * Instead of writing rows to the database, it puts all of the "passed in
 * values" in a subquery with alias "c", casting the null values to the right types.
 * 
 * @author stamm
 */
public class EmbeddedPostgresTester extends AbstractDbTester {

	private EmbeddedPostgres pg = null;

	public EmbeddedPostgresTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "postgres";
	}

	/**
	 * @return an EmbeddedPostgres to use
	 * @throws IOException if an exception occurred while constructing the postgres db
	 */
	protected EmbeddedPostgres constructPostgres() throws IOException {
		return EmbeddedPostgres.builder().start();
	}
	
	final EmbeddedPostgres getPostgres() throws IOException {
		if (pg == null) {
			pg = constructPostgres();
		}
		return this.pg;
	}
	
	@Override
	protected Connection getConnection() throws SQLException, IOException {
		return getPostgres().getDatabase("postgres", "postgres").getConnection();
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

	@Override
	public void close() throws Exception {
		// It autocloses.
	}

}

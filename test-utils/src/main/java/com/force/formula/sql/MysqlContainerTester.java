/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.MySQLContainer;

/**
 * Mysql tester that uses a container. 
 * @author stamm
 */
public class MysqlContainerTester extends DbContainerTester<MySQLContainer<?>> {

	/**
	 * @throws IOException
	 */
	public MysqlContainerTester() throws IOException {
	}

	@Override
	protected MySQLContainer<?> constructDb() throws IOException {
		return new MySQLContainer<>("mysql");
	}

}

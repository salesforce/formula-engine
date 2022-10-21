/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.TrinoContainer;

/**
 * Trino tester that uses a container
 * @author stamm
 */
public class TrinoContainerTester extends PrestoStyleContainerTester<TrinoContainer> {

	/**
	 * @throws IOException
	 */
	public TrinoContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "trino";
	}

	@Override
	protected TrinoContainer constructDb() throws IOException {
		return new TrinoContainer("trinodb/trino");
	}
}

/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.PrestoContainer;

/**
 * Presto tester that uses a container.  
 * @author stamm
 */
@SuppressWarnings("deprecation") // Presto is replaced with Trino, but Athena is based on Presto for now.
public class PrestoContainerTester extends PrestoStyleContainerTester<PrestoContainer<?>> {

	/**
	 * @throws IOException
	 */
	public PrestoContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "presto";
	}

	@Override
	protected PrestoContainer<?> constructDb() throws IOException {
		return new PrestoContainer<>("ghcr.io/trinodb/presto");
	}

}

/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.OracleContainer;

/**
 * Oracle tester that uses a container.  
 * @author stamm
 */
public class OracleContainerTester extends DbContainerTester<OracleContainer> {

	/**
	 * @throws IOException
	 */
	public OracleContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "oracle";
	}

	@Override
	protected OracleContainer constructDb() throws IOException {
		return new OracleContainer("gvenzl/oracle-xe");
	}

	/**
	 * @return the string that will be "from dual" in oracle in the subselect
	 */
    @Override
    protected String getFromDual()  {
    	return " FROM DUAL";
    }
	
	/**
	 * @return the type to use as as text type when testing (text in postgres, varchar2 in oracle)
	 */
    @Override
	protected String getTextType() {
		return "varchar2(255)";
	}

}

/**
 * 
 */
package com.force.formula.util;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaFieldInfo.FormulaFieldType;
import com.force.formula.FormulaReturnType;
import com.force.formula.FormulaSchema;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.FormulaSchema.Field;
import com.force.formula.FormulaSchema.FieldOrColumn;
import com.force.formula.GlobalFormulaProperties;

/**
 * Test of the functions on ContextualFormulaFieldInfoImpl 
 * @author stamm
 * @since 0.1.16
 */
public class ContextualFormulaFieldInfoImplTest {

    @Test
    public void testContextEqualsHashCode() {
        FormulaContext context = new NullFormulaContext(new GlobalFormulaProperties(null)) {
            @Override
            public FormulaReturnType getFormulaReturnType() {
                return null;
            }
            @Override
            public String getName() {
                return "hi";
            }
        };
        FieldOrColumn field1 = new MockFieldOrColumn("test");
        FieldOrColumn field2 = new MockFieldOrColumn("test");
        FieldOrColumn field3 = new MockFieldOrColumn("nottest");
        ContextualFormulaFieldInfo info1 = new ContextualFormulaFieldInfoImpl(context, field1);
        ContextualFormulaFieldInfo info2 = new ContextualFormulaFieldInfoImpl(context, field2);
        Assert.assertEquals(info1, info2);
        Assert.assertEquals(info1.hashCode(), info2.hashCode());
        Assert.assertNotSame(info1, info2);
        Assert.assertNotEquals(field1, field3);
        Assert.assertNotEquals(field1.hashCode(), field3.hashCode());
    }

    
    public static class MockFieldOrColumn implements FormulaSchema.Field {
        private final String name;
        public MockFieldOrColumn(String name) {
            this.name = name;
        }
        @Override
        public FormulaDataType getDataType() {
            return null;
        }
        @Override
        public Field getFieldInfo() {
            return this;
        }
        @Override
        public Entity getEntityInfo() {
            return null;
        }
        @Override
        public String getName() {
            return this.name;
        }
        @Override
        public boolean isColumnInfo() {
            return true;
        }
        @Override
        public Entity[] getFormulaForeignKeyDomains() {
            return null;
        }
        @Override
        public FormulaFieldType getFieldType() {
            return null;
        }
        @Override
        public int hashCode() {
            return Objects.hash(this.name);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MockFieldOrColumn other = (MockFieldOrColumn)obj;
            return Objects.equals(this.name, other.name);
        }
        
    }
    
}

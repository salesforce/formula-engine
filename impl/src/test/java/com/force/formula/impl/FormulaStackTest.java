package com.force.formula.impl;

import org.junit.Assert;

import com.google.common.collect.Iterators;

import junit.framework.TestCase;

/**
 * Test FormulaStack masks null
 * @author stamm
 */
public class FormulaStackTest extends TestCase {

    public FormulaStackTest(String name) {
        super(name);
    }
    
    public void testMethods() {
        FormulaStack stack = new FormulaStack(8);
        stack.addLast("hi");
        stack.addFirst(null);
        Assert.assertArrayEquals(new Object[] {null, "hi"}, Iterators.toArray(stack.iterator(), Object.class));
        Assert.assertArrayEquals(new Object[] {"hi", null}, Iterators.toArray(stack.descendingIterator(), Object.class));
        
        // Note this is the opposite of Deque's usual order
        Assert.assertArrayEquals(new Object[] {"hi", null}, stack.toArray());
        Assert.assertArrayEquals(new String[] {"hi", null}, stack.toArray(new String[0]));
        assertNull(stack.removeFirst());
        assertEquals("hi", stack.removeLast()); 
        assertEquals(0, stack.size());
        assertTrue(stack.offerLast("hi"));
        assertTrue(stack.offerFirst(null));
        assertNull(stack.pollFirst());
        assertEquals("hi", stack.pollLast());
        assertNull(stack.pollFirst());
    }

}

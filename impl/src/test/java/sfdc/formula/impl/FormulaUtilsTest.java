package sfdc.formula.impl;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FormulaUtilsTest {

    @Test
    public void testNumberOfPrecedingBackslashesZero() {
        String input = "abcdefghijklmnopqrstuvwxyz";
        for(int i = 0; i < input.length(); i++) {
            assertTrue(FormulaUtils.numberOfPrecedingBackslashes(input, i) == 0);
        }
    }

    @Test
    public void testNumberOfPrecedingBackslashesNonZero() {
        for(int i = 1; i < 10; i++) {
            String input = generateStringWithBackslashes(i);
            assertTrue(FormulaUtils.numberOfPrecedingBackslashes(input, input.length() - 1) == i);
        }
    }

    @Test
    public void testNumberOfPrecedingBackslashesCornerCases() {
        String input = "abcdefghijklmnopqrstuvwxyz";
        assertTrue(FormulaUtils.numberOfPrecedingBackslashes(input, -1) == 0);
        assertTrue(FormulaUtils.numberOfPrecedingBackslashes(input, input.length() + 1) == 0);


        input = "abc\\";
        assertTrue(FormulaUtils.numberOfPrecedingBackslashes(input, input.length()) == 1);
    }

    @Test
    public void testNumberOfPrecedingBackslashesNotIncludingIndex() {
        String input = generateStringWithBackslashes(4); // "\\\\'"

        for(int i = 0; i < input.length() - 1; i++) {
            assertTrue(FormulaUtils.numberOfPrecedingBackslashes(input, i) == i);
        }
    }
    
    private static String generateStringWithBackslashes(int count) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < count; i++) {
            sb.append("\\");
        }
        sb.append("'");
        return sb.toString();
    }
}

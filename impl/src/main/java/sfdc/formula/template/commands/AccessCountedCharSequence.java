package sfdc.formula.template.commands;

/**
 * A char sequence that counts the number of accesses and limits that number.
 * To be used with regular expressions and other string manipulation code that does not inheritly have useful runtime limits.
 * 
 * @author lhofhansl
 * @since 152
 */
public class AccessCountedCharSequence implements CharSequence {
    private final CharSequence source;
    private int count;
    private final int maxAccess;

    public AccessCountedCharSequence(CharSequence source, int maxAccess) {
        this.source = source;
        this.maxAccess = maxAccess;
        this.count = 0;
    }

    @Override
    public char charAt(int index) {
        count++;
        check();
        return source.charAt(index);
    }

    @Override
    public int length() {
        return source.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        count += end - start;
        check();
        return source.subSequence(start, end);
    }

    @Override
    public String toString() {
        count += length();
        check();
        return source.toString();
    }

    private void check() {
        if (count > maxAccess) {
            fail();
        }
    }
    
    protected void fail() {
        throw new AccessCountExceededException();        
    }
    
    public static class AccessCountExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}


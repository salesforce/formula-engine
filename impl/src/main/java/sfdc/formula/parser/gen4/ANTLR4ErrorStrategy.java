package sfdc.formula.parser.gen4;

import org.antlr.v4.runtime.*;

/**
 * @author ashanjani
 * @since 220
 */
public class ANTLR4ErrorStrategy extends DefaultErrorStrategy {

    /**
     * Due to a bug in DefaultErrorStrategy.reportError() which causes a NPE for
     * a formula like "abc #", we override it to do nothing.
     * TODO(arman): possibly send a pull request to ANTLR4 repo to fix this issue
     */
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {

    }

    /** Instead of recovering from exception e, rethrow it wrapped
     *  in a generic RuntimeException so it is not caught by the
     *  rule function catches.  Exception e is the "cause" of the
     *  RuntimeException.
     */
    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        throw e;
    }

    /** Make sure we don't attempt to recover inline; if the parser
     *  successfully recovers, it won't throw an exception.
     */
    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
        throw new InputMismatchException(recognizer);
    }

    /** Make sure we don't attempt to recover from problems in subrules. */
    @Override
    public void sync(Parser recognizer) {

    }
}

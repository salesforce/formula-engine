package com.force.formula;

import com.force.formula.impl.BaseFormulaInfoImpl;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaASTVisitor;
import com.force.formula.impl.FormulaUtils;
import com.force.formula.parser.gen.FormulaTokenTypes;
import junit.framework.TestCase;

import java.util.*;

public class BaseFormulaInfoImplTest extends TestCase {

    //Test for Labels which are treated as templates and are defined in a linear way
    public void testVisit1() throws Exception{

        final List<String> actualOutput = new ArrayList<>();
        FormulaASTVisitor visitor = new FormulaASTVisitor() {
            @Override
            public void visit(FormulaAST node) throws FormulaException {
                if (node.getType() == FormulaTokenTypes.IDENT) {
                    actualOutput.add(node.getText());
                }
            }
        };
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsTemplate(true);

        //Testcase 1: source is empty (source can never be null)
        String source1 = "";
        List<String> expectedOutput1 = Collections.emptyList();
        FormulaAST ast1 = FormulaUtils.parse(source1,properties);

        //Testcase 2: source has 1 Label
        String source2 = "{!$Label.a}";
        List<String> expectedOutput2 = Arrays.asList("$Label.a");
        FormulaAST ast2 = FormulaUtils.parse(source2,properties);

        //Testcase 3: source has multiple Labels
        String source3 = "\n" +
                "    {!$Label.a}\n" +
                "    {!$Label.b}\n" +
                "    {!$Label.c}\n" +
                "    {!$Label.d}";
        List<String> expectedOutput3 = Arrays.asList("$Label.a",
                "$Label.b",
                "$Label.c",
                "$Label.d");
        FormulaAST ast3 = FormulaUtils.parse(source3,properties);

        BaseFormulaInfoImpl.visit(ast1,visitor,properties);
        assertEquals(actualOutput,expectedOutput1);

        actualOutput.clear();

        BaseFormulaInfoImpl.visit(ast2,visitor,properties);
        assertEquals(actualOutput,expectedOutput2);

        actualOutput.clear();

        BaseFormulaInfoImpl.visit(ast3,visitor,properties);
        assertEquals(actualOutput,expectedOutput3);

        actualOutput.clear();
    }

    //Test if the AST is visited in-order manner by BaseFormulaInfoImpl.visit()
    public void testVisit0() throws Exception{
        FormulaProperties properties = new FormulaProperties();
        final List<String> actualOutput = new ArrayList<>();
        FormulaASTVisitor visitor = new FormulaASTVisitor() {
            @Override
            public void visit(FormulaAST node) throws FormulaException {
                if (node.getType() == FormulaTokenTypes.NUMBER) {
                    actualOutput.add(node.getText());
                }
            }
        };
        //Testcase 1: when root of AST is null
        FormulaAST ast1 = constructTestAST(null);
        BaseFormulaInfoImpl.visit(ast1, visitor, properties);
        assertEquals(Collections.EMPTY_LIST,actualOutput);

        actualOutput.clear();

        //Testcase 2: when root of AST has 1 node
        FormulaAST ast2 = constructTestAST("1");
        BaseFormulaInfoImpl.visit(ast2, visitor, properties);
        assertEquals(Arrays.asList("1"), actualOutput);

        actualOutput.clear();

        //Testcase 3: when root of AST has 3 node like shown below
        FormulaAST ast3 = constructTestAST("1,2,3");
        /*
                   1
                 /   \
                2     3
         */
        BaseFormulaInfoImpl.visit(ast3, visitor, properties);
        assertEquals(Arrays.asList("2","1","3"), actualOutput);

        actualOutput.clear();

        //Testcase 4: when root of AST has 3 node like shown below
        FormulaAST ast4 = constructTestAST("1,null,2,null,3");
        /*
                   1
                    \
                     2
                      \
                       3
         */
        BaseFormulaInfoImpl.visit(ast4, visitor, properties);
        assertEquals(Arrays.asList("1","2","3"), actualOutput);

        actualOutput.clear();

        //Testcase 5: when root of AST has 3 node like shown below
        FormulaAST ast5 = constructTestAST("1,null,2,3,null");
        /*
                   1
                    \
                     2
                    /
                   3
         */
        BaseFormulaInfoImpl.visit(ast5, visitor, properties);
        assertEquals(Arrays.asList("1","3","2"), actualOutput);

        actualOutput.clear();

        //Testcase 6: when root of AST has 3 node like shown below
        FormulaAST ast6 = constructTestAST("1,2,null,3,null");
        /*
                   1
                  /
                 2
                /
               3
         */
        BaseFormulaInfoImpl.visit(ast6, visitor, properties);
        assertEquals(Arrays.asList("3","2","1"), actualOutput);

        actualOutput.clear();

        //Testcase 7: when root of AST has 3 node like shown below
        FormulaAST ast7 = constructTestAST("1,2,null,null,3");
        /*
                   1
                  /
                 2
                  \
                   3
         */
        BaseFormulaInfoImpl.visit(ast7, visitor, properties);
        assertEquals(Arrays.asList("2","3","1"), actualOutput);

    }

    //The input represents the serialized format of a binary tree using level order traversal,
    // where null signifies a path terminator where no node exists below.
    private FormulaAST constructTestAST(String data){
        if(data==null || data=="" || data.equals("null")) return null;
        String[] list = data.split(",");
        FormulaAST root = new FormulaAST(list[0]);
        root.setType(FormulaTokenTypes.NUMBER);
        List<FormulaAST> queue = new ArrayList<>();
        queue.add(root);
        for(int i=1;i<list.length;i++){
            FormulaAST parent = queue.remove(0);
            parent.setType(FormulaTokenTypes.NUMBER);
            if(!list[i].equals("null")){
                FormulaAST left = new FormulaAST(list[i]);
                left.setType(FormulaTokenTypes.NUMBER);
                parent.setFirstChild(left);
                queue.add(left);
            }
            if(!list[++i].equals("null")){
                FormulaAST right = new FormulaAST(list[i]);
                right.setType(FormulaTokenTypes.NUMBER);
                parent.setNextSibling(right);
                queue.add(right);
            }
        }
        return root;
    }
}

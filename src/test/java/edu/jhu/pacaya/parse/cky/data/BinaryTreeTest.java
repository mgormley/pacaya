package edu.jhu.pacaya.parse.cky.data;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class BinaryTreeTest {

    @Test
    public void testGetFromPennTreebankString() throws IOException {
        String origTreeStr = "" +
                "((VP (VP1 (VB join)\n" +
                           "(NP (DT the) (NN board) ))\n" +
                      "(VP2 (PP-CLR (IN as)\n" + 
                                   "(NP (DT a) (NN director) ))\n" +
                            "(NP-TMP (NNP Nov.) (CD 29) ))))\n";
        
        StringReader reader = new StringReader(origTreeStr);
        BinaryTree tree = NaryTree.readTreeInPtbFormat(reader).leftBinarize();
        String newTreeStr = tree.getAsPennTreebankString();
                
        System.out.println(newTreeStr);
        newTreeStr = canonicalizeTreeString(newTreeStr);
        origTreeStr = canonicalizeTreeString(origTreeStr);

        assertEquals(origTreeStr, newTreeStr);
    }

    @Test
    public void testCollapseToNaryTree() throws IOException {
        String origBinaryTreeStr = "" + 
                "((VP (@VP (@VP (VB join)\n" +
                                   "(NP (DT the)\n" +
                                       "(NN board)))\n" +
                              "(PP-CLR (IN as)\n" + 
                                      "(NP (@NP (DT a)\n" +
                                               "(JJ nonexecutive))\n" +
                                          "(NN director))))\n"+
                    "(NP-TMP (NNP Nov.)\n"+
                            "(CD 29))))\n";
        String origNaryTreeStr = "" +
                "((VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )))\n";
        
        // First just build (and check) the binary tree.
        StringReader reader = new StringReader(origNaryTreeStr);
        NaryTree naryTree = NaryTree.readTreeInPtbFormat(reader);
        BinaryTree binaryTree = naryTree.leftBinarize();

        String newBinaryTreeStr = binaryTree.getAsPennTreebankString();
        
        
        System.out.println(newBinaryTreeStr);
        newBinaryTreeStr = canonicalizeTreeString(newBinaryTreeStr);
        origBinaryTreeStr = canonicalizeTreeString(origBinaryTreeStr);
        assertEquals(origBinaryTreeStr, newBinaryTreeStr);
        
        // Now do the actual collapsing.
        naryTree = binaryTree.collapseToNary();

        String newNaryTreeStr = naryTree.getAsPennTreebankString();

        // Test the collapsed tree.        
        System.out.println(newNaryTreeStr);
        newNaryTreeStr = canonicalizeTreeString(newNaryTreeStr);
        origNaryTreeStr = canonicalizeTreeString(origNaryTreeStr);
        assertEquals(origNaryTreeStr, newNaryTreeStr);        
    }
    
    private static String canonicalizeTreeString(String newTreeStr) {
        return newTreeStr.trim().replaceAll("\\s+\\)", ")").replaceAll("\\s+", " ");
    }

}

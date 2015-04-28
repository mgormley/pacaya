package edu.jhu.pacaya.parse.cky.intdata;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.jhu.pacaya.util.Alphabet;

public class IntNaryTreeTest {

    @Test
    public void testGetAsPennTreebankString2() throws IOException {
        String origTreeStr = "" +
                "((S (VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )) ))\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        IntNaryTree tree = IntNaryTree.readTreeInPtbFormat(alphabet, alphabet, reader);
        String newTreeStr = tree.getAsPennTreebankString();
        
        System.out.println(alphabet);
        System.out.println(newTreeStr);
        newTreeStr = canonicalizeTreeString(newTreeStr);
        origTreeStr = canonicalizeTreeString(origTreeStr);

        assertEquals(origTreeStr, newTreeStr);
    }

    @Test
    public void testGetFromPennTreebankString() throws IOException {
        String origTreeStr = "" +
                "((VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )))\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        IntNaryTree tree = IntNaryTree.readTreeInPtbFormat(alphabet, alphabet, reader);
        String newTreeStr = tree.getAsPennTreebankString();
        
        System.out.println(alphabet);
        System.out.println(newTreeStr);
        newTreeStr = canonicalizeTreeString(newTreeStr);
        origTreeStr = canonicalizeTreeString(origTreeStr);

        assertEquals(origTreeStr, newTreeStr);
    }

    @Test
    public void testUpdateStartEnd() throws IOException {
        String origTreeStr = "" +
                "((VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )))\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        IntNaryTree tree = IntNaryTree.readTreeInPtbFormat(alphabet, alphabet, reader);
        String newTreeStr = tree.getAsPennTreebankString();
        
        System.out.println(newTreeStr);
        tree.updateStartEnd();
        assertEquals(0, tree.getStart());
        assertEquals(9, tree.getEnd());
        assertEquals(3, tree.getChildren().get(2).getStart());
        assertEquals(7, tree.getChildren().get(2).getEnd());
    }
    

    @Test
    public void testBinarize() throws IOException {
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
        
        StringReader reader = new StringReader(origNaryTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        IntNaryTree naryTree = IntNaryTree.readTreeInPtbFormat(alphabet, alphabet, reader);
        assertEquals(20, alphabet.size());
        IntBinaryTree binaryTree = naryTree.leftBinarize(alphabet);
        assertEquals(22, alphabet.size());

        String newBinaryTreeStr = binaryTree.getAsPennTreebankString();
        
        System.out.println(alphabet);
        System.out.println(newBinaryTreeStr);
        newBinaryTreeStr = canonicalizeTreeString(newBinaryTreeStr);
        origBinaryTreeStr = canonicalizeTreeString(origBinaryTreeStr);
        assertEquals(origBinaryTreeStr, newBinaryTreeStr);        
    }
    
    private static String canonicalizeTreeString(String newTreeStr) {
        return newTreeStr.trim().replaceAll("\\s+\\)", ")").replaceAll("\\s+", " ");
    }

}

package edu.jhu.hltcoe.parse.cky;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.util.Alphabet;

public class NaryTreeNodeTest {

    @Test
    public void testGetAsPennTreebankString2() throws IOException {
        String origTreeStr = "" +
                "( (S (VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )) ))\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        NaryTreeNode tree = NaryTreeNode.readTreeInPtbFormat(alphabet, alphabet, reader);
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
                "( (VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )) )\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        NaryTreeNode tree = NaryTreeNode.readTreeInPtbFormat(alphabet, alphabet, reader);
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
                "( (VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )) )\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        NaryTreeNode tree = NaryTreeNode.readTreeInPtbFormat(alphabet, alphabet, reader);
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
        String origTreeStr = "" +
                "( (VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) )) )\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        NaryTreeNode naryTree = NaryTreeNode.readTreeInPtbFormat(alphabet, alphabet, reader);
        assertEquals(20, alphabet.size());
        BinaryTreeNode binaryTree = naryTree.binarize(alphabet);
        assertEquals(22, alphabet.size());

        String newTreeStr = binaryTree.getAsPennTreebankString();
        
        System.out.println(alphabet);
        System.out.println(newTreeStr);
        newTreeStr = canonicalizeTreeString(newTreeStr);
        Assert.assertTrue(newTreeStr.contains("(VP (@VP (@VP (@VP (VB join)"));
        Assert.assertTrue(newTreeStr.contains("(NP (@NP (@NP (DT a) (JJ nonexecutive)) (NN director)) (NN director))))"));
    }
    
    private static String canonicalizeTreeString(String newTreeStr) {
        return newTreeStr.trim().replaceAll("\\s+\\)", ")").replaceAll("\\s+", " ");
    }

}

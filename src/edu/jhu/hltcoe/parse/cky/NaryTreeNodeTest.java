package edu.jhu.hltcoe.parse.cky;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.jhu.hltcoe.util.Alphabet;

public class NaryTreeNodeTest {

    @Test
    public void testGetFromPennTreebankString() throws IOException {
        String origTreeStr = "" +
                "(VP (VB join)\n" +
                    "(NP (DT the) (NN board) )\n" +
                    "(PP-CLR (IN as)\n" + 
                      "(NP (DT a) (JJ nonexecutive) (NN director) ))\n" +
                    "(NP-TMP (NNP Nov.) (CD 29) ))\n";
        
        StringReader reader = new StringReader(origTreeStr);
        Alphabet<String> alphabet = new Alphabet<String>();
        NaryTreeNode tree = NaryTreeNode.readTreeInPtbFormat(alphabet, reader);
        String newTreeStr = tree.getAsPennTreebankString();
        
        System.out.println(alphabet);
        System.out.println(newTreeStr);
        newTreeStr = canonicalizeTreeString(newTreeStr);
        origTreeStr = canonicalizeTreeString(origTreeStr);

        assertEquals(origTreeStr, newTreeStr);
    }

    private static String canonicalizeTreeString(String newTreeStr) {
        return newTreeStr.trim().replaceAll("\\s+\\)", ")").replaceAll("\\s+", " ");
    }

}

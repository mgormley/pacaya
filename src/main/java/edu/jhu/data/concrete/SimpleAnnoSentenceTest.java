package edu.jhu.data.concrete;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.featurize.SentFeatureExtractorTest;

public class SimpleAnnoSentenceTest {

    @Test
    public void testGetParentsAndUseGoldSyntax() {
        CoNLL09Sentence sent = SentFeatureExtractorTest.getDogConll09Sentence();
        {
            // Test with gold syntax.
            boolean useGoldSyntax = true;
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(useGoldSyntax);
            assertArrayEquals(new int[] { 1, 2, -1, 2 }, simpleSent.getParents());
        }
        {
            // Test without gold syntax.
            boolean useGoldSyntax = false;
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(useGoldSyntax);
            assertArrayEquals(new int[] { 2, 0, -1, 2 }, simpleSent.getParents());
        }
    }
    
}

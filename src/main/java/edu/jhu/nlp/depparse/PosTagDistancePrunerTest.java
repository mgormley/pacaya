package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.util.collections.Lists;

public class PosTagDistancePrunerTest {

    @Test
    public void testAutoSents() {
        // Checks that we get 100% oracle pruning accuracy when evaluating on our training data.
        AnnoSentenceCollection sents = getSents(3, 10, 10);
        System.out.println(sents);
        PosTagDistancePruner pruner = new PosTagDistancePruner();
        pruner.train(null, sents, null, null);
        pruner.annotate(sents);
        System.out.println(sents);
        AnnoSentence sent = sents.get(0);
        for (int child=0; child<sent.size(); child++) {
            int trueParent = sent.getParent(child);
            for (int p=-1; p<sent.size(); p++) {
                if (p == child) {
                    continue;
                }
                boolean pruned = sent.getDepEdgeMask().isPruned(p, child);
                if (p == trueParent) {
                    assertTrue(!pruned);
                } else {
                    assertTrue(pruned);
                }
            }
        }
    }

    @Test
    public void testTwoSents() {
        AnnoSentenceCollection sents1 = new AnnoSentenceCollection();        
        AnnoSentence sent1 = new AnnoSentence();
        sent1.setWords(  Lists.getList("0", "1", "2", "3", "4", "5", "6"));
        sent1.setPosTags(Lists.getList("N", "N", "V", "D", "N", "P", "N"));
        sent1.setParents(new int[]    {  1,   2,  -1,   4,   2,   2,  5 });
        sents1.add(sent1);
        
        AnnoSentenceCollection sents2 = new AnnoSentenceCollection();        
        AnnoSentence sent2 = new AnnoSentence();
        sent2.setWords(  Lists.getList("0", "1", "2", "3", "4", "5", "6"));
        sent2.setPosTags(Lists.getList("N", "V", "P", "D", "P", "P", "N"));
        sent2.setParents(new int[]    { -1,   0,   1,   2,   3,   4,  5 });
        sents2.add(sent2);
        
        PosTagDistancePruner pruner = new PosTagDistancePruner();
        pruner.train(null, sents1, null, null);
        pruner.annotate(sents2);        

        System.out.println(sents1);
        System.out.println(sents2);
        
        assertTrue(sent2.getDepEdgeMask().isPruned(0, 1)); // N V
        assertTrue(!sent2.getDepEdgeMask().isPruned(1, 0)); // V N
        assertTrue(sent2.getDepEdgeMask().isPruned(1, 6)); // V N
        assertTrue(!sent2.getDepEdgeMask().isPruned(1, 2)); // V P
        assertTrue(!sent2.getDepEdgeMask().isPruned(1, 4)); // V P
        assertTrue(sent2.getDepEdgeMask().isPruned(1, 5)); // V P
        assertTrue(!sent2.getDepEdgeMask().isPruned(5, 6)); // P N
    }    
    
    @Test
    public void testDirection() {
        AnnoSentenceCollection sents1 = new AnnoSentenceCollection();        
        AnnoSentence sent1 = new AnnoSentence();
        sent1.setWords(  Lists.getList("0", "1", "2", "3"));
        sent1.setPosTags(Lists.getList("N", "N", "V", "V"));
        sent1.setParents(new int[]    {  3,   3,  3,   -1});
        sents1.add(sent1);
        
        AnnoSentenceCollection sents2 = new AnnoSentenceCollection();        
        AnnoSentence sent2 = new AnnoSentence();
        sent2.setWords(  Lists.getList("0", "1", "2", "3", "4"));
        sent2.setPosTags(Lists.getList("V", "V", "N", "N", "V"));
        sents2.add(sent2);
        
        PosTagDistancePruner pruner = new PosTagDistancePruner();
        pruner.train(null, sents1, null, null);
        pruner.annotate(sents2);        

        System.out.println(sents1);
        System.out.println(sents2);
        
        assertTrue(sent2.getDepEdgeMask().isPruned(0, 1)); // V V Right
        assertTrue(!sent2.getDepEdgeMask().isPruned(1, 0)); // V V Left
        assertTrue(sent2.getDepEdgeMask().isPruned(0, 2)); // V N Right
        assertTrue(sent2.getDepEdgeMask().isPruned(2, 0)); // N V Left
        assertTrue(!sent2.getDepEdgeMask().isPruned(4, 2)); // V N Left
        assertTrue(sent2.getDepEdgeMask().isPruned(2, 4)); // N V Right
    }

    @Test
    public void testUnknownTag() {
        AnnoSentenceCollection sents1 = new AnnoSentenceCollection();        
        AnnoSentence sent1 = new AnnoSentence();
        sent1.setWords(  Lists.getList("0", "1"));
        sent1.setPosTags(Lists.getList("N", "N"));
        sent1.setParents(new int[]    {  -1,   0 } );
        sents1.add(sent1);
        
        AnnoSentenceCollection sents2 = new AnnoSentenceCollection();        
        AnnoSentence sent2 = new AnnoSentence();
        sent2.setWords(  Lists.getList("0", "1"));
        sent2.setPosTags(Lists.getList("N", "NEW_TAG"));
        sent2.setParents(new int[]    { -1,   0 });
        sents2.add(sent2);
        
        PosTagDistancePruner pruner = new PosTagDistancePruner();
        pruner.train(null, sents1, null, null);
        pruner.annotate(sents2);        

        System.out.println(sents1);
        System.out.println(sents2);
        
        // Check that edges to or from an unknown tag (NEW_TAG) are NOT pruned. 
        assertTrue(!sent2.getDepEdgeMask().isPruned(0, 1));
        assertTrue(!sent2.getDepEdgeMask().isPruned(1, 0));
    }
    
    public static AnnoSentenceCollection getSents(int numSents) {
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        for (int i=0; i<numSents; i++) {
            sents.add(getPosTaggedSent((5 + i*2), (3 + i*2)));
        }
        return sents;
    }

    public static AnnoSentenceCollection getSents(int numSents, int numTokens, int numTags) {
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        for (int i=0; i<numSents; i++) {
            sents.add(getPosTaggedSent(numTokens, numTags));
        }
        return sents;
    }
    
    public static AnnoSentence getPosTaggedSent(int numTokens, int numTags) {
        AnnoSentence sent = new AnnoSentence();
        
        // Add words.
        List<String> words = new ArrayList<String>();
        for (int i=0; i<numTokens; i++) {
            words.add("word"+i);
        }
        sent.setWords(words);
        
        // Add tags
        List<String> tags = new ArrayList<String>();
        for (int i=0; i<numTokens; i++) {
            tags.add("tag"+(i%numTags));
        }
        sent.setPosTags(tags);
        
        // Add dep tree
        int[] parents = new int[numTokens];
        for (int i=0; i<numTokens; i++) {
            if (i % 2 == 1) {
                parents[i] = Math.max(0, i-3);
            } else if (i % 3 == 2) {
                parents[i] = Math.max(1, i-5);
            } else {
                parents[i] = i-1;
            }
        }
        sent.setParents(parents);
        
        return sent;
    }
    
}

package edu.jhu.hltcoe.model.dmv;

import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;

public class SimpleStaticDmvModelTest {

    @Test
    public void testTwoPosTagDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getTwoPosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        System.out.println(generator.getTreebank(10));
        System.out.println(dmvModel);
    }
    
    @Test
    public void testThreePosTagDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        System.out.println(dmvModel);
        DepTreebank treebank = generator.getTreebank(10);
        System.out.println(treebank);
        int maxSentenceLength = -1;
        int sumSentLen = 0;
        for (DepTree tree : treebank) {
            sumSentLen += tree.getNumTokens(); 
            if (tree.getNumTokens() > maxSentenceLength) {
                maxSentenceLength = tree.getNumTokens();
            }
        }
        System.out.println("maxSentenceLength = " + maxSentenceLength);
        System.out.println("avg sent len = " + (double) sumSentLen / treebank.size());
    }
    
    @Test
    public void testAltThreePosTagDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getAltThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        System.out.println(dmvModel);
        DepTreebank treebank = generator.getTreebank(400);
        System.out.println(treebank);
        int maxSentenceLength = -1;
        int sumSentLen = 0;
        for (DepTree tree : treebank) {
            sumSentLen += tree.getNumTokens(); 
            if (tree.getNumTokens() > maxSentenceLength) {
                maxSentenceLength = tree.getNumTokens();
            }
        }
        System.out.println("maxSentenceLength = " + maxSentenceLength);
        System.out.println("avg sent len = " + (double) sumSentLen / treebank.size());
    }

    @Test
    public void testFixedStopRandChildDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getFixedStopRandChild();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        System.out.println(dmvModel);
        DepTreebank treebank = generator.getTreebank(400);
        System.out.println(treebank);
        int maxSentenceLength = -1;
        int sumSentLen = 0;
        for (DepTree tree : treebank) {
            sumSentLen += tree.getNumTokens(); 
            if (tree.getNumTokens() > maxSentenceLength) {
                maxSentenceLength = tree.getNumTokens();
            }
        }
        System.out.println("maxSentenceLength = " + maxSentenceLength);
        System.out.println("avg sent len = " + (double) sumSentLen / treebank.size());
    }

}

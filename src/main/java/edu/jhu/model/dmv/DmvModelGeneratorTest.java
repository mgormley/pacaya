package edu.jhu.model.dmv;

import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.model.dmv.DmvModelGenerator.StochasticRealParamGenerator;

public class DmvModelGeneratorTest {

    @Test
    public void testTieredModelGeneration() {
        int numTiers = 3;
        int numTags = 7;
        DmvModel dmvModel = DmvModelGenerator.getTieredModel(numTiers, numTags, new StochasticRealParamGenerator(10.0));

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        dmvModel.convertLogToReal();
        System.out.println(dmvModel);
        dmvModel.convertRealToLog();
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

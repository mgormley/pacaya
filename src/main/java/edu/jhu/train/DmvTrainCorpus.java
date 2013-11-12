package edu.jhu.train;

import java.util.Arrays;
import java.util.Set;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;

public class DmvTrainCorpus implements TrainCorpus {
    
    private DepTreebank allTrees;
    private SentenceCollection allSentences;
    private boolean[] isLabeled;
    int numLabeled;
    
    public DmvTrainCorpus(DepTreebank treebank, double propSupervised) {
        allTrees = treebank;
        allSentences = treebank.getSentences();
        isLabeled = new boolean[treebank.size()];
        
        // Put all the unlabeled instances at the front.
        Arrays.fill(isLabeled, true);
        Arrays.fill(isLabeled, 0, (int)(isLabeled.length*(1.0 - propSupervised)), false);
        // TODO: switch this a randomly ordered set of booleans. The trick is dealing with cplex.getDuals() in 
        // the D-W relaxation (and RLT relaxation?).
        // isLabeled = getRandBools(treebank.size(), propSupervised);
        
        numLabeled = getNumTrue(isLabeled);
    }

    /**
     * For testing only.
     */
    public DmvTrainCorpus(SentenceCollection unlabeledSentences) {
        allSentences = unlabeledSentences;
        allTrees = null;
        isLabeled = new boolean[unlabeledSentences.size()];
        Arrays.fill(isLabeled, false);
        numLabeled = 0;
    }

    private static int getNumTrue(boolean[] bools) {
        int count = 0;
        for (int i=0; i<bools.length; i++) {
            if (bools[i]) {
                count++;
            }
        }
        return count;
    }

    private static boolean[] getRandBools(int numBooleans, double proportionTrue) {
        boolean[] bools = new boolean[numBooleans];
        for (int i=0; i<bools.length; i++) {
            bools[i] = Prng.nextDouble() < proportionTrue;
        }
        return bools;
    }
    
    public Alphabet<Label> getLabelAlphabet() {
        return allSentences.getLabelAlphabet();
    }

    public Set<Label> getVocab() {
        return allSentences.getVocab();
    }

    public SentenceCollection getSentences() {
        return allSentences;
    }

    public int size() {
        return allSentences.size();
    }

    public int getNumLabeled() { 
        return numLabeled;
    }
    
    public int getNumUnlabeled() {
        return size() - numLabeled;
    }

    public Sentence getSentence(int s) {
        return allSentences.get(s);
    }

    public boolean isLabeled(int s) {
        return isLabeled[s];
    }

    public DepTree getTree(int s) {
        if (isLabeled[s]) {
            return allTrees.get(s);
        } else {
            return null;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("All sentences:\n");
        sb.append(allSentences.toString());
        sb.append("All trees:\n");
        sb.append(allTrees.toString());
        return sb.toString();
    }
        
}

package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Alphabet;

public class DmvRule extends Rule {

    private boolean isLeftHead;

    public DmvRule(int parent, int leftChild, int rightChild, double score,
            Alphabet<String> ntAlphabet, Alphabet<String> lexAlphabet, boolean isLeftHead) {
        super(parent, leftChild, rightChild, score, ntAlphabet, lexAlphabet);
        this.isLeftHead = isLeftHead;
    }

    public boolean isLeftHead() {
        return isLeftHead;
    }

}

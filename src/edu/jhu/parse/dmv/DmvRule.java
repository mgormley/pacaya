package edu.jhu.parse.dmv;

import edu.jhu.data.Label;
import edu.jhu.parse.cky.Rule;
import edu.jhu.util.Alphabet;

public class DmvRule extends Rule {

    public enum DmvRuleType { ROOT, CHILD, STRUCTURAL, DECISION };
    
    private boolean isLeftHead;
    private DmvRuleType type;

    public DmvRule(int parent, int leftChild, int rightChild, double score,
            Alphabet<Label> ntAlphabet, Alphabet<Label> lexAlphabet, boolean isLeftHead, DmvRuleType type) {
        super(parent, leftChild, rightChild, score, ntAlphabet, lexAlphabet);
        this.isLeftHead = isLeftHead;
        this.type = type;
    }
    public boolean isLeftHead() {
        return isLeftHead;
    }

    public DmvRuleType getType() {
        return type;
    }

}

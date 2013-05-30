package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Alphabet;

/**
 * A unary or binary rule (or production) in a CNF grammar.
 * 
 * @author mgormley
 *
 */
public class Rule {

    public static final int LEXICAL_RULE = -1;
    public static final int UNARY_RULE = -2;
    
    private int parent;
    private int leftChild;
    private int rightChild;
    private double score;
    // TODO: Subclass: LogProb rule should have this: private double logProb;
    // TODO: Separate subclass should compute based on features of the sentence.
    
    private Alphabet<String> lexAlphabet;
    private Alphabet<String> ntAlphabet;

    public Rule(int parent, int leftChild, int rightChild, double score, Alphabet<String> ntAlphabet,
            Alphabet<String> lexAlphabet) {
        this.parent = parent;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.score = score;
        this.lexAlphabet = lexAlphabet;
        this.ntAlphabet = ntAlphabet;
    }

    public double getScore() {
        return score;
    }

    public int getParent() {
        return parent;
    }

    public boolean isBinary() {
        return rightChild >= 0;
    }
    
    public boolean isUnary() {
        return rightChild == UNARY_RULE;
    }
    
    public boolean isLexical() {
        return rightChild == LEXICAL_RULE;
    }

    public int getLeftChild() {
        return leftChild;
    }

    public int getRightChild() {
        return rightChild;
    }
    
    public String getParentStr() {
        return ntAlphabet.lookupObject(parent);
    }

    public String getLeftChildStr() {
        if (isLexical()) {
            return lexAlphabet.lookupObject(leftChild);
        } else {
            return ntAlphabet.lookupObject(leftChild);
        }        
    }

    public String getRightChildStr() {
        if (rightChild < 0) {
            return null;
        } else {
            return ntAlphabet.lookupObject(rightChild);
        }        
    }
    
    @Override
    public String toString() {
        return "Rule [parent=" + getParentStr() + ", leftChild=" + getLeftChildStr()
                + ", rightChild=" + getRightChildStr() + ", score=" + score + "]";
    }
}

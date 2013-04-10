package edu.jhu.hltcoe.model.dmv;

import java.util.HashSet;
import java.util.Set;

import util.Alphabet;
import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.model.Model;
import depparsing.extended.DepProbMatrix;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.math.LabeledMultinomial;

public class DmvModel extends DepProbMatrix implements Model {

    public enum Lr { 
        LEFT, RIGHT;
        public int getAsInt() {
            return (this == LEFT) ? Constants.LEFT : Constants.RIGHT;
        } 
        public String toString() {
            return (this == LEFT) ? "l" : "r";
        }
    }

    private static final long serialVersionUID = -8847338812640416552L;
    public static final boolean[] ADJACENTS = new boolean[]{true, false};
    
    public DmvModel(Alphabet<Label> tagAlphabet) {
        super(tagAlphabet, 2, 1);
    }

    // ------------------ Alphabet -------------------

    public Set<Label> getVocab() {
        Set<Label> children = new HashSet<Label>();
        for (int i=0; i<tagAlphabet.size(); i++) {
            children.add(tagAlphabet.lookupIndex(i));
        }
        return children;
    }
    
    // ------------------ Root -------------------

    public double getRootWeight(Label childLabel) {
        int c = tagAlphabet.lookupObject(childLabel);
        return root[c];
    }

    public LabeledMultinomial<Label> getRootWeights() {
        Set<Label> children = getVocab();
        LabeledMultinomial<Label> mult = new LabeledMultinomial<Label>();
        for (Label childLabel : children) {
            int c = tagAlphabet.lookupObject(childLabel);
            mult.put(childLabel, root[c]);
        }
        return mult;
    }
    
    public void putRootWeight(Label childLabel, double logProb) {
        int c = tagAlphabet.lookupObject(childLabel);
        root[c] = logProb;
    }
    
    // ------------------ Child -------------------

    @Deprecated
    public LabeledMultinomial<Label> getChildWeights(Label label, String lr) {
        return getChildWeights(label, lr.equals("l") ? Lr.LEFT : Lr.RIGHT);
    }
    
    public LabeledMultinomial<Label> getChildWeights(Label label, Lr lr) {
        
        // TODO: this should be a method on Alphabet.
        Set<Label> children = getVocab();
        
        LabeledMultinomial<Label> mult = new LabeledMultinomial<Label>();
        for (Label childLabel : children) {
            int c = tagAlphabet.lookupObject(childLabel);
            int p = tagAlphabet.lookupObject(label);
            int dir = lr.getAsInt();
            int cv = 0;
            mult.put(childLabel, child[c][p][dir][cv]);
        }
        return mult;
    }

    public double getChildWeight(Label parentLabel, Lr lr, Label childLabel) {
        int c = tagAlphabet.lookupObject(childLabel);
        int p = tagAlphabet.lookupObject(parentLabel);
        int dir = lr.getAsInt();
        int cv = 0;
        return child[c][p][dir][cv];
    }

    @Deprecated
    public void putChildWeight(Label parentLabel, String lr, Label childLabel, double logProb) {
        putChildWeight(parentLabel, lr.equals("l") ? Lr.LEFT : Lr.RIGHT, childLabel, logProb);
    }
    
    public void putChildWeight(Label parentLabel, Lr lr, Label childLabel, double logProb) {
        int c = tagAlphabet.lookupObject(childLabel);
        int p = tagAlphabet.lookupObject(parentLabel);
        int dir = lr.getAsInt();
        int cv = 0;
        child[c][p][dir][cv] = logProb;
    }
    
    // ------------------ Decision -------------------
    @Deprecated
    public double getStopWeight(Label parent, String lr, boolean adjacent) {
        return getStopWeight(parent, lr.equals("l") ? Lr.LEFT : Lr.RIGHT, adjacent);
    }
    
    public double getStopWeight(Label parent, Lr lr, boolean adjacent) {
        int pid = tagAlphabet.lookupObject(parent);
        int dir = lr.getAsInt();
        // Note this is backwards from how adjacency is encoded for the ILPs
        int kids = adjacent ? 0 : 1;        
        return decision[pid][dir][kids][Constants.END];
    }
    
    public double getContWeight(Label parent, Lr lr, boolean adjacent) {
        int pid = tagAlphabet.lookupObject(parent);
        int dir = lr.getAsInt();
        // Note this is backwards from how adjacency is encoded for the ILPs
        int kids = adjacent ? 0 : 1;        
        return decision[pid][dir][kids][Constants.CONT];
    }

    @Deprecated
    public void putStopProb(Label parent, String lr, boolean adjacent, double prob) {
        putStopProb(parent, lr.equals("l") ? Lr.LEFT : Lr.RIGHT, adjacent, prob);
    }

    public void putStopProb(Label parent, Lr lr, boolean adjacent, double prob) {
        int pid = tagAlphabet.lookupObject(parent);
        int dir = lr.getAsInt();
        // Note this is backwards from how adjacency is encoded for the ILPs
        int kids = adjacent ? 0 : 1;        
        decision[pid][dir][kids][Constants.END] = prob;
        decision[pid][dir][kids][Constants.CONT] = 1.0 - prob;
    }

    public void fillStopProbs(double prob) {
        for (Label parent : getVocab()) {
            for (Lr lr : Lr.values()) {
                for (boolean adjacent : ADJACENTS) {
                    putStopProb(parent, lr, adjacent, prob);
                }
            }
        }
    }

}

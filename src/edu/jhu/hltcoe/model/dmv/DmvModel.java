package edu.jhu.hltcoe.model.dmv;

import java.util.HashSet;
import java.util.Set;

import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOneToOne;
import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.math.Multinomials;

public class DmvModel implements Model {
    
    // Rules that correspond to probabilities in the model.
    public final double[] root; // Indexed by child.
    public final double[][][] child; // Indexed by child, parent, and direction.
    public final double[][][][] decision; // Indexed by parent, direction, valence (0 or 1), and STOP/CONT.    
    
    private final int numTags;
    private final Alphabet<Label> tagAlphabet;
    
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
        this.numTags = tagAlphabet.size();
        this.tagAlphabet = tagAlphabet;
        
        this.root = new double[numTags];
        this.child = new double[numTags][numTags][2];
        this.decision = new double[numTags][2][2][2];
    }

    public void copyFrom(DmvModel other) {
        for (int c=0; c<numTags; c++) {
            this.root[c] = other.root[c];
            for (int dir=0; dir<2; dir++) {
                for (int p=0; p<numTags; p++) {
                    this.child[c][p][dir] = other.child[c][p][dir];
                }
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        this.decision[c][dir][val][sc] = other.decision[c][dir][val][sc];
                    }
                }
            }
        }
    }

    // ------------------ Alphabet -------------------

    public Set<Label> getVocab() {
        Set<Label> children = new HashSet<Label>();
        for (int i=0; i<tagAlphabet.size(); i++) {
            children.add(tagAlphabet.lookupObject(i));
        }
        return children;
    }

    public Alphabet<Label> getTagAlphabet() {
        return tagAlphabet;
    }
    
    // ------------------ Root -------------------

    public double getRootWeight(Label childLabel) {
        int c = tagAlphabet.lookupIndex(childLabel);
        return root[c];
    }

    public LabeledMultinomial<Label> getRootWeights() {
        Set<Label> children = getVocab();
        LabeledMultinomial<Label> mult = new LabeledMultinomial<Label>();
        for (Label childLabel : children) {
            int c = tagAlphabet.lookupIndex(childLabel);
            mult.put(childLabel, root[c]);
        }
        return mult;
    }
    
    public void putRootWeight(Label childLabel, double logProb) {
        int c = tagAlphabet.lookupIndex(childLabel);
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
            int c = tagAlphabet.lookupIndex(childLabel);
            int p = tagAlphabet.lookupIndex(label);
            int dir = lr.getAsInt();
            mult.put(childLabel, child[c][p][dir]);
        }
        return mult;
    }

    public double getChildWeight(Label parentLabel, Lr lr, Label childLabel) {
        int c = tagAlphabet.lookupIndex(childLabel);
        int p = tagAlphabet.lookupIndex(parentLabel);
        int dir = lr.getAsInt();
        return child[c][p][dir];
    }

    @Deprecated
    public void putChildWeight(Label parentLabel, String lr, Label childLabel, double logProb) {
        putChildWeight(parentLabel, lr.equals("l") ? Lr.LEFT : Lr.RIGHT, childLabel, logProb);
    }
    
    public void putChildWeight(Label parentLabel, Lr lr, Label childLabel, double logProb) {
        int c = tagAlphabet.lookupIndex(childLabel);
        int p = tagAlphabet.lookupIndex(parentLabel);
        int dir = lr.getAsInt();
        child[c][p][dir] = logProb;
    }
    
    // ------------------ Decision -------------------
    @Deprecated
    public double getStopWeight(Label parent, String lr, boolean adjacent) {
        return getStopWeight(parent, lr.equals("l") ? Lr.LEFT : Lr.RIGHT, adjacent);
    }
    
    public double getStopWeight(Label parent, Lr lr, boolean adjacent) {
        int pid = tagAlphabet.lookupIndex(parent);
        int dir = lr.getAsInt();
        // Note this is backwards from how adjacency is encoded for the ILPs
        int kids = adjacent ? 0 : 1;        
        return decision[pid][dir][kids][Constants.END];
    }
    
    public double getContWeight(Label parent, Lr lr, boolean adjacent) {
        int pid = tagAlphabet.lookupIndex(parent);
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
        int pid = tagAlphabet.lookupIndex(parent);
        int dir = lr.getAsInt();
        // Note this is backwards from how adjacency is encoded for the ILPs
        int kids = adjacent ? 0 : 1;        
        decision[pid][dir][kids][Constants.END] = prob;
        decision[pid][dir][kids][Constants.CONT] = 1.0 - prob;
    }

    /** Sets all the stop probabilities to prob. THIS IS SLOW. */
    public void fillStopProbs(double prob) {
        for (Label parent : getVocab()) {
            for (Lr lr : Lr.values()) {
                for (boolean adjacent : ADJACENTS) {
                    putStopProb(parent, lr, adjacent, prob);
                }
            }
        }
    }
    
    // ------------------ Apply Methods -------------------

    public void fill(double value) {
        fillRoot(value);
        fillChild(value);
        fillStop(value);
    }

    public void fillRoot(double value) {
        for (int c=0; c<numTags; c++) {
            this.root[c] = value;
        }
    }

    public void fillChild(double value) {
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    this.child[c][p][dir] = value;
                }
            }
        }
    }

    public void fillStop(double value) {
        for (int c=0; c<numTags; c++) {
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        this.decision[c][dir][val][sc] = value;
                    }
                }
            }
        }
    }
    
    public void apply(LambdaOneToOne<Double,Double> lambda) {
        applyRoot(lambda);
        applyChild(lambda);
        applyStop(lambda);
    }

    public void applyRoot(LambdaOneToOne<Double,Double> lambda) {
        for (int c=0; c<numTags; c++) {
            this.root[c] = lambda.call(this.root[c]);
        }
    }

    public void applyChild(LambdaOneToOne<Double,Double> lambda) {
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    this.child[c][p][dir] = lambda.call(this.child[c][p][dir]);
                }
            }
        }
    }

    public void applyStop(LambdaOneToOne<Double,Double> lambda) {
        for (int c=0; c<numTags; c++) {
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        this.decision[c][dir][val][sc] = lambda.call(this.decision[c][dir][val][sc]);
                    }
                }
            }
        }
    }
    
    public void addConstant(final double addend) {
        apply(new LambdaOneToOne<Double, Double>() {
            public Double call(Double value) {
                return value + addend;
            }
        });
    }

    /** 
     * Log-adds the logAddend to every parameter of the model. 
     */
    public void logAddConstant(final double logAddend) {
        apply(new LambdaOneToOne<Double, Double>() {
            public Double call(Double value) {
                return Utilities.logAdd(value, logAddend);
            }
        });
    }

    public void convertRealToLog() {
        apply(new LambdaOneToOne<Double, Double>() {
            public Double call(Double value) {
                return Utilities.log(value);
            }
        });
    }
    
    public void convertLogToReal() {
        apply(new LambdaOneToOne<Double, Double>() {
            public Double call(Double value) {
                return Utilities.exp(value);
            }
        });
    }

    public void setRandom() {
        LambdaOneToOne<Double, Double> lambda = new LambdaOneToOne<Double, Double>() {
            @Override
            public Double call(Double obj) {
                return Prng.nextDouble();
            }
            
        };
        applyRoot(lambda);
        applyChild(lambda);
        applyStop(lambda);
    }

    public void normalize() {
        // Normalize the root parameters.
        Multinomials.normalizeProps(root);
        
        // Normalize the stop/continue parameters.
        for (int c=0; c<numTags; c++) {
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        Multinomials.normalizeProps(decision[c][dir][val]);
                    }
                }
            }
        }
        
        // Normalize the child parameters.
        //
        // Because the indexing here is poorly ordered, we need to compute all
        // the sums and then divide them out.
        // 
        // Compute all the normalizing constants.
        double[][] sums = new double[numTags][2]; // Indexed by parent and direction.
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    sums[p][dir] += child[c][p][dir];
                }
            }
        }
        // Subtract off the normalizing constants.
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    if (sums[p][dir] != 0) {
                        child[c][p][dir] /= sums[p][dir];
                    } else {
                        child[c][p][dir] = 1.0 / numTags;
                    }
                    assert(!Double.isNaN(child[c][p][dir]));
                }
            }
        }        
    }
    
    public void logNormalize() {
        // Normalize the root parameters.
        Multinomials.normalizeLogProps(root);
        
        // Normalize the stop/continue parameters.
        for (int c=0; c<numTags; c++) {
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        Multinomials.normalizeLogProps(decision[c][dir][val]);
                    }
                }
            }
        }
        
        // Normalize the child parameters.
        //
        // Because the indexing here is poorly ordered, we need to compute all
        // the log sums and then subtract them off. 
        // 
        // Compute all the normalizing constants.
        double[][] logSums = new double[numTags][2]; // Indexed by parent and direction.
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    logSums[p][dir] = Utilities.logAdd(logSums[p][dir], child[c][p][dir]);
                }
            }
        }
        // Subtract off the normalizing constants.
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    child[c][p][dir] -= logSums[p][dir];
                    assert(!Double.isNaN(child[c][p][dir]));
                }
            }
        }
    }

    public void assertLogNormalized(double delta) {

        // Normalize the root parameters.
        Multinomials.assertLogNormalized(root, delta);
        
        // Normalize the stop/continue parameters.
        for (int c=0; c<numTags; c++) {
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        Multinomials.assertLogNormalized(decision[c][dir][val], delta);
                    }
                }
            }
        }
        
        // Normalize the child parameters.
        //
        // Because the indexing here is poorly ordered, we need to compute all
        // the log sums manually.
        // 
        // Compute all the normalizing constants.
        double[][] logSums = new double[numTags][2]; // Indexed by parent and direction.
        for (int c=0; c<numTags; c++) {
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    logSums[p][dir] = Utilities.logAdd(logSums[p][dir], child[c][p][dir]);
                }
            }
        }
        // Assert that each sum is within delta of 0.0.
        for (int p=0; p<numTags; p++) {
            for (int dir=0; dir<2; dir++) {
                assert(Utilities.equals(0.0, logSums[p][dir], delta)); 
            }
        }
    }
    
}

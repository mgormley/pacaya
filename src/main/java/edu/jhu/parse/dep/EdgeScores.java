package edu.jhu.parse.dep;

import java.util.Arrays;

import edu.jhu.autodiff.Tensor;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;
import edu.jhu.util.semiring.Algebra;

/**
 * Edge scores for a dependency parser.
 * 
 * @author mgormley
 */
public class EdgeScores {
    
    public double[] root;
    public double[][] child;
    
    public EdgeScores(int n, double value) {
        this.root = new double[n];
        this.child = new double[n][n];
        DoubleArrays.fill(root, value);
        DoubleArrays.fill(child, value);
    }
    
    public EdgeScores(double[] root, double[][] child) {
        this.root = root;
        this.child = child;
    }
    
    public double getScore(int p, int c) {
        return (p == -1) ? root[c] : child[p][c];
    }

    public void setScore(int p, int c, double val) {
        if (p == -1) {
            root[c] = val;
        } else {
            child[p][c] = val;
        }
    }
    
    /** Safely checks whether the child array contains a value -- ignoring diagonal entries. */
    public static boolean childContains(double[][] child, double value, double delta) {
        for (int i=0; i<child.length; i++) {
            for (int j=0; j<child.length; j++) {
                if (i == j) { continue; }
                if (Primitives.equals(child[i][j], value, delta)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "EdgeScores [root=" + Arrays.toString(root) + ", child=" + Arrays.deepToString(child) + "]";
    }

    public void apply(LambdaUnaryOpDouble lambda) {
        for (int i=0; i<root.length; i++) {
            root[i] = lambda.call(root[i]);
        }
        for (int i=0; i<child.length; i++) {
            for (int j=0; j<child.length; j++) {
                if (i == j) { continue; }
                child[i][j] = lambda.call(child[i][j]);
            }
        }
    }    

    /** Convert an EdgeScores object to a Tensor, where the wall node is indexed as position n+1 in the Tensor. 
     * @param s TODO*/
    public static Tensor edgeScoresToTensor(EdgeScores es, Algebra s) {
        int n = es.child.length;
        Tensor m = new Tensor(s, n, n);
        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                if (p == c) { continue; }
                int pp = getTensorParent(p, c);
                m.set(es.getScore(p, c), pp, c);
            }
        }
        return m;
    }
    
    /** Convert a Tensor object to an EdgeScores, where the wall node is indexed as position n+1 in the Tensor. */
    public static EdgeScores tensorToEdgeScores(Tensor t) {
        if (t.getDims().length != 2) {
            throw new IllegalArgumentException("Tensor must be an nxn matrix.");
        }
        int n = t.getDims()[1];
        EdgeScores es = new EdgeScores(n, 0);        
        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                if (p == c) { continue; }
                int pp = getTensorParent(p, c);
                es.setScore(p, c, t.get(pp, c));
            }
        }
        return es;
    }

    public Tensor toTensor(Algebra s) {
        return edgeScoresToTensor(this, s);
    }

    /** In the tensor, we use the diagonal as the scores for the wall node. */
    public static int getTensorParent(int p, int c) {
        if (p == c) {
            throw new IllegalArgumentException("No entry defined for p == c case.");
        }
        return (p == -1) ? c : p;
    }

    /**
     * Combines a set of edge weights represented as wall and child weights into a single set of
     * weights. The combined weights are indexed such that the wall has index 0 and the tokens of
     * the sentence are 1-indexed.
     * 
     * @param fracRoot The edge weights from the wall to each child.
     * @param fracChild The edge weights from parent to child.
     * @return The combined weights.
     */
    public static double[][] combine(double[] fracRoot, double[][] fracChild) {
        int n = fracChild.length + 1;
        double[][] scores = new double[n][n];
        for (int p=0; p<n; p++) { 
            for (int c=0; c<n; c++) {
                if (c == 0) {
                    scores[p][c] = Double.NEGATIVE_INFINITY;
                } else if (p == 0 && c > 0) {
                    scores[p][c] = fracRoot[c-1];
                } else {
                    scores[p][c] = fracChild[p-1][c-1];
                }
            }
        }
        return scores;
    }
    
}
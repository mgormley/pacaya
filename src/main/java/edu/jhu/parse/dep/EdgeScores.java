package edu.jhu.parse.dep;

import java.util.Arrays;

import edu.jhu.autodiff.Tensor;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;

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

    /** Convert an EdgeScores object to a Tensor, where the wall node is indexed as position n+1 in the Tensor. */
    public static Tensor edgeScoresToTensor(EdgeScores es) {
        int n = es.child.length;
        Tensor m = new Tensor(n, n);
        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                if (p == c) { continue; }
                int pp = getTensorParent(n, p, c);
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
                int pp = getTensorParent(n, p, c);
                es.setScore(p, c, t.get(pp, c));
            }
        }
        return es;
    }

    public Tensor toTensor() {
        return edgeScoresToTensor(this);
    }

    /** In the tensor, we use the diagonal as the scores for the wall node. */
    public static int getTensorParent(int n, int p, int c) {
        if (p == c) {
            throw new IllegalArgumentException("No entry defined for p == c case.");
        }
        return (p == -1) ? c : p;
    }
    
}
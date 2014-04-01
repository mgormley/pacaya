package edu.jhu.parse.dep;

import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;

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
}
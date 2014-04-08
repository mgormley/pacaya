package edu.jhu.parse.dep;

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
}
/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.util.Utilities;

public class RelaxedDepTreebank {
    private double[][] fracRoots;
    private double[][][] fracChildren;

    public RelaxedDepTreebank(double[][] fracRoots, double[][][] fracChildren) {
        this.fracRoots = fracRoots;
        this.fracChildren = fracChildren;
    }

    public double[][] getFracRoots() {
        return fracRoots;
    }

    public double[][][] getFracChildren() {
        return fracChildren;
    }

    public double getPropFracArcs() {
        int numFractional = 0;
        int numArcs = 0;
        for (int s = 0; s < fracRoots.length; s++) {
            double[] fracRoot = fracRoots[s];
            double[][] fracChild = fracChildren[s];
            
            if (fracRoot == null) {
                continue;
            }
            
            for (int child = 0; child < fracRoot.length; child++) {
                if (isFractional(fracRoot[child])) {
                    numFractional++;
                }
                numArcs++;
            }
            for (int parent = 0; parent < fracChild.length; parent++) {
                for (int child = 0; child < fracChild[parent].length; child++) {
                    if (isFractional(fracChild[parent][child])) {
                        numFractional++;
                    }
                    numArcs++;
                }
            }
        }
        return numFractional / numArcs;
    }

    private boolean isFractional(double arcWeight) {
        if (Utilities.equals(arcWeight, 0.0, 1e-9) || Utilities.equals(arcWeight, 1.0, 1e-9)) {
            return false;
        }
        return true;
    }
}
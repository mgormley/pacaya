/**
 * 
 */
package edu.jhu.gridsearch.dmv;

import edu.jhu.data.Sentence;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.util.Utilities;

public class RelaxedDepTreebank {
    private double[][] fracRoots;
    private double[][][] fracChildren;

    public RelaxedDepTreebank(DmvTrainCorpus corpus) {
        fracRoots = new double[corpus.size()][];
        fracChildren = new double[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                fracRoots[s] = null;
                fracChildren[s] = null;
            } else {
                Sentence sentence = corpus.getSentence(s);
                fracRoots[s] = new double[sentence.size()];
                fracChildren[s] = new double[sentence.size()][sentence.size()];
            }
        }
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
        if (numArcs == 0) {
            return 0.0;
        } else {
            return (double) numFractional / numArcs;
        }
    }

    private boolean isFractional(double arcWeight) {
        if (Utilities.equals(arcWeight, 0.0, 1e-9) || Utilities.equals(arcWeight, 1.0, 1e-9)) {
            return false;
        }
        return true;
    }

    public double[][] getFracRoots() {
        return fracRoots;
    }

    public double[][][] getFracChildren() {
        return fracChildren;
    }

    public void setFracRoots(double[][] fracRoots) {
        this.fracRoots = fracRoots;
    }

    public void setFracChildren(double[][][] fracChildren) {
        this.fracChildren = fracChildren;
    }

    public int size() {
        return fracRoots.length;
    }
    
}
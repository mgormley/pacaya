package edu.jhu.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.PipelineRunner;
import edu.jhu.data.DepTreebank;
import edu.jhu.gridsearch.dmv.IndexedDmvModel.Rhs;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.util.Utilities;

public class DmvObjective {
    
    public static class DmvObjectivePrm {
        // Parameters for shiny edges posterior constraint.
        public boolean universalPostCons = false;
        public double universalMinProp = 0.8;
        public ShinyEdges shinyEdges = null;
    }

    private static final Logger log = Logger.getLogger(DmvObjective.class);
    private static final double EQUALITY_TOLERANCE = 1e-8;
    private DmvObjectivePrm prm;
    private IndexedDmvModel idm;
    
    public DmvObjective(DmvObjectivePrm prm, IndexedDmvModel idm) {
        this.prm = prm;
        this.idm = idm;
        if (prm.universalPostCons && prm.shinyEdges == null) {
            // Default to the universal linguistic constraint.
            prm.shinyEdges = ShinyEdges.getUniversalSet(idm.getLabelAlphabet());
        }
    }

    public double computeTrueObjective(double[][] logProbs, double[][] featCounts) {
        if (prm.universalPostCons) {
            double propShiny = propShiny(featCounts);
            log.info("Proporition shiny edges: " + propShiny);
            if (propShiny < prm.universalMinProp) {
                log.info("Proporition shiny edges < min prop");
                return Double.NEGATIVE_INFINITY;
            }
        }
        double quadObj = 0.0;
        for (int c = 0; c < logProbs.length; c++) {
            for (int m = 0; m < logProbs[c].length; m++) {
                if (!Utilities.equals(featCounts[c][m], 0.0, EQUALITY_TOLERANCE)) {
                    quadObj += (logProbs[c][m] * featCounts[c][m]);
                }
                assert (!Double.isNaN(quadObj));
            }
        }
        return quadObj;
    }
    
    public double computeTrueObjective(double[][] logProbs, int[][] featCounts) {
        if (prm.universalPostCons) {
            double propShiny = propShiny(featCounts);
            log.info("Proporition shiny edges: " + propShiny);
            if (propShiny < prm.universalMinProp) {
                log.info("Proporition shiny edges < min prop");
                return Double.NEGATIVE_INFINITY;
            }
        }
        double quadObj = 0.0;
        for (int c = 0; c < logProbs.length; c++) {
            for (int m = 0; m < logProbs[c].length; m++) {
                if (featCounts[c][m] > 0) {
                    quadObj += (logProbs[c][m] * featCounts[c][m]);
                }
                assert (!Double.isNaN(quadObj));
            }
        }
        return quadObj;
    }

    // TODO: this is untested.
    public double propShiny(double[][] featCounts) {
        double numShiny = 0;
        double numEdges = 0;
        for (int c = 0; c < featCounts.length; c++) {
            Rhs rhs = idm.getRhs(c);
            if (rhs.get(0) == IndexedDmvModel.CHILD || rhs.get(0) == IndexedDmvModel.ROOT) {
                int parent = (rhs.get(0) == IndexedDmvModel.CHILD) ? rhs.get(1) : prm.shinyEdges.getWallIdx();
                for (int child = 0; child < featCounts[c].length; child++) {
                    if (prm.shinyEdges.isShinyEdge[parent][child]) {
                        numShiny += featCounts[c][child];
                    }
                    numEdges += featCounts[c][child];
                }
            }
        }
        return numShiny / numEdges;
    }
    
    public double propShiny(int[][] featCounts) {
        double numShiny = 0;
        double numEdges = 0;
        for (int c = 0; c < featCounts.length; c++) {
            Rhs rhs = idm.getRhs(c);
            if (rhs.get(0) == IndexedDmvModel.CHILD || rhs.get(0) == IndexedDmvModel.ROOT) {
                int parent = (rhs.get(0) == IndexedDmvModel.CHILD) ? rhs.get(1) : prm.shinyEdges.getWallIdx();
                for (int child = 0; child < featCounts[c].length; child++) {
                    if (prm.shinyEdges.isShinyEdge[parent][child]) {
                        numShiny += featCounts[c][child];
                    }
                    numEdges += featCounts[c][child];
                }
            }
        }
        return numShiny / numEdges;
    }

    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        int[][] featCounts = idm.getTotFreqCm(treebank);
        
        return computeTrueObjective(logProbs, featCounts);
    }

    public Double computeTrueObjective(DmvModel model, DepTreebank treebank) {
        double[][] logProbs = idm.getCmLogProbs(model);
        return computeTrueObjective(logProbs, treebank);
    }

}

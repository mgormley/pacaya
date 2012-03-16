package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.model.dmv.DmvModel;

public class DmvDantzigWolfeRelaxation {

    private DmvBounds bounds;
    
    public DmvDantzigWolfeRelaxation(DmvBounds bounds) {
        this.bounds = bounds;
    }

    public void updateBounds(DmvBounds bounds) {
        // TODO Auto-generated method stub
        
    }

    public double solve() {
        // TODO Auto-generated method stub
        return 0.0;
    }

    public DepTreebank getProjectedParses() {

//        double[][][] fractionalParses;
//        for (int s=0; s<fractionalParses.length; s++) {
//            double[][] weights = fractionalParses[s];
//            // For non-projective case we'd do something like this.
//            //            int[] parents = new int[weights.length];
//            //            Edmonds eds = new Edmonds();
//            //            CompleteGraph graph = new CompleteGraph(weights);
//            //            eds.getMaxBranching(graph, 0, parents);
//            // For projective case we use a DP parser
//        }
        return null;
    }

    public DmvModel getProjectedModel() {
        // TODO Auto-generated method stub
        return null;
    }

    public double computeTrueObjective(DmvModel model, DepTreebank treebank) {
        // TODO Auto-generated method stub
        return 0;
    }

}

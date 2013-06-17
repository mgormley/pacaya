package edu.jhu.hltcoe.gm;

import java.util.ArrayList;

import edu.jhu.hltcoe.gm.BipartiteGraph.Node;
import edu.jhu.hltcoe.gm.FactorGraph.Var;

/**
 * Factor graph.
 * 
 * @author mgormley
 *
 */
public class FactorGraph {
    
    /**
     * A factor in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class Factor extends Node {
        
        /** The set of variables in this factor. */
        private VarSet vars;
        /** The values of each entry in this factor. */
        // TODO: we could use the ordering used by libDAI to represent.
        // TODO: should we instead represent this as a sparse factor? what are the tradeoffs?
        private double[] values;
        
        

        /** 
         * Gets the marginal distribution over a subset of the variables in this factor, optionally normalized.
         * 
         * @param vars The subset of variables for the marginal distribution. This will sum over all variables not in this set.
         * @param normalize Whether to normalize the resulting distribution.
         * @return The marginal distribution represented as log-probabilities.
         */
        public Factor getMarginal(VarSet vars, boolean normalize) {
            return null; // TODO:
        }

        /**
         * Gets the value of the c'th configuration of the variables.
         */
        public double getValue(int c) {
            // TODO Auto-generated method stub
            return 0;
        }

        /**
         * Gets the number of configurations possible for this factor.
         */
        public int getNumConfigs() {
            // TODO Auto-generated method stub
            return 0;
        }
        
    }
    
    /**
     * A variable in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class Var extends Node {
        
        public enum VarType {
            /** Observed variables will always be observed at training and test time. */
            OBSERVED,
            /** Latent variables will never be observed at training or at test time. */
            LATENT, 
            /** Predicated variables are observed at training time, but not at test time. */
            PREDICTED 
        };

        private VarType type;
        
    }
    
    /**
     * A subset of the variables.
     * 
     * @author mgormley
     *
     */
    public static class VarSet {

        public VarSet(Var var) {
            // TODO Auto-generated constructor stub
        }
        
    }
    
    private BipartiteGraph graph;
    private ArrayList<Factor> factors;
    private ArrayList<Var> vars;
    
}

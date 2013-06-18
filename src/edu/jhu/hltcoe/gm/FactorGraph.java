package edu.jhu.hltcoe.gm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.hltcoe.gm.BipartiteGraph.Edge;
import edu.jhu.hltcoe.gm.BipartiteGraph.Node;
import edu.jhu.hltcoe.util.math.Multinomials;
import edu.jhu.hltcoe.util.math.Vectors;

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
    // TODO: maybe this shouldn't extend Node.
    public static class Factor extends Node {
        
        /** The set of variables in this factor. */
        private VarSet vars;
        /** The values of each entry in this factor. */
        // TODO: Are these in the log-domain??
        // TODO: we could use the ordering used by libDAI to represent.
        // TODO: should we instead represent this as a sparse factor? what are the tradeoffs?
        private double[] values;
        
        public Factor(VarSet vars) {
            int numConfigs = getNumConfigs(vars);
            this.vars = vars;
            this.values = new double[numConfigs];
        }

        /**
         * Gets the number of possible configurations for this set of variables.
         */
        public static int getNumConfigs(VarSet vars) {
            if (vars.size() == 0) {
                return 0;
            }
            int numConfigs = 1;
            for (Var var : vars) {
                numConfigs *= var.getNumStates();
            }
            return numConfigs;
        }

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

        /** Set all the values to the given value. */
        public void set(double value) {
            Arrays.fill(values, value);
        }
        
        /** Add the addend to each value. */
        public void add(double addend) {
            Vectors.add(values, addend);
        }
        
        /** Scale each value by lambda. */
        public void scale(double lambda) {
            Vectors.scale(values, lambda);
        }
        
        /** Normalizes the values. */
        public void normalize() {
            Multinomials.normalizeLogProps(values);
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

        /** The type of variable. */
        private VarType type;
        /** The unique index for this variable. */
        private int id;
        /** The number of states that this variable can take on. */
        private int numStates;
        /** The unique name of this variable. */
        private String name;
        /** State names. */
        private ArrayList<String> stateNames;
        
        public int getNumStates() {
            return numStates;
        }
        
    }
    
    /**
     * A subset of the variables.
     * 
     * Implementation Note: Internally, we use a simple ArrayList to store the
     * variables. This was inspired by libDAI which makes an analygous design
     * choice for the representation of a variable set which we expect to be
     * quite small.
     * 
     * @author mgormley
     * 
     */
    public static class VarSet extends SmallSet<Var> {

        public VarSet(Var var) {
            super(1);
            add(var);
        }
                
    }
    
    private BipartiteGraph graph;
    private ArrayList<Factor> factors;
    private ArrayList<Var> vars;
    
    public List<Edge> getEdges() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNumEdges() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Edge getEdge(int i) {
        // TODO Auto-generated method stub
        return null;
    }
    
}

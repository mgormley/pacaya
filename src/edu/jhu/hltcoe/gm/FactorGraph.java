package edu.jhu.hltcoe.gm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.jhu.hltcoe.gm.BipartiteGraph.Edge;
import edu.jhu.hltcoe.gm.BipartiteGraph.Node;
import edu.jhu.hltcoe.gm.FactorGraph.Factor;
import edu.jhu.hltcoe.gm.FactorGraph.Var;
import edu.jhu.hltcoe.util.Utilities;
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
     * An edge in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class FgEdge extends Edge {
        
        // TODO: write hashCode and equals.
        public FgEdge() {
            // TODO: 
        }
        
        public Factor getFactor() {
            Node n1 = this.getParent();
            Node n2 = this.getChild();
            Factor factor;
            Var var;
            if (n1 instanceof Factor) {
                factor = (Factor) n1;
                var = (Var) n2;
            } else {
                factor = (Factor) n2;
                var = (Var) n1;
            }
            return factor;
        }
        
        public Var getVar() {
            Node n1 = this.getParent();
            Node n2 = this.getChild();
            Factor factor;
            Var var;
            if (n1 instanceof Factor) {
                factor = (Factor) n1;
                var = (Var) n2;
            } else {
                factor = (Factor) n2;
                var = (Var) n1;
            }
            return var;
        }

        public int getId() {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isVarToFactor() {
            // TODO Auto-generated method stub
            return false;
        }
        
    }
    
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
            this.vars = vars;
            int numConfigs = vars.getNumConfigs();
            this.values = new double[numConfigs];
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
        
        public VarSet getVars() {
            return vars;
        }

        /**
         * Gets the value of the c'th configuration of the variables.
         */
        public double getValue(int c) {
            return values[c];
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
            Multinomials.normalizeProps(values);
        }

        /** Normalizes the values. */
        public void logNormalize() {
            Multinomials.normalizeLogProps(values);
        }
        
        /**
         * Adds each entry in the given factor to this factor.
         */
        public void add(Factor f) {
            if (this.vars.equals(f.vars)) {
                for (int i=0; i<values.length; i++) {
                    values[i] += f.values[i];
                }
            } else if (this.vars.isSuperset(f.vars)) {
                
            } else {
                throw new IllegalStateException("The varsets must be equal.");
            }
        }

        /**
         * Multiplies each entry in the given factor to this factor.
         */
        public void prod(Factor f) {
            if (!this.vars.equals(f.vars)) {
                throw new IllegalStateException("The varsets must be equal.");
            }
                        
            for (int i=0; i<values.length; i++) {
                values[i] *= f.values[i];
            }
        }
        
        /**
         * Log-adds each entry in the given factor to this factor.
         */
        public void logAdd(Factor f) {
            if (!this.vars.equals(f.vars)) {
                throw new IllegalStateException("The varsets must be equal.");
            }
            
            for (int i=0; i<values.length; i++) {
                values[i] = Utilities.logAdd(values[i], f.values[i]);
            }
        }

        /**
         * Sets each entry in this factor to that of the given factor.
         * @param factor
         */
        public void set(Factor f) {
            if (!this.vars.equals(f.vars)) {
                throw new IllegalStateException("The varsets must be equal.");
            }
            
            for (int i=0; i<values.length; i++) {
                values[i] = f.values[i];
            }
        }

        /** Gets the sum of the values for this factor. */
        public double getSum() {
            return Vectors.sum(values);
        }
        
        /** Gets the log of the sum of the values for this factor. */
        public double getLogSum() {
            return Vectors.logSum(values);
        }
        
    }
    
    /**
     * A variable in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class Var extends Node implements Comparable<Var> {
        
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

        @Override
        public int compareTo(Var other) {
            return this.id - other.id;
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

        public boolean isSuperset(VarSet vars) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * Gets the number of possible configurations for this set of variables.
         */
        public int getNumConfigs() {
            if (this.size() == 0) {
                return 0;
            }
            int numConfigs = 1;
            for (Var var : this) {
                numConfigs *= var.getNumStates();
            }
            return numConfigs;
        }
                
    }
    
    private BipartiteGraph graph;
    private ArrayList<Factor> factors;
    private ArrayList<Var> vars;
    
    public List<FgEdge> getEdges() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNumEdges() {
        // TODO Auto-generated method stub
        return 0;
    }

    public FgEdge getEdge(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<FgEdge> getEdges(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getNode(Var var) {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getNode(Factor factor) {
        // TODO Auto-generated method stub
        return null;
    }

    public Var getVar(int varId) {
        return vars.get(varId);
    }

    public Factor getFactor(int factorId) {
        return factors.get(factorId);
    }
    
}

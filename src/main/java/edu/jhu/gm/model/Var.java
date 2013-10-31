package edu.jhu.gm.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import edu.jhu.util.collections.Lists;

/**
 * A discrete variable in a factor graph.
 * 
 * @author mgormley
 *
 */
public class Var implements Comparable<Var>, Serializable {
    
    private static final long serialVersionUID = 1L;
    /** All variables without an id are given this value. */
    public static final int UNINITIALIZED_NODE_ID = -1;
    
    public enum VarType {
        /** Observed variables will always be observed at training and test time. */
        OBSERVED,
        /** Latent variables will never be observed at training or at test time. */
        LATENT, 
        /** Predicated variables are observed at training time, but not at test time. */
        PREDICTED 
    };

    /** The type of variable. */
    private Var.VarType type;
    /** The number of states that this variable can take on. */
    private int numStates;
    /** The unique name of this variable. */
    private String name;
    /** State names, where the i'th entry gives the state names of the i'th state. */
    private ArrayList<String> stateNames;
    
    // TODO: Remove
    //    /** Counter used to create a unique id for each instance of this class. */
    //    private static final AtomicInteger instanceCounter = new AtomicInteger();
    //    /** An id that is unique to this instance of this class. */
    //    private int instanceId = instanceCounter.incrementAndGet();
        
    /**
     * 
     * @param type  The type of variable.
     * @param numStates  The number of states that this variable can take on.
     * @param name The unique name of this variable. 
     * @param stateNames The state names, where the i'th entry gives the state names of the i'th state.
     */
    public Var(VarType type, int numStates, String name, List<String> stateNames) {
        this.type = type;
        this.numStates = numStates;
        // Store and intern the name and state names.
        this.name = name.intern();
        if (stateNames != null) {
            assert(numStates == stateNames.size());
            this.stateNames = Lists.getInternedList(stateNames);
        }
    }

    public int getNumStates() {
        return numStates;
    }
            
    public Var.VarType getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }

    public ArrayList<String> getStateNames() {
        return stateNames;
    }

    /**
     * Gets the index of the variable state with the specified name.
     * 
     * Note: Current implementation takes O(n) time, where n is the number of states.
     * 
     * @param stateName The state name.
     * @return The index of the variable state, or -1 if it doesn't contain the state name.
     */
    public int getState(String stateName) {
        return stateNames.indexOf(stateName);
    }

    @Override
    public String toString() {
        return "Var [type=" + type + ", numStates=" + numStates
                + ", name=" + name + ", stateNames=" + stateNames + "]";
    }
    
    // TODO: Remove
//    /*
//     * The compareTo, equals, and hashCode methods only depend on the instanceId of
//     * the variable.
//     */
//
//    @Override
//    public int compareTo(Var other) {
//        return this.instanceId - other.instanceId;
//    }
//    
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + this.instanceId;
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        Var other = (Var) obj;
//        if (this.instanceId != other.instanceId)
//            return false;
//        return true;
//    }

    /*
     * The following allows Var object equality to be determined by the number
     * of states, the type, the name, and the state names. This is much slow
     * than using the instanceId but may be preferable for code clarity.
     */
    
    @Override
    public int compareTo(Var other) {
        int c;
                
        c = this.numStates - other.numStates;
        if (c != 0) { return c; }

        c = this.type.compareTo(other.type);
        if (c != 0) { return c; }

        c = this.name.compareTo(other.name);
        if (c != 0) { return c; }
        
        c = this.stateNames.size() - other.stateNames.size();
        if (c != 0) { return c; }        
        for (int i=0; i<this.stateNames.size(); i++) {
            c = this.stateNames.get(i).compareTo(other.stateNames.get(i));
            if (c != 0) { return c; }
        }

        return c;
    }

    private int hash = 0;
    
    @Override
    public int hashCode() {
        if (hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + numStates;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((stateNames == null) ? 0 : stateNames.hashCode());
            //return result;
            hash = result;
        }
        return hash;            
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Var other = (Var) obj;
        if (numStates != other.numStates)
            return false;
        if (type != other.type)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        
        if (stateNames == null) {
            if (other.stateNames != null)
                return false;
        } else if (!stateNames.equals(other.stateNames))
            return false;
        
        return true;
    }
    
}
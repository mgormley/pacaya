package edu.jhu.gm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A variable in a factor graph.
 * 
 * @author mgormley
 *
 */
public class Var implements Comparable<Var> {
    
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
    /** State names. */
    private ArrayList<String> stateNames;
    /** OPTIONAL: The id of the node corresponding to this variable. */
    private int nodeId;
    
    /** Counter used to create a unique id for each instance of this class. */
    private static final AtomicInteger instanceCounter = new AtomicInteger();
    /** An id that is unique to this instance of this class. */
    private int instanceId = instanceCounter.incrementAndGet();
    
    public Var(VarType type, int numStates, String name,
            List<String> stateNames) {
        this(type, numStates, name, stateNames, UNINITIALIZED_NODE_ID);
    }

    public Var(VarType type, int numStates, String name, List<String> stateNames,
            int id) {
        this.type = type;
        this.numStates = numStates;
        this.name = name;
        if (stateNames != null) {
            assert(numStates == stateNames.size());
            this.stateNames = new ArrayList<String>(stateNames);
        }
        this.nodeId = id;
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
     * Gets the id of the node corresponding to this variable.
     * 
     * For INTERNAL USE ONLY.
     */
    int getNodeId() {
        return nodeId;
    }

    /**
     * Sets the id of the node corresponding to this variable.
     *      
     * For INTERNAL USE ONLY.
     * 
     * @throws IllegalStateException if the node id for this variable was already set. 
     */
    void setNodeId(int id) {
        if (this.nodeId != UNINITIALIZED_NODE_ID) {
            throw new IllegalStateException("The id for this variable was already set: " + id);
        }
        this.nodeId = id;
    }

    /**
     * Gets the index of the variable state with the specified name.
     * 
     * Note: Current implementation takes O(n) time, where n is the number of states.
     * 
     * @param stateName The state name.
     * @return The index of the variable state.
     */
    public int getState(String stateName) {
        return stateNames.indexOf(stateName);
    }
    
    /*
     * The compareTo, equals, and hashCode methods only depend on the instanceId of
     * the variable.
     */

    @Override
    public int compareTo(Var other) {
        return this.instanceId - other.instanceId;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.instanceId;
        return result;
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
        if (this.instanceId != other.instanceId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Var [type=" + type + ", nodeId=" + nodeId + ", numStates=" + numStates
                + ", name=" + name + ", stateNames=" + stateNames + "]";
    }
    
}
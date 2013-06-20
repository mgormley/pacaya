package edu.jhu.gm;

import java.util.ArrayList;

import edu.jhu.gm.BipartiteGraph.Node;

/**
 * A variable in a factor graph.
 * 
 * @author mgormley
 *
 */
public class Var extends Node implements Comparable<Var> {
    
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
    /** The unique index for this variable. */
    private int id;
    /** The number of states that this variable can take on. */
    private int numStates;
    /** The unique name of this variable. */
    private String name;
    /** State names. */
    private ArrayList<String> stateNames;
        
    public Var(VarType type, int id, int numStates, String name,
            ArrayList<String> stateNames) {
        this.type = type;
        this.id = id;
        this.numStates = numStates;
        this.name = name;
        this.stateNames = stateNames;
    }

    public int getNumStates() {
        return numStates;
    }
            
    public Var.VarType getType() {
        return type;
    }

    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public ArrayList<String> getStateNames() {
        return stateNames;
    }

    /*
     * The compareTo, equals, and hashCode methods only depend on the id of
     * the variable.
     */

    @Override
    public int compareTo(Var other) {
        return this.id - other.id;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Var [type=" + type + ", id=" + id + ", numStates=" + numStates
                + ", name=" + name + ", stateNames=" + stateNames + "]";
    }
    
}
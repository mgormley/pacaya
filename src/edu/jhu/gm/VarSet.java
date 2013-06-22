package edu.jhu.gm;

import java.util.Arrays;
import java.util.Iterator;

import cern.colt.list.IntArrayList;

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
public class VarSet extends SmallSet<Var> {

    public VarSet() {
        super();
    }
    
    /** Copy constructor. */
    public VarSet(VarSet vars) {
        super(vars);
    }
    
    /** Constructs a variable set containing only this variable. */
    public VarSet(Var var) {
        super(1);
        add(var);
    }

    /** Constructs a new variable set which is the union of vars1 and vars2. */
    @SuppressWarnings("unchecked")
    public VarSet(VarSet vars1, VarSet vars2) {
        super(vars1, vars2);
    }

    /** Constucts a variable set containing all the given vars. */
    public VarSet(Var... vars) {
        super(vars.length);
        addAll(Arrays.asList(vars));
    }

    /**
     * Gets an iterator over the configurations of the variables for this
     * variable set, such that the c'th configuration in the iterator will
     * correspond to the variable setting where all the values of the
     * variables in this set take on the same values as the c'th
     * configuration of the variables in vars, and all the other variables
     * take on the zero state.
     * 
     * This is a trick from libDAI for efficiently computing factor
     * products.
     * 
     * @param vars The variable set to which the iterator should be aligned.
     * @return The iterator.
     */
    public IntIter getConfigIter(VarSet vars) {
        return new IndexFor(this, vars);
    }
    
    public int[] getConfigArr(VarSet vars) {        
        IntArrayList a = new IntArrayList(vars.getNumConfigs());
        IntIter iter = getConfigIter(vars);
        while (iter.hasNext()) {
            a.add(iter.next());
        }
        a.trimToSize();
        return a.elements();
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

    @Override
    public String toString() {
        return "VarSet [" + super.toString() + "]";
    }
            
}
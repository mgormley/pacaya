package edu.jhu.gm.model;

import edu.jhu.gm.util.IntIter;
import edu.jhu.prim.list.IntArrayList;

/**
 * Iterator over variable configuration indices, where some of the variables
 * have been clamped.
 * 
 * @author mgormley
 */
public class IndexForVc extends IndexFor {

    private int addend;
    
    // This constructor is private -- the factory methods below should be used instead.
    private IndexForVc(VarSet indexVars, VarSet forVars, int addend) {
        super(indexVars, forVars);
        this.addend = addend;
    }
    
    @Override
    public int next() {
        int index = super.next();
        if (index == -1) {
            return index;
        } else {
            return index + addend;
        }
    }
    
    /**
     * Iterates over all the configurations of indexVars where the subset of
     * variables in config have been clamped to their given values. The
     * iterator returns the configuration index of variables in indexVars.
     * 
     * @param indexVars Variable set over which to iterate.
     * @param config Clamped assignment.
     * @return Iterator.
     */
    public static IndexForVc getConfigIter(VarSet indexVars, VarConfig config) {
        int fixedConfigContrib = getConfigIndex(indexVars, config);
        VarSet forVars = new VarSet(indexVars);
        forVars.removeAll(config.getVars());
        return new IndexForVc(indexVars, forVars, fixedConfigContrib);
    }

    /**
     * Gets an array version of the configuration iterator.
     * @see edu.jhu.gm.model.IndexForVc#getConfigIter
     */
    public static int[] getConfigArr(VarSet vars, VarConfig config) {        
        IntArrayList a = new IntArrayList(config.getVars().calcNumConfigs());
        IntIter iter = getConfigIter(vars, config);
        while (iter.hasNext()) {
            a.add(iter.next());
        }
        return a.toNativeArray();
    }
    
    /**
     * Gets the index of the configuration of the variables where all those in config 
     * have the specified value, and all other variables in vars have the zero state.
     * 
     * @param vars The variable set over which to iterate.
     * @param config An assignment to a subset of vars.
     * @return The configuration index.
     */
    public static int getConfigIndex(VarSet vars, VarConfig config) {
        int configIndex = 0;
        int numStatesProd = 1;
        for (Var var : vars) {
            int state = config.getState(var, 0);
            configIndex += state * numStatesProd;
            numStatesProd *= var.getNumStates();
        }
        return configIndex;
    }
}
package edu.jhu.gridsearch.rlt.filter;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.gridsearch.rlt.Rlt;
import edu.jhu.lp.FactorBuilder.BoundFactor;
import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.lp.FactorList;
import edu.jhu.util.Pair;
import edu.jhu.util.SafeCast;
import edu.jhu.util.Utilities;
import edu.jhu.util.collections.IntArrayList;
import edu.jhu.util.collections.IntHashSet;
import edu.jhu.util.collections.IntObjectHashMap;
import edu.jhu.util.tuple.OrderedPair;
import edu.jhu.util.tuple.UnorderedPair;
import edu.jhu.util.vector.LongDoubleEntry;

/**
 * Adds only RLT rows that have a non-zero coefficient for some RLT variable corresponding
 * to the given pairs of variables.
 */
public class VarRltRowAdder implements RltRowAdder {
    
    private Set<UnorderedPair> varIdPairs;
    private IntHashSet inputVarIds;
    private List<Pair<IloNumVar, IloNumVar>> pairs;
    private Rlt rlt;
    private boolean boundsOnly;
    
    public VarRltRowAdder(List<Pair<IloNumVar,IloNumVar>> pairs, boolean boundsOnly) {
        this.pairs = pairs;
        this.boundsOnly = boundsOnly;
    }

    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        varIdPairs = new HashSet<UnorderedPair>();
        inputVarIds = new IntHashSet();
        for (Pair<IloNumVar, IloNumVar> pair : pairs) {
            int varId1 = SafeCast.safeLongToInt(rlt.getIdForInputVar(pair.get1()));
            int varId2 = SafeCast.safeLongToInt(rlt.getIdForInputVar(pair.get2()));
            varIdPairs.add(new UnorderedPair(varId1, varId2));
            inputVarIds.add(varId1);
            inputVarIds.add(varId2);
        }
        pairs = null;
        this.rlt = rlt;
    }

    @Override
    public Collection<OrderedPair> getRltRowsForEq(int startFac, int endFac, int numVars, RowType type) {
        FactorList eqFactors = rlt.getEqFactors().sublist(startFac, endFac);

        // Get a mapping of variables ids to factors indices.
        IntObjectHashMap<IntArrayList> varConsMap = getVarConsMap(eqFactors);
        
        // For each pair of variable ids, lookup the corresponding pair of lists of factors.
        Set<OrderedPair> rltRows = new HashSet<OrderedPair>();
        for (UnorderedPair varIdPair : varIdPairs) {
            IntArrayList list1 = Utilities.safeGetList(varConsMap, varIdPair.get1());
            for (int i=0; i<list1.size(); i++) {
                int consId1 = list1.get(i);
                // Add a RLT row for the current factor multiplied with the
                // variable corresponding to the second variable in this pair.
                rltRows.add(new OrderedPair(consId1, varIdPair.get2()));
            }
            IntArrayList list2 = Utilities.safeGetList(varConsMap, varIdPair.get2());
            for (int i=0; i<list2.size(); i++) {
                int consId2 = list2.get(i);
                // Add a RLT row for the current factor multiplied with the
                // variable corresponding to the first variable in this pair.
                rltRows.add(new OrderedPair(consId2, varIdPair.get1()));
            }
        }
        
        return rltRows;    
    }

    @Override
    public Collection<UnorderedPair> getRltRowsForLeq(int startFac1, int endFac1, int startFac2, int endFac2,
            RowType type) {
        FactorList leqFactors = rlt.getLeqFactors();

        // Get a mapping of variables ids to factors indices.
        IntObjectHashMap<IntArrayList> varConsMap = getVarConsMap(leqFactors);
        
        // For each pair of variable ids, lookup the corresponding pair of lists of factors.
        Set<UnorderedPair> rltRows = new HashSet<UnorderedPair>();
        for (UnorderedPair varIdPair : varIdPairs) {
            IntArrayList list1 = Utilities.safeGetList(varConsMap, varIdPair.get1());
            IntArrayList list2 = Utilities.safeGetList(varConsMap, varIdPair.get2());
            for (int i=0; i<list1.size(); i++) {
                int consId1 = list1.get(i);
                for (int j=0; j<list2.size(); j++) {
                    int consId2 = list2.get(j);
                    if ((      startFac1 <= consId1 && consId1 < endFac1 
                            && startFac2 <= consId2 && consId2 < endFac2) ||
                        (      startFac2 <= consId1 && consId1 < endFac2 
                            && startFac1 <= consId2 && consId2 < endFac1)) {
                        // Add a RLT row, for each pair of factors.
                        rltRows.add(new UnorderedPair(consId1, consId2));
                    }
                }
            }
        }
        
        return rltRows;
    }

    /**
     * Construct a multi-map of variable ids to factor indices. For each factor i
     * containing a non-zero coefficient for variable k, add an entry for (k
     * --> i).
     */ 
    private IntObjectHashMap<IntArrayList> getVarConsMap(FactorList factors) {
        IntObjectHashMap<IntArrayList> varConsMap = new IntObjectHashMap<IntArrayList>();
        for (int i=0; i<factors.size(); i++) {
            Factor factor = factors.get(i);
            if (boundsOnly && !(factor instanceof BoundFactor)){
                continue;
            }
            for (LongDoubleEntry ve : factor.G) {
                // Check that this is a nonzero entry.
                if (ve.get() != 0.0) {
                    int veIdx = SafeCast.safeLongToInt(ve.index());
                    if (inputVarIds.contains(veIdx)) {
                        Utilities.addToList(varConsMap, veIdx, i);
                    }
                }
            }
        }
        return varConsMap;
    }
    
}
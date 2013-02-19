package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.uib.cipr.matrix.sparse.longs.LVectorEntry;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.lp.FactorList;
import edu.jhu.hltcoe.lp.FactorBuilder.BoundFactor;
import edu.jhu.hltcoe.lp.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.SafeCast;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.tuple.OrderedPair;
import edu.jhu.hltcoe.util.tuple.UnorderedPair;
import gnu.trove.TIntHashSet;

/**
 * Adds only RLT rows that have a non-zero coefficient for some RLT variable corresponding
 * to the given pairs of variables.
 */
public class VarRltRowAdder implements RltRowAdder {
    
    private Set<UnorderedPair> varIdPairs;
    private TIntHashSet inputVarIds;
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
        inputVarIds = new TIntHashSet();
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
        Map<Integer, List<Integer>> varConsMap = getVarConsMap(eqFactors);
        
        // For each pair of variable ids, lookup the corresponding pair of lists of factors.
        Set<OrderedPair> rltRows = new HashSet<OrderedPair>();
        for (UnorderedPair varIdPair : varIdPairs) {
            for (Integer consId1 : Utilities.safeGetList(varConsMap, varIdPair.get1())) {
                // Add a RLT row for the current factor multiplied with the
                // variable corresponding to the second variable in this pair.
                rltRows.add(new OrderedPair(consId1, varIdPair.get2()));
            }
            for (Integer consId2 : Utilities.safeGetList(varConsMap, varIdPair.get2())) {
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
        Map<Integer, List<Integer>> varConsMap = getVarConsMap(leqFactors);
        
        // For each pair of variable ids, lookup the corresponding pair of lists of factors.
        Set<UnorderedPair> rltRows = new HashSet<UnorderedPair>();
        for (UnorderedPair varIdPair : varIdPairs) {
            for (Integer i : Utilities.safeGetList(varConsMap, varIdPair.get1())) {
                for (Integer j : Utilities.safeGetList(varConsMap, varIdPair.get2())) {
                    if ((startFac1 <= i && i < endFac1 && startFac2 <= j && j < endFac2) ||
                            (startFac2 <= i && i < endFac2 && startFac1 <= j && j < endFac1)) {
                        // Add a RLT row, for each pair of factors.
                        rltRows.add(new UnorderedPair(i, j));
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
    private Map<Integer, List<Integer>> getVarConsMap(FactorList factors) {
        Map<Integer, List<Integer>> varConsMap = new HashMap<Integer, List<Integer>>();
        for (int i=0; i<factors.size(); i++) {
            Factor factor = factors.get(i);
            if (boundsOnly && !(factor instanceof BoundFactor)){
                continue;
            }
            for (LVectorEntry ve : factor.G) {
                int veIdx = SafeCast.safeLongToInt(ve.index());
                if (inputVarIds.contains(veIdx)) {
                    Utilities.addToList(varConsMap, veIdx, i);
                }
            }
        }
        return varConsMap;
    }
    
}
package edu.jhu.hltcoe.gridsearch.dmv;

import gnu.trove.TDoubleArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.LpStoBuilderPrm;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltPrm;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.VarRltFactorFilter;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.VarRltRowFilter;
import edu.jhu.hltcoe.lp.CplexPrm;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder.DmvTreeProgram;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.CplexUtils;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

public class DmvRltRelaxation implements DmvRelaxation {

    public static class DmvRltRelaxPrm {
        public File tempDir = null;
        public int maxCutRounds = 1;
        public boolean objVarFilter = true;
        public CplexPrm cplexPrm = new CplexPrm();
        public RltPrm rltPrm = new RltPrm();
        public LpStoBuilderPrm stoPrm = new LpStoBuilderPrm();
        public DmvRltRelaxPrm() { 
            cplexPrm.simplexAlgorithm = IloCplex.Algorithm.Dual;
        }
        public DmvRltRelaxPrm(File tempDir, int maxCutRounds, CutCountComputer ccc, boolean envelopeOnly) {
            this();
            this.tempDir = tempDir;
            this.maxCutRounds = maxCutRounds;
            this.stoPrm.initCutCountComp = ccc;
            this.rltPrm.envelopeOnly = envelopeOnly;
        }
    }
    
    private static final Logger log = Logger.getLogger(DmvRltRelaxation.class);

    private static final double OBJ_VAL_DECREASE_TOLERANCE = 1.0;
    private static final double INTERNAL_BEST_SCORE = Double.NEGATIVE_INFINITY;
    private static final double INTERNAL_WORST_SCORE = Double.POSITIVE_INFINITY;
    
    private IloCplex cplex;
    private int numSolves;
    private Stopwatch simplexTimer;

    private DmvTrainCorpus corpus;
    private IndexedDmvModel idm;
    private CptBounds bounds;
    private LpProblem mp;
    private LpSumToOneBuilder sto;    
    private DmvObjective dmvObj;
 
    private DmvRltRelaxPrm prm;
    
    public DmvRltRelaxation(DmvRltRelaxPrm prm) {
        this.prm = prm;
        this.numSolves = 0;
        this.simplexTimer = new Stopwatch();
        this.sto = new LpSumToOneBuilder(prm.stoPrm);        
    }
    
    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public void init1(DmvTrainCorpus corpus) {
        this.corpus = corpus;
        this.idm = new IndexedDmvModel(this.corpus);
        this.dmvObj = new DmvObjective(this.corpus);
    }
    
    // Copied from DantzigWolfeRelaxation.
    @Override
    public void init2(DmvSolution initFeasSol) {
        this.cplex = prm.cplexPrm.getIloCplexInstance();
        try {
            buildModel(cplex, initFeasSol);
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience class for passing around Master Problem variables
     */
    protected static class LpProblem {
        public IloObjective objective;
        public IloNumVar[][] objVars;
        public IloLPMatrix origMatrix;
        public Rlt rlt;
        public DmvTreeProgram pp;
    }

    private void buildModel(IloCplex cplex, DmvSolution initFeasSol) throws IloException {
        this.bounds = new CptBounds(this.idm);

        mp = new LpProblem();
        
        // Add the LP matrix that will contain all the constraints.
        mp.origMatrix = cplex.LPMatrix("couplingMatrix");
        
        // Initialize the model parameter variables and constraints.
        sto.init(cplex, mp.origMatrix, idm, bounds);
        
        int numConds = idm.getNumConds();
        
        // Create the model parameters variables.
        sto.createModelParamVars();

        // Add sum-to-one constraints on the model parameters.
        sto.addModelParamConstraints();
        
        // Add the parsing constraints.
        DmvParseLpBuilder builder = new DmvParseLpBuilder(cplex, IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ);
        mp.pp = builder.buildDmvTreeProgram(corpus);
        builder.addConsToMatrix(mp.pp, mp.origMatrix);
        
        RltPrm rltPrm = prm.rltPrm;
        if (prm.objVarFilter) {
            if (rltPrm.rowFilter != null && rltPrm.factorFilter != null) {
                log.warn("Overriding existing filters");
            }
            // Accept only RLT rows/factors that have a non-zero coefficient for some objective variable.
            rltPrm.rowFilter = new VarRltRowFilter(getObjVarPairs());
            rltPrm.factorFilter = new VarRltFactorFilter(getObjVarCols());
        }
        
        // Add the RLT constraints.
        mp.rlt = new Rlt(cplex, mp.origMatrix, rltPrm);
        IloLPMatrix rltMat = mp.rlt.getRltMatrix();
        cplex.add(mp.origMatrix);
        cplex.add(rltMat);
        
        // Create the objective
        mp.objective = cplex.addMinimize();

        // Add the supervised portion to the objective.
        int[][] totSupFreqCm = idm.getTotSupervisedFreqCm(corpus);
        for (int c = 0; c < numConds; c++) {
            for (int m = 0; m < idm.getNumParams(c); m++) {
                if (totSupFreqCm[c][m] != 0) {
                    // Negate the coefficients since we are minimizing
                    cplex.setLinearCoef(mp.objective, -totSupFreqCm[c][m], sto.modelParamVars[c][m]);
                }
            }
        }
        
        // Create the objective variables, adding them to the objective
        mp.objVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            mp.objVars[c] = new IloNumVar[numParams];
            for (int m=0; m<numParams; m++) {
                // Assign RLT vars to objVars.
                mp.objVars[c][m] = mp.rlt.getRltVar(sto.modelParamVars[c][m], mp.pp.featCountVars[c][m]);
                // Negate the coefficients since we are minimizing
                cplex.setLinearCoef(mp.objective, -1.0, mp.objVars[c][m]);
            }
        }
    }

    private List<IloNumVar> getObjVarCols() {
        List<IloNumVar> vars = new ArrayList<IloNumVar>();
        for (int c = 0; c < sto.modelParamVars.length; c++) {
            for (int m = 0; m < sto.modelParamVars[c].length; m++) {
                vars.add(sto.modelParamVars[c][m]);
                vars.add(mp.pp.featCountVars[c][m]);
            }
        }
        return vars;
    }

    private List<Pair<IloNumVar, IloNumVar>> getObjVarPairs() {
        List<Pair<IloNumVar, IloNumVar>> pairs = new ArrayList<Pair<IloNumVar, IloNumVar>>();
        for (int c = 0; c < sto.modelParamVars.length; c++) {
            for (int m = 0; m < sto.modelParamVars[c].length; m++) {
                pairs.add(new Pair<IloNumVar,IloNumVar>(sto.modelParamVars[c][m], mp.pp.featCountVars[c][m]));
            }
        }
        return pairs;
    }

    @Override
    public void addFeasibleSolution(DmvSolution initFeasSol) {
        // Do nothing.
    }

    // Copied from DantzigWolfeRelaxation.
    @Override
    public RelaxedSolution solveRelaxation() {
        return solveRelaxation(LazyBranchAndBoundSolver.WORST_SCORE);
    }
    
    // Copied from DantzigWolfeRelaxation.
    @Override
    public RelaxedSolution solveRelaxation(double incumbentScore) {
        try {
            numSolves++;
            // Negate since we're minimizing internally
            double upperBound = -incumbentScore;
            Pair<RelaxStatus,Double> pair = runSimplexAlgo(cplex, upperBound);
            RelaxStatus status = pair.get1();
            double lowerBound = pair.get2();
            
            // Negate the objective since we were minimizing 
            double objective = -lowerBound;
            assert(!Double.isNaN(objective));
            // This won't always be true if we are stopping early: 
            // assert(Utilities.lte(objective, 0.0, 1e-7));
            
            if (prm.tempDir != null) {
                cplex.exportModel(new File(prm.tempDir, "rlt.lp").getAbsolutePath());
            }
            
            log.info("Solution status: " + status);
            if (!status.hasSolution()) {
                return new RelaxedDmvSolution(null, null, objective, status, null, null, Double.NaN);
            }
            
            if (prm.tempDir != null) {
                cplex.writeSolution(new File(prm.tempDir, "rlt.sol").getAbsolutePath());
            }
            log.info("Lower bound: " + lowerBound);
            RelaxedSolution relaxSol = extractSolution(status, objective);
            log.info("True obj for relaxed vars: " + relaxSol.getTrueObjectiveForRelaxedSolution());
            return relaxSol;
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    private Pair<RelaxStatus, Double> runSimplexAlgo(IloCplex cplex2, double upperBound) throws IloException {
        if (!isFeasible()) {
            return new Pair<RelaxStatus,Double>(RelaxStatus.Infeasible, INTERNAL_WORST_SCORE);
        }
        
        RelaxStatus status = RelaxStatus.Unknown;
        TDoubleArrayList cutIterLowerBounds = new TDoubleArrayList();
        TDoubleArrayList cutIterObjVals = new TDoubleArrayList();
        ArrayList<Status> cutIterStatuses = new ArrayList<Status>();
        WarmStart warmStart = null;
        cutIterLowerBounds.add(INTERNAL_BEST_SCORE);        
        
        // Stop early if we can fathom the node.
        cplex.setParam(DoubleParam.ObjULim, upperBound);
        
        int cut;
        // Solve the full LP problem
        for (cut = 0; ;) {
            if (prm.tempDir != null) {
                cplex.exportModel(new File(prm.tempDir, "rlt.lp").getAbsolutePath());
            }
            
            // Solve the master problem
            if (warmStart != null) {
                setWarmStart(warmStart);
            }
            simplexTimer.start();
            cplex.solve();
            simplexTimer.stop();
            status = RelaxStatus.getForLp(cplex.getStatus()); 
            
            log.trace("LP solution status: " + cplex.getStatus());
            log.trace("LP CPLEX status: " + cplex.getCplexStatus());
            if (status == RelaxStatus.Infeasible) {
                return new Pair<RelaxStatus,Double>(status, INTERNAL_WORST_SCORE);
            }
            if (prm.tempDir != null) {
                cplex.writeSolution(new File(prm.tempDir, "rlt.sol").getAbsolutePath());
            }
            warmStart = getWarmStart();
            double objVal = cplex.getObjValue();
            log.trace("Simplex solution value: " + objVal);
            double prevObjVal = cutIterObjVals.size() > 0 ? cutIterObjVals.get(cutIterObjVals.size() - 1)
                    : INTERNAL_WORST_SCORE;
            if (objVal > prevObjVal + OBJ_VAL_DECREASE_TOLERANCE) {
                Status prevStatus = cutIterStatuses.size() > 0 ? cutIterStatuses.get(cutIterObjVals.size() - 1)
                        : Status.Unknown;
                log.warn(String.format("LP objective should monotonically decrease: prev=%f cur=%f. prevStatus=%s curStatus=%s.", prevObjVal, objVal, prevStatus, cplex.getStatus()));
            }
            cutIterObjVals.add(objVal);
            cutIterStatuses.add(cplex.getStatus());
            
            double lowerBound = objVal;
            cutIterLowerBounds.add(lowerBound);
            
            if (status == RelaxStatus.Feasible) {
                // TODO: get the dual bound and remove this warning.
                log.warn("A feasible solution does not currently give a valid bound.");
            }
            if( cplex.getCplexStatus() == CplexStatus.AbortObjLim && lowerBound < upperBound) {
                log.warn(String.format("Lower bound %f should >= upper bound %f.", lowerBound, upperBound));
            }
            
            // Check whether to continue
            if (lowerBound >= upperBound) {
                // We can fathom this node
                status = RelaxStatus.Pruned;
                break;
            } else {
                // Optimal solution found
                status = RelaxStatus.Optimal;
                
                if (cut < prm.maxCutRounds) {
                    log.debug(String.format("Iteration objective values (cut=%d): %s", cut, cutIterObjVals));
                    int numCutAdded = addCuts(cplex, cut);
                    log.debug("Added cuts " + numCutAdded + ", round " + cut);
                    if (numCutAdded == 0) {
                        // No new cuts are needed
                        log.debug("No more cut rounds needed after " + cut + " rounds");
                        break;
                    }
                    cut++;
                } else {
                    // Optimal solution found and no more cut rounds left
                    break;
                }
            }
        }
        
        // The lower bound should be strictly increasing, because we add cuts. We still
        // keep track of the lower bounds in case we terminate early.
        double lowerBound = Vectors.max(cutIterLowerBounds.toNativeArray());
        
        log.debug("Number of cut rounds: " + cut);
        log.debug("Final lower bound: " + lowerBound);
        log.debug(String.format("Iteration objective values (cut=%d): %s", cut, cutIterObjVals));
        log.debug("Iteration lower bounds: " + cutIterLowerBounds);
        log.debug("Avg simplex time(ms) per solve: " + Time.totMs(simplexTimer) / numSolves);
        log.info(String.format("Summary: #cuts=%d #origCons=%d #rltCons=%d", 
                sto.getNumStoCons(), mp.origMatrix.getNrows(), mp.rlt.getRltMatrix().getNrows()));
    
        return new Pair<RelaxStatus,Double>(status, lowerBound);
    }

    private int addCuts(IloCplex cplex, int cut) throws UnknownObjectException, IloException {
         List<Integer> rows = sto.projectModelParamsAndAddCuts();
         return mp.rlt.addRows(rows);
    }
    
    // Copied from DmvDantzigWolfeRelaxation.
    private RelaxedSolution extractSolution(RelaxStatus status, double objective) throws UnknownObjectException, IloException {
        // Store optimal model parameters
        double[][] optimalLogProbs = extractRelaxedLogProbs();
        
        // Store optimal feature counts
        double[][] optimalFeatCounts = getFeatureCounts();

        // Store objective values z_{c,m}
        double[][] objVals = new double[idm.getNumConds()][];
        for (int c = 0; c < idm.getNumConds(); c++) {
            objVals[c] = cplex.getValues(mp.objVars[c]);
        }
        
        // Compute the true quadratic objective given the model
        // parameters and feature counts found by the relaxation.
        double trueRelaxObj = dmvObj.computeTrueObjective(optimalLogProbs, optimalFeatCounts);
        
        // Store fractional corpus parse
        RelaxedDepTreebank treebank = extractRelaxedParse();

        // Print out proportion of fractional edges
        log.info("Proportion of fractional arcs: " + treebank.getPropFracArcs());
        
        return new RelaxedDmvSolution(Utilities.copyOf(optimalLogProbs), treebank, objective, status, Utilities
                .copyOf(optimalFeatCounts), Utilities.copyOf(objVals), trueRelaxObj);
    }

    // Copied from DmvDantzigWolfeRelaxation.
    private double[][] extractRelaxedLogProbs() throws UnknownObjectException, IloException {
        return sto.extractRelaxedLogProbs();
    }

    private double[][] getFeatureCounts() throws IloException {
        return CplexUtils.getValues(cplex, mp.pp.featCountVars);
    }

    protected RelaxedDepTreebank extractRelaxedParse() throws UnknownObjectException, IloException {
        RelaxedDepTreebank relaxTreebank = new RelaxedDepTreebank(corpus);
        relaxTreebank.setFracRoots(CplexUtils.getValues(cplex, mp.pp.arcRoot));
        relaxTreebank.setFracChildren(CplexUtils.getValues(cplex, mp.pp.arcChild));
        return relaxTreebank;
    }

    // Copied from DmvDantzigWolfeRelaxation.
    private boolean isFeasible() {
        return bounds.areFeasibleBounds();
    }

    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        return dmvObj.computeTrueObjective(logProbs, treebank);
    }
    
    // Copied from DantzigWolfeRelaxation.
    @Override
    public void end() {
        cplex.end();
    }

    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public void reverseApply(CptBoundsDeltaList deltas) {
        applyDeltaList(CptBoundsDeltaList.getReverse(deltas));
    }

    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public void forwardApply(CptBoundsDeltaList deltas) {
        applyDeltaList(deltas);
    }

    // Copied from DmvDantzigWolfeRelaxation.
    private void applyDeltaList(CptBoundsDeltaList deltas) {
        for (CptBoundsDelta delta : deltas) {
            applyDelta(delta);
        }
    }
    
    // Copied (with modifications) from DmvDantzigWolfeRelaxation.
    private void applyDelta(CptBoundsDelta delta) {
        try {
            Type type = delta.getType();
            int c = delta.getC();
            int m = delta.getM();

            double origLb = bounds.getLb(type, c, m);
            double origUb = bounds.getUb(type, c, m);
            double newLb = origLb;
            double newUb = origUb;

            if (delta.getLu() == Lu.LOWER) {
                newLb = origLb + delta.getDelta();
            } else if (delta.getLu() == Lu.UPPER) {
                newUb = origUb + delta.getDelta();
            } else {
                throw new IllegalStateException();
            }

            assert newLb <= newUb : String.format("l,u = %f, %f", newLb, newUb);
            bounds.set(type, c, m, newLb, newUb);

            if (type == Type.PARAM) {
                // Updates the bounds of the model parameters
                sto.updateModelParamBounds(c, m, newLb, newUb);
                mp.rlt.updateBound(sto.modelParamVars[c][m], delta.getLu());
            } else {
                //TODO: Implement this
                throw new RuntimeException("not implemented");
            }

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }    
    
    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public CptBounds getBounds() {
        return bounds;
    }
    
    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public IndexedDmvModel getIdm() {
        return idm;
    }
    
    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public WarmStart getWarmStart() {
        try {
            WarmStart warmStart = new WarmStart();
            
            ArrayList<IloNumVar> numVars = new ArrayList<IloNumVar>();
            numVars.addAll(Arrays.asList(mp.origMatrix.getNumVars()));
            numVars.addAll(Arrays.asList(mp.rlt.getRltMatrix().getNumVars()));
            
            ArrayList<IloRange> ranges = new ArrayList<IloRange>();
            ranges.addAll(Arrays.asList(mp.origMatrix.getRanges()));
            ranges.addAll(Arrays.asList(mp.rlt.getRltMatrix().getRanges()));
            
            warmStart.numVars = numVars.toArray(new IloNumVar[]{});
            warmStart.ranges = ranges.toArray(new IloRange[]{});
            warmStart.numVarStatuses = cplex.getBasisStatuses(warmStart.numVars);
            warmStart.rangeStatuses = cplex.getBasisStatuses(warmStart.ranges);
            return warmStart;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void setWarmStart(WarmStart warmStart) {
        try {
            // Set the basis status of all variables
            cplex.setBasisStatuses(warmStart.numVars, warmStart.numVarStatuses, warmStart.ranges, warmStart.rangeStatuses);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
}

package edu.jhu.gridsearch.dmv;

import edu.jhu.util.collections.DoubleArrayList;
import edu.jhu.util.collections.IntArrayList;
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

import edu.jhu.data.DepTreebank;
import edu.jhu.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.gridsearch.ProblemNode;
import edu.jhu.gridsearch.RelaxStatus;
import edu.jhu.gridsearch.RelaxedSolution;
import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.gridsearch.cpt.LpSumToOneBuilder;
import edu.jhu.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.gridsearch.cpt.LpSumToOneBuilder.LpStoBuilderPrm;
import edu.jhu.gridsearch.dmv.DmvDantzigWolfeRelaxation.DmvRelaxationFactory;
import edu.jhu.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.gridsearch.dr.DimReducer;
import edu.jhu.gridsearch.dr.DimReducer.DimReducerPrm;
import edu.jhu.gridsearch.rlt.Rlt;
import edu.jhu.gridsearch.rlt.Rlt.RltPrm;
import edu.jhu.gridsearch.rlt.filter.UnionRltRowAdder;
import edu.jhu.gridsearch.rlt.filter.VarRltFactorFilter;
import edu.jhu.gridsearch.rlt.filter.VarRltRowAdder;
import edu.jhu.parse.IlpFormulation;
import edu.jhu.parse.relax.DmvParseLpBuilder;
import edu.jhu.parse.relax.DmvParseLpBuilder.DmvParseLpBuilderPrm;
import edu.jhu.parse.relax.DmvParseLpBuilder.DmvTreeProgram;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.util.Pair;
import edu.jhu.util.Timer;
import edu.jhu.util.Triple;
import edu.jhu.util.Utilities;
import edu.jhu.util.cplex.CplexPrm;
import edu.jhu.util.cplex.CplexUtils;
import edu.jhu.util.math.Vectors;

public class DmvRltRelaxation implements DmvRelaxation {

    public static class DmvRltRelaxPrm implements DmvRelaxationFactory {
        public final double OBJ_VAL_DECREASE_TOLERANCE = 1.0;
        public File tempDir = null;
        public int maxCutRounds = 1;
        public boolean objVarFilter = true;
        public boolean addBindingCons = false;
        public double timeoutSeconds = Double.MAX_VALUE;
        public int rootMaxCutRounds = 1;
        public CplexPrm cplexPrm = new CplexPrm();
        public RltPrm rltPrm = new RltPrm();
        public LpStoBuilderPrm stoPrm = new LpStoBuilderPrm();
        public DmvParseLpBuilderPrm parsePrm = new DmvParseLpBuilderPrm();
        public DimReducerPrm drPrm = new DimReducerPrm();
        public DmvObjectivePrm objPrm = new DmvObjectivePrm();
        public DmvRltRelaxPrm() { 
            // We have to use the Dual simplex algorithm in order to 
            // stop early and fathom a node.
            cplexPrm.simplexAlgorithm = IloCplex.Algorithm.Dual;
            // For now we always use this formulation.
            parsePrm.formulation = IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ;
        }
        public DmvRltRelaxPrm(File tempDir, int maxCutRounds, CutCountComputer ccc, boolean envelopeOnly) {
            this();
            this.tempDir = tempDir;
            this.maxCutRounds = maxCutRounds;
            this.stoPrm.initCutCountComp = ccc;
            this.rltPrm.envelopeOnly = envelopeOnly;
            this.rootMaxCutRounds = maxCutRounds;
        }
        @Override
        public DmvRelaxation getInstance(DmvTrainCorpus corpus, DmvSolution initFeasSol) {
            DmvRltRelaxation relax = new DmvRltRelaxation(this);
            relax.init1(corpus);
            relax.init2(initFeasSol);
            return relax;
        }
    }
    
    private static final Logger log = Logger.getLogger(DmvRltRelaxation.class);

    private static final double INTERNAL_BEST_SCORE = Double.NEGATIVE_INFINITY;
    private static final double INTERNAL_WORST_SCORE = Double.POSITIVE_INFINITY;
    
    private IloCplex cplex;
    private int numSolves;
    private Timer simplexTimer;
    private Timer switchTimer;

    private DmvTrainCorpus corpus;
    private IndexedDmvModel idm;
    private CptBounds bounds;
    private LpProblem mp;
    private LpSumToOneBuilder sto;    
    private DmvObjective dmvObj;
    private DmvProblemNode activeNode;
    
    private DmvRltRelaxPrm prm;

    private boolean drOn;
    
    public DmvRltRelaxation(DmvRltRelaxPrm prm) {
        this.prm = prm;
        this.numSolves = 0;
        this.simplexTimer = new Timer();
        this.switchTimer = new Timer();
        this.sto = new LpSumToOneBuilder(prm.stoPrm);        
    }
    
    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public void init1(DmvTrainCorpus corpus) {
        this.corpus = corpus;
        this.idm = new IndexedDmvModel(this.corpus);
        this.dmvObj = new DmvObjective(prm.objPrm, idm);
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
        public IloLPMatrix drMatrix;
        public Rlt rlt;
        public DmvTreeProgram pp;
    }

    private void buildModel(IloCplex cplex, DmvSolution initFeasSol) throws IloException {
        this.bounds = new CptBounds(this.idm);
        mp = new LpProblem();
        int numConds = idm.getNumConds();
        // Create the LP matrix that will contain all the constraints.
        mp.origMatrix = cplex.LPMatrix("origMatrix");

        // Initialize the model parameter variables and constraints.
        sto.init(cplex, mp.origMatrix, idm, bounds);
        // Create the model parameters variables.
        sto.createModelParamVars();
        // Add sum-to-one constraints on the model parameters.
        sto.addModelParamConstraints();
        
        // Add the parsing constraints.
        DmvParseLpBuilder builder = new DmvParseLpBuilder(prm.parsePrm, cplex);
        mp.pp = builder.buildDmvTreeProgram(corpus);
        builder.addConsToMatrix(mp.pp, mp.origMatrix);    
        
        // Add the parsing constraints to the input matrix for dim-reduction.
        IloLPMatrix drInputMatrix = cplex.LPMatrix("drInputMatrix");
        builder.addConsToMatrix(mp.pp, drInputMatrix);
        
        // Reduce the dimensionality of the matrix.
        DimReducer dr = new DimReducer(prm.drPrm);
        // Add an LP matrix that will contain the lower-dimensional constraints.
        mp.drMatrix = cplex.LPMatrix("drMatrix");
        dr.reduceDimensionality(drInputMatrix, mp.drMatrix);
        drOn = mp.drMatrix.getNrows() > 0;
        if (drOn) {
            // Dimensionality reduced.
            mp.origMatrix.addRows(mp.drMatrix.getRanges());
        } else {
            // Dimensionality NOT reduced.
            mp.drMatrix = null;
            // Add the full corpus parse constraints to the original matrix.
            mp.origMatrix.addRows(drInputMatrix.getRanges());
        }
        
        // Configuration for RLT.
        RltPrm rltPrm = prm.rltPrm;
        // We always keep the convex/concave envelope on the objective variables
        // so that the problem isn't unbounded.
        if (prm.objVarFilter) {
            if (rltPrm.rowAdder != null && rltPrm.factorFilter != null) {
                log.warn("Overriding existing filters");
            }
            // Accept only RLT rows/factors that have a non-zero coefficient for some objective variable.
            rltPrm.factorFilter = new VarRltFactorFilter(getObjVarCols());
            rltPrm.rowAdder = new VarRltRowAdder(getObjVarPairs(), false);
        } else {
            // Always add the convex/concave envelope. 
            VarRltRowAdder envelopeAdder = new VarRltRowAdder(getObjVarPairs(), true);
            rltPrm.rowAdder = new UnionRltRowAdder(envelopeAdder, rltPrm.rowAdder);
        }

        log.info("Applying RLT to the original matrix");
        // Add the RLT constraints using the original matrix as input.
        mp.rlt = new Rlt(cplex, mp.origMatrix, rltPrm);
        IloLPMatrix rltMat = mp.rlt.getRltMatrix();
        cplex.add(mp.origMatrix);
        cplex.add(rltMat);
        
        if (drOn) {
            // Add the full corpus parse constraints to the original matrix.
            mp.origMatrix.addRows(drInputMatrix.getRanges());
        }
                
        // Create the objective
        mp.objective = cplex.addMinimize();

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
        
        // Print out stats about the matrices.
        log.info(CplexUtils.getMatrixStats(mp.origMatrix));
        if (mp.drMatrix != null) {
            log.info(CplexUtils.getMatrixStats(mp.drMatrix));
        }
        log.info(CplexUtils.getMatrixStats(mp.rlt.getRltMatrix()));
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
    public RelaxedSolution getRelaxedSolution(ProblemNode curNode) {
        return getRelaxedSolution(curNode, LazyBranchAndBoundSolver.WORST_SCORE);
    }
    
    // Copied from DantzigWolfeRelaxation.
    @Override
    public RelaxedSolution getRelaxedSolution(ProblemNode curNode, double incumbentScore) {
        switchTimer.start();
        setAsActiveNode(curNode);

        WarmStart ws = curNode.getWarmStart();
        if (ws != null) {
            setWarmStart(ws);
        }
        switchTimer.stop();

        Pair<RelaxedSolution, WarmStart> pair = solveRelaxation(curNode, incumbentScore); 
        RelaxedSolution relaxSol = pair.get1();
        WarmStart warmStart = pair.get2();
        
        if (curNode.getLocalUb() < relaxSol.getScore()) {
            // If CPLEX gets a worse bound, then keep the parent's bound.
            relaxSol.setScore(curNode.getLocalUb());
        } else {
            curNode.setOptimisticBound(relaxSol.getScore());
        }
        curNode.setWarmStart(warmStart);

        return relaxSol;
    }
    
    // Copied from DantzigWolfeRelaxation.
    private Pair<RelaxedSolution, WarmStart> solveRelaxation(ProblemNode curNode, double incumbentScore) {
        try {            
            numSolves++;
            // Negate since we're minimizing internally
            double upperBound = -incumbentScore;
            Triple<RelaxStatus,Double,WarmStart> triple = runSimplexAlgo(cplex, upperBound, curNode.getDepth());
            RelaxStatus status = triple.get1();
            double lowerBound = triple.get2();
            WarmStart warmStart = triple.get3();
            
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
                DmvRelaxedSolution relaxSol = new DmvRelaxedSolution(null, null, objective, status, null, null, Double.NaN);
                return new Pair<RelaxedSolution, WarmStart>(relaxSol, warmStart);
            }
            
            if (prm.tempDir != null) {
                cplex.writeSolution(new File(prm.tempDir, "rlt.sol").getAbsolutePath());
            }
            log.info("Lower bound: " + lowerBound);
            RelaxedSolution relaxSol = extractSolution(status, objective);
            log.info("True obj for relaxed vars: " + relaxSol.getTrueObjectiveForRelaxedSolution());
            
            return new Pair<RelaxedSolution, WarmStart>(relaxSol, warmStart);
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    // Copied from DmvDantzigWolfeRelaxation.
    protected void setAsActiveNode(ProblemNode pn) {
        DmvProblemNode curNode = (DmvProblemNode)pn;

        if (activeNode == curNode) {
            return;
        } else if (activeNode == null) {
            // This is the root node.
            assert(curNode.getDepth() == 0);
            // TODO: add support for deltas at the root node.
            assert(curNode.getDeltas() == null);
            activeNode = curNode;
            return;
        }
        DmvProblemNode prevNode = activeNode;
        activeNode = curNode;

        // Get sequence of deltas to be forward applied to the current relaxation.
        List<CptBoundsDeltaList> deltasList = DmvProblemNode.getDeltasBetween(prevNode, curNode);

        // Forward apply the deltas.
        for (CptBoundsDeltaList deltas : deltasList) {
            forwardApply(deltas);
        }
    }

    // Copied from DmvDantzigWolfeRelaxation.
    @Override
    public DmvProblemNode getActiveNode() {
        return activeNode;
    }
    
    private Triple<RelaxStatus,Double,WarmStart> runSimplexAlgo(IloCplex cplex2, double upperBound, int depth) throws IloException {
        if (!isFeasible()) {
            return new Triple<RelaxStatus,Double,WarmStart>(RelaxStatus.Infeasible, INTERNAL_WORST_SCORE, null);
        }
        
        int maxCutRounds = (depth == 0) ? prm.rootMaxCutRounds  : prm.maxCutRounds;
        
        RelaxStatus status = RelaxStatus.Unknown;
        DoubleArrayList cutIterLowerBounds = new DoubleArrayList();
        ArrayList<Status> cutIterStatuses = new ArrayList<Status>();
        WarmStart warmStart = null;
        cutIterLowerBounds.add(INTERNAL_BEST_SCORE);        
        int totalSimplexIterations = 0;
        
        // Ensures that we stop early if we can fathom the node. We use the
        // upper limit because the dual problem (which we're solving) is a
        // maximization.
        cplex.setParam(DoubleParam.ObjULim, upperBound);
        
        // Time from the start, stopping early if we run out of time.
        Timer timer = new Timer();
        int cut;
        // Solve the full LP problem
        for (cut = 0; ;) {
            timer.start();

            if (prm.tempDir != null) {
                cplex.exportModel(new File(prm.tempDir, "rlt.lp").getAbsolutePath());
            }
            
            // Solve the master problem
            simplexTimer.start();
            boolean hasFeasSol = cplex.solve();
            simplexTimer.stop();

            // Get CPLEX status.
            status = RelaxStatus.getForLp(cplex);
            log.debug("LP solution status: " + cplex.getStatus());
            log.debug("LP CPLEX status: " + cplex.getCplexStatus());
            log.debug("Proven dual feasibility? " +  cplex.isDualFeasible());
            log.debug("Proven primal feasibility? " +  cplex.isPrimalFeasible());
            log.debug("Has feasible solution? " + hasFeasSol);
            
            // Get the lower bound. 
            double lowerBound;
            if (status == RelaxStatus.Unknown) {
                // Do not call cplex.getObjValue() because that it might throw an error.
                lowerBound = INTERNAL_BEST_SCORE;
            } else if (status == RelaxStatus.Infeasible) {
                lowerBound = INTERNAL_WORST_SCORE;
            } else { // if (status == RelaxStatus.Optimal || status == RelaxStatus.Feasible || status == RelaxStatus.Pruned) {                
                // Get the lower bound from CPLEX. Because we explicitly use the Dual simplex
                // algorithm, the objective value is the lower bound, even if we
                // terminate early.
                lowerBound = cplex.getObjValue();
                log.trace("Simplex solution value: " + lowerBound);
                double prevLowerBound = cutIterLowerBounds.size() > 0 ? cutIterLowerBounds.get(cutIterLowerBounds.size() - 1)
                        : INTERNAL_WORST_SCORE;
                if (!Utilities.lte(prevLowerBound, lowerBound, prm.OBJ_VAL_DECREASE_TOLERANCE)) {
                    Status prevStatus = cutIterStatuses.size() > 0 ? cutIterStatuses.get(cutIterLowerBounds.size() - 1)
                            : Status.Unknown;
                    log.warn(String.format("Lower bound should monotonically increase: prev=%f cur=%f. prevStatus=%s curStatus=%s.", prevLowerBound, lowerBound, prevStatus, cplex.getStatus()));
                }
                if( cplex.getCplexStatus() == CplexStatus.AbortObjLim && lowerBound < upperBound) {
                    log.warn(String.format("Lower bound %f should >= upper bound %f.", lowerBound, upperBound));
                }
                
                // Update status if this node can be fathomed.
                if (lowerBound >= upperBound) {
                    status = RelaxStatus.Pruned;
                }

                if (prm.tempDir != null) {
                    // Write solution to a file.
                    cplex.writeSolution(new File(prm.tempDir, "rlt.sol").getAbsolutePath());
                }
                
                // Store the warm start information.
                warmStart = getWarmStart();
            } 
            
            // Record the values for this iteration.
            cutIterLowerBounds.add(lowerBound);
            cutIterStatuses.add(cplex.getStatus());
            totalSimplexIterations += cplex.getNiterations();
            log.debug(String.format("Iteration lower bounds (cut=%d): %s", cut, cutIterLowerBounds));

            timer.stop();
            if (status == RelaxStatus.Unknown || status == RelaxStatus.Infeasible 
                    || status == RelaxStatus.Pruned || timer.totSec() > prm.timeoutSeconds) {
                // Terminate because we have either:
                // - Hit a CPLEX error.
                // - Found an infeasible solution.
                // - Are able to fathom this node. 
                // - Run out of time. 
                break;
            } else if (cut < maxCutRounds) {
                // Try to add cuts based on the optimal or feasible solution found.
                int numCutAdded = addCuts(cplex, cut);
                log.debug("Added cuts " + numCutAdded + ", round " + cut);
                if (numCutAdded == 0) {
                    // Terminate: no new cuts are needed
                    log.debug("No more cut rounds needed after " + cut + " rounds");
                    break;
                }
                cut++;
            } else {
                // Terminate: Optimal or feasible solution found, but no cut rounds left.
                break;
            }
        }
        
        // The lower bound should be strictly increasing, because we add cuts. We still
        // keep track of the lower bounds in case we terminate early.
        double lowerBound = Vectors.max(cutIterLowerBounds.toNativeArray());
        
        log.debug("Number of cut rounds: " + cut);
        log.debug("Final lower bound: " + lowerBound);
        log.debug(String.format("Iteration lower bounds (cut=%d): %s", cut, cutIterLowerBounds));
        log.debug("Iteration statuses: " + cutIterStatuses);
        log.debug("Avg switch time(ms) per solve: " + switchTimer.totMs() / numSolves);
        log.debug("Avg simplex time(ms) per solve: " + simplexTimer.totMs() / numSolves);
        log.debug("Total # simplex iterations: " + totalSimplexIterations);
        
        // Subtract off the STO cuts b/c they are in the same matrix.
        int origCons = mp.origMatrix.getNrows() - sto.getNumStoCons();
        log.info(String.format("Summary: #cuts=%d #origCons=%d #rltCons=%d", 
                sto.getNumStoCons(), origCons, mp.rlt.getRltMatrix().getNrows()));
    
        return new Triple<RelaxStatus,Double,WarmStart>(status, lowerBound, warmStart);
    }

    private int addCuts(IloCplex cplex, int cut) throws UnknownObjectException, IloException {
        IntArrayList rows = new IntArrayList(); 

        if (prm.addBindingCons) {
            // TODO: add binding bounds as factors too.
            // TODO: This will fail because it won't be a consecutive list of rows.
            double[] vars = cplex.getValues(mp.origMatrix);
            IloNumVar[] numVars = mp.origMatrix.getNumVars();
            numVars[0].getLB();
//            cplex.
//            cplex.getSlacks(double );
            
            // Binding constraints have a slack of zero.
            double[] slacks = cplex.getSlacks(mp.origMatrix);
            for (int i=0; i<slacks.length; i++) {
                if (Utilities.equals(slacks[i], 0.0, 1e-8)) {
                    // This is a binding constraint.
                    rows.add(i);
                }
            }
            log.debug(String.format("Proportion of constraints binding: %f (%d / %d)", 
                        (double) rows.size() / slacks.length, rows.size(), slacks.length));
        }
        
        // Add sum-to-one cuts.
        rows.add(sto.projectModelParamsAndAddCuts().toNativeArray());
        
        int rltCuts = 0;
        if (mp.drMatrix == null) {
            // Add RLT cuts only if we are NOT reducing the dimensionality.
            rltCuts = mp.rlt.addRowsAsFactors(rows);
        }
        // TODO: Update low-dimensional matrix with cuts, and then add to RLT.
        
        return rows.size() + rltCuts;
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
        
        return new DmvRelaxedSolution(Utilities.copyOf(optimalLogProbs), treebank, objective, status, Utilities
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
    
    @Override
    public void updateTimeRemaining(double timeoutSeconds) {
        prm.timeoutSeconds = timeoutSeconds;
        CplexPrm.updateTimeoutSeconds(cplex, timeoutSeconds);
    }
}

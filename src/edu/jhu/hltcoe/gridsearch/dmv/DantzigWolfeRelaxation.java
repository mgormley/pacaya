package edu.jhu.hltcoe.gridsearch.dmv;

import gnu.trove.TDoubleArrayList;
import ilog.concert.IloException;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BasisStatus;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Time;

public abstract class DantzigWolfeRelaxation implements DmvRelaxation {

    static Logger log = Logger.getLogger(DantzigWolfeRelaxation.class);

    protected static final double NEGATIVE_REDUCED_COST_TOLERANCE = -1e-5;
    protected IloCplex cplex;
    protected File tempDir;
    
    private static final double OBJ_VAL_DECREASE_TOLERANCE = 1.0;
    private static final double INTERNAL_BEST_SCORE = Double.NEGATIVE_INFINITY;
    private static final double INTERNAL_WORST_SCORE = Double.POSITIVE_INFINITY;
    private double workMemMegs;
    private int numThreads;
    private int maxSimplexIterations;
    private int maxDwIterations;
    private int maxCutRounds;
    private int numSolves;
    private Stopwatch simplexTimer;

    public static class SubproblemRetVal {
        public double sumReducedCosts;
        public int numPositiveRedCosts;
        public boolean isInfeasible;
        
        public SubproblemRetVal(double sumReducedCosts, int numPositiveRedCosts, boolean isInfeasible) {
            this.sumReducedCosts = sumReducedCosts;
            this.numPositiveRedCosts = numPositiveRedCosts;
            this.isInfeasible = isInfeasible;
        }
    
    }

    public DantzigWolfeRelaxation(File tempDir, int maxCutRounds) {
        // TODO: pass these through
        this.tempDir = tempDir;
        this.workMemMegs = 256;
        this.numThreads = 1;
        this.maxSimplexIterations = 2100000000;
        this.maxDwIterations = 1000;
        this.maxCutRounds = maxCutRounds;
        this.numSolves = 0;
        this.simplexTimer = new Stopwatch();
    }

    public void init(DmvSolution initFeasSol) {
        try {
            this.cplex = new IloCplex();
            buildModel(cplex, initFeasSol);
            setCplexParams(cplex);
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RelaxedDmvSolution solveRelaxation() {
        return solveRelaxation(LazyBranchAndBoundSolver.WORST_SCORE);
    }

    public RelaxedDmvSolution solveRelaxation(double incumbentScore) {
        try {
            clearRelaxedSolution();
            numSolves++;
            // Negate since we're minimizing internally
            double upperBound = -incumbentScore;
            Pair<RelaxStatus,Double> pair = runDWAlgo(cplex, upperBound);
            RelaxStatus status = pair.get1();
            double lowerBound = pair.get2();
            
            // Negate the objective since we were minimizing 
            double objective = -lowerBound;
            assert(!Double.isNaN(objective));
            // This won't always be true if we are stopping early: 
            // assert(Utilities.lte(objective, 0.0, 1e-7));
            
            if (tempDir != null) {
                cplex.exportModel(new File(tempDir, "dw.lp").getAbsolutePath());
            }
            
            log.info("Solution status: " + status);
            if (!status.hasSolution()) {
                return new RelaxedDmvSolution(null, null, null, objective, status);
            }
            
            if (tempDir != null) {
                cplex.writeSolution(new File(tempDir, "dw.sol").getAbsolutePath());
            }
            log.info("Lower bound: " + lowerBound);
            return extractSolution(status, objective);
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    public void setWarmStart(WarmStart warmStart) {
        try {
//            // Initialize all the lambda variables to their lower bounds.
//            IloNumVar[] lambdaVars = new IloNumVar[mp.lambdaVars.size()];
//            for (int i=0; i<lambdaVars.length; i++) {
//                lambdaVars[i] = mp.lambdaVars.get(i).lambdaVar;
//            }
//            BasisStatus[] lambdaStatuses = new BasisStatus[lambdaVars.length];
//            Arrays.fill(lambdaStatuses, BasisStatus.AtLower);
//            cplex.setBasisStatuses(lambdaVars, lambdaStatuses, new IloRange[0], new BasisStatus[0]);
            
            // TODO: this is painfully slow.
            HashSet<IloNumVar> knownVars = new HashSet<IloNumVar>(Arrays.asList(warmStart.numVars));
            ArrayList<IloNumVar> unknownVars = getUnknownVars(knownVars);
            
            IloNumVar[] allVars = new IloNumVar[knownVars.size() + unknownVars.size()];
            BasisStatus[] allStatuses = new BasisStatus[allVars.length];

            // Fill the entire status array with AtLower.
            Arrays.fill(allStatuses, BasisStatus.AtLower);
            // Fill the beginning of the array with the known variables.
            System.arraycopy(warmStart.numVars, 0, allVars, 0, warmStart.numVars.length);
            System.arraycopy(warmStart.numVarStatuses, 0, allStatuses, 0, warmStart.numVarStatuses.length);
            // Fill the rest of the array with the new variables.
            System.arraycopy(unknownVars.toArray(), 0, allVars, warmStart.numVars.length, unknownVars.size());
            
            // Set the basis status of known and unknown variables
            cplex.setBasisStatuses(allVars, allStatuses, warmStart.ranges, warmStart.rangeStatuses);            
            
            //TODO: remove: cplex.setBasisStatuses(warmStart.numVars, warmStart.numVarStatuses, warmStart.ranges, warmStart.rangeStatuses);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private void setCplexParams(IloCplex cplex) throws IloException, FileNotFoundException {
        // Specifies an upper limit on the amount of central memory, in
        // megabytes, that CPLEX is permitted to use for working memory
        // before swapping to disk files, compressing memory, or taking
        // other actions.
        // Values: Any nonnegative number, in megabytes; default: 128.0
        cplex.setParam(DoubleParam.WorkMem, workMemMegs);
        //cplex.setParam(StringParam.WorkDir, tempDir.getAbsolutePath());
    
        cplex.setParam(IntParam.Threads, numThreads);
    
        // -1 = oportunistic, 0 = auto (default), 1 = deterministic
        // In this context, deterministic means that multiple runs with
        // the
        // same model at the same parameter settings on the same
        // platform
        // will reproduce the same solution path and results.
        cplex.setParam(IntParam.ParallelMode, 1);
    
        // From the CPLEX documentation: the Dual algorithm can take better advantage of a previous basis
        // after adding new constraints. 
        // http://ibm.co/GHorLT
        // However it may be that Primal can better take advantage of a feasible basis after adding 
        // new variables.
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);
        
        // Note: we'd like to reuse basis information by explicitly storing it
        // with the Fork nodes as in SCIP. However, this is only possible if the
        // number of rows/columns in the problem remains the same, which it will
        // not for our master problem.
        // http://ibm.co/GCQ709
        // By default, the solver will make use of basis information internally 
        // even when we update the problem. This is (hopefully) good enough.
    
        // TODO: For v12.3 only: cplex.setParam(IntParam.CloneLog, 1);
        
        cplex.setParam(IntParam.ItLim, maxSimplexIterations);
        
        // For continuous models solved with simplex, setting 1 (one) will use the 
        // currently loaded basis. If a basis is available only for the original, unpresolved 
        // model, or if CPLEX has a start vector rather than a simplex basis, then the 
        // simplex algorithm will proceed on the unpresolved model. With setting 2, 
        // CPLEX will first perform presolve on the model and on the basis or start vector, 
        // and then proceed with optimization on the presolved problem.
        cplex.setParam(IntParam.AdvInd, 1);
        
        // Whether or not to presolve.
        //cplex.setParam(BooleanParam.PreInd, false);
        
        //        OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, "cplex.log")));
        //        cplex.setOut(out);
        //         cplex.setWarning(out);
    }

    public Pair<RelaxStatus, Double> runDWAlgo(IloCplex cplex, double upperBound) throws UnknownObjectException, IloException {
        if (!isFeasible()) {
            return new Pair<RelaxStatus,Double>(RelaxStatus.Infeasible, INTERNAL_WORST_SCORE);
        }
        
        RelaxStatus status = RelaxStatus.Unknown;
        TDoubleArrayList iterationLowerBounds = new TDoubleArrayList();
        TDoubleArrayList iterationObjVals = new TDoubleArrayList();
        ArrayList<Status> iterationStatus = new ArrayList<Status>();
        WarmStart warmStart = null;
        iterationLowerBounds.add(INTERNAL_BEST_SCORE);        
        
        int cut;
        int dwIter;
        // Solve the full D-W problem
        for (dwIter = 0, cut = 0; ; dwIter++) {
            if (tempDir != null) {
                cplex.exportModel(new File(tempDir, String.format("dw.%d.lp", dwIter)).getAbsolutePath());
            }
            
            // Solve the master problem
            if (warmStart != null) {
                setWarmStart(warmStart);
            }
            simplexTimer.start();
            cplex.solve();
            simplexTimer.stop();
            warmStart = getWarmStart();
            status = RelaxStatus.get(cplex.getStatus()); 
            
            log.trace("Master solution status: " + cplex.getStatus());
            if (status == RelaxStatus.Infeasible) {
                return new Pair<RelaxStatus,Double>(status, INTERNAL_WORST_SCORE);
            }
            if (dwIter>=maxDwIterations) {
                break;
            }
            if (tempDir != null) {
                cplex.writeSolution(new File(tempDir, String.format("dw.%d.sol", dwIter)).getAbsolutePath());
            }
            double objVal = cplex.getObjValue();
            log.trace("Master solution value: " + objVal);
            double prevObjVal = iterationObjVals.size() > 0 ? iterationObjVals.get(iterationObjVals.size() - 1)
                    : INTERNAL_WORST_SCORE;
            if (objVal > prevObjVal + OBJ_VAL_DECREASE_TOLERANCE) {
                Status prevStatus = iterationStatus.size() > 0 ? iterationStatus.get(iterationObjVals.size() - 1)
                        : Status.Unknown;
                log.warn(String.format("Master problem objective should monotonically decrease: prev=%f cur=%f. prevStatus=%s curStatus=%s.", prevObjVal, objVal, prevStatus, cplex.getStatus()));
                throw new IllegalStateException("Master problem objective should monotonically decrease");
            }
            iterationObjVals.add(objVal);
            iterationStatus.add(cplex.getStatus());
    
            SubproblemRetVal sprv = addColumns(cplex);
            if (sprv.isInfeasible) {
                return new Pair<RelaxStatus,Double>(RelaxStatus.Infeasible, INTERNAL_WORST_SCORE);
            }
            
            double lowerBound = objVal + sprv.sumReducedCosts;
            iterationLowerBounds.add(lowerBound);
            
            // Check whether to continue
            if (lowerBound >= upperBound) {
                // We can fathom this node
                status = RelaxStatus.Fathomed;
                break;
            } else if (sprv.numPositiveRedCosts == 0) {
                // Optimal solution found
                lowerBound = cplex.getObjValue();
                status = RelaxStatus.Optimal;
                
                if (cut < maxCutRounds) {
                    int numCutAdded = addCuts(cplex, iterationObjVals, iterationStatus, cut);
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
            } else {
                // Non-optimal solution, continuing
            }
        }
        
        // The lower bound oscillates because of the yo-yo effect, so we take the max.
        double lowerBound = Vectors.max(iterationLowerBounds.toNativeArray());
        
        log.debug("Number of cut rounds: " + cut);
        log.debug("Number of DW iterations: " + dwIter);
        log.debug("Max number of DW iterations: " + maxDwIterations);
        log.debug("Final lower bound: " + lowerBound);
        log.debug(String.format("Iteration objective values (cut=%d): %s", cut, iterationObjVals));
        log.debug("Iteration lower bounds: " + iterationLowerBounds);
        log.debug("Avg simplex time(ms) per solve: " + Time.totMs(simplexTimer) / numSolves);
        printSummary();
    
        return new Pair<RelaxStatus,Double>(status, lowerBound);
    }
    
    public void end() {
        cplex.end();
    }

    public int getNumSolves() {
        return numSolves;
    }
    
    public void setMaxSimplexIterations(int maxSimplexIterations) {
        this.maxSimplexIterations = maxSimplexIterations;
    }

    public void setMaxDwIterations(int maxDwIterations) {
        this.maxDwIterations = maxDwIterations;
    }
    
    protected abstract void clearRelaxedSolution();

    protected abstract RelaxedDmvSolution extractSolution(RelaxStatus status, double objective)  throws UnknownObjectException, IloException;
    
    protected abstract ArrayList<IloNumVar> getUnknownVars(HashSet<IloNumVar> knownVars);

    protected abstract void printSummary();

    protected abstract int addCuts(IloCplex cplex, TDoubleArrayList iterationObjVals, ArrayList<Status> iterationStatus, int cut)  throws UnknownObjectException, IloException;

    protected abstract SubproblemRetVal addColumns(IloCplex cplex)  throws UnknownObjectException, IloException;

    public abstract WarmStart getWarmStart();

    protected abstract boolean isFeasible();

    protected abstract void buildModel(IloMPModeler cplex, DmvSolution initFeasSol) throws IloException;

}
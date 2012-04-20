package edu.jhu.hltcoe.gridsearch.dmv;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
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
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class DmvDantzigWolfeRelaxation {

    static final double MIN_SUM_FOR_CUT = 1.01;

    private static Logger log = Logger.getLogger(DmvDantzigWolfeRelaxation.class);

    private DmvBounds bounds;
    private File tempDir;
    private double workMemMegs;
    private int numThreads;
    private SentenceCollection sentences;
    private IndexedDmvModel idm;
    private int numLambdas;
    private int cutCounter;
    private IloCplex cplex;
    private MasterProblem mp;
    private Projections projections;
    private int numCutRounds;
    private CutCountComputer initCutCountComp;
    
    public DmvDantzigWolfeRelaxation(SentenceCollection sentences, File tempDir,
            int numCutRounds, CutCountComputer initCutCountComp) {
        this.sentences = sentences;
        this.tempDir = tempDir;

        this.idm = new IndexedDmvModel(sentences);
        this.bounds = new DmvBounds(this.idm);
        this.projections = new Projections(tempDir);
        
        // TODO: pass these through
        this.numThreads = 1;
        this.workMemMegs = 256;
        this.numCutRounds = numCutRounds;
        this.initCutCountComp = initCutCountComp;
        // Counter for printing 
        this.cutCounter = 0;
    }

    public void init(DepTreebank initFeasSol) {
        try {
            cplex = new IloCplex();
            mp = buildModel(cplex, initFeasSol);
            // TODO: add the initial feasible solution to cplex object? Does this even make sense?
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
        try {            
            runDWAlgo(cplex, mp);
            
            if (tempDir != null) {
                cplex.exportModel(new File(tempDir, "dw.lp").getAbsolutePath());
                cplex.writeSolution(new File(tempDir, "dw.sol").getAbsolutePath());
            }
            
            log.info("Solution status: " + cplex.getStatus());
            if (cplex.getStatus() != Status.Optimal) {
                return new RelaxedDmvSolution(null, null, null, LazyBranchAndBoundSolver.WORST_SCORE, cplex.getStatus());
            }
            log.info("Solution value: " + cplex.getObjValue());
            log.info(String.format("Summary: #lambdas=%d #cuts=%d", mp.lambdaVars.size(), mp.numStoCons));
            
            // Negate the objective since we were minimizing 
            double objective = -cplex.getObjValue();
            assert(!Double.isNaN(objective));
            assert(Utilities.lte(objective, 0.0, 1e-7));

            // Store optimal model parameters
            double[][] logProbs = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                logProbs[c] = cplex.getValues(mp.modelParamVars[c]);
            }

            // Store fractional corpus parse
            double[][] fracRoots = new double[sentences.size()][];
            double[][][] fracParses = new double[sentences.size()][][];
            for (int s = 0; s < sentences.size(); s++) {
                Sentence sentence = sentences.get(s);
                fracRoots[s] = new double[sentence.size()];
                fracParses[s] = new double[sentence.size()][sentence.size()];
            }
            for (LambdaVar triple : mp.lambdaVars) {
                double frac = cplex.getValue(triple.lambdaVar);
                int s = triple.s;
                int[] parents = triple.parents;

                double[] fracRoot = fracRoots[s];
                double[][] fracParse = fracParses[s];
                for (int child = 0; child < parents.length; child++) {
                    int parent = parents[child];
                    if (parent == WallDepTreeNode.WALL_POSITION) {
                        fracRoot[child] += frac;
                    } else {
                        fracParse[parent][child] += frac;
                    }
                }
            }
            
            return new RelaxedDmvSolution(logProbs, fracRoots, fracParses, objective, cplex.getStatus());
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    public double[][] getRegretCm() {
        try {
            // TODO: getting the model parameters in this way is redundant
            // Store optimal model parameters \theta_{c,m}
            double[][] logProbs = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                logProbs[c] = cplex.getValues(mp.modelParamVars[c]);
            }

            // Store feature counts \bar{f}_{c,m} (i.e. number of times each
            // model parameter was used)
            double[][] featCounts = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                featCounts[c] = new double[idm.getNumParams(c)];
            }

            for (LambdaVar triple : mp.lambdaVars) {
                double frac = cplex.getValue(triple.lambdaVar);
                int s = triple.s;
                int[] sentSol = triple.sentSol;
                for (int i = 0; i < sentSol.length; i++) {
                    int c = idm.getC(s, i);
                    int m = idm.getM(s, i);
                    featCounts[c][m] += sentSol[i] * frac;
                }
            }

            // Store objective values z_{c,m}
            double[][] objVals = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                objVals[c] = cplex.getValues(mp.objVars[c]);
            }

            // Compute the regret as the difference between the
            // objective value and true objective value
            double[][] regret = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                regret[c] = new double[idm.getNumParams(c)];
                for (int m = 0; m < idm.getNumParams(c); m++) {
                    regret[c][m] = objVals[c][m] - (logProbs[c][m] * featCounts[c][m]);
                    assert(Utilities.gte(regret[c][m], 0.0, 1e-7));
                }
            }

            return regret;
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

        // From the CPLEX documentation: the Dual algorithm can take better advantage of a previous solve. 
        // http://ibm.co/GHorLT
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Dual);
        
        // Note: we'd like to reuse basis information by explicitly storing it
        // with the Fork nodes as in SCIP. However, this is only possible if the
        // number of rows/columns in the problem remains the same, which it will
        // not for our master problem.
        // http://ibm.co/GCQ709
        // By default, the solver will make use of basis information internally 
        // even when we update the problem. This is (hopefully) good enough.

        // TODO: For v12.3 only: cplex.setParam(IntParam.CloneLog, 1);
        
//        OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, "cplex.log")));
//        cplex.setOut(out);
//        cplex.setWarning(out);
    }

    /**
     * Convenience class for passing around Master Problem variables
     */
    private static class MasterProblem {
        public IloObjective objective;
        public IloRange[] lambdaSumCons;
        public List<LambdaVar> lambdaVars;
        public HashSet<LambdaVar> lambdaVarSet;
        public IloNumVar[][] modelParamVars;
        public IloNumVar[][] objVars;
        public IloRange[][] couplConsLower;
        public IloRange[][] couplConsUpper;
        public IloLPMatrix couplMatrix;
        public int numStoCons = 0;
    }
    
    /**
     * Convenience class for storing the columns generated by
     * Dantzig-Wolfe and their corresponding parses.
     */
    private static class LambdaVar {
        
        public IloNumVar lambdaVar;
        public int s;
        public int[] parents;
        private int[] sentSol;
        public int colind;
        
        public LambdaVar(IloNumVar lambdaVar, int s, int[] parents, int[] sentSol, int colind) {
            super();
            this.lambdaVar = lambdaVar;
            this.s = s;
            this.parents = parents;
            this.sentSol = sentSol;
            this.colind = colind;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(parents);
            result = prime * result + s;
            return result;
        }

        /**
         * Two LambdaVar objects are equal if their sentence index, s, and their
         * parents array are equal.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LambdaVar other = (LambdaVar) obj;
            if (!Arrays.equals(parents, other.parents))
                return false;
            if (s != other.s)
                return false;
            return true;
        }
                
    }

    private MasterProblem buildModel(IloMPModeler cplex, DepTreebank initFeasSol) throws IloException {
        mp = new MasterProblem();
        
        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        // Create the model parameter variables
        int numConds = idm.getNumConds();
        mp.modelParamVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            mp.modelParamVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m = 0; m < mp.modelParamVars[c].length; m++) {
                mp.modelParamVars[c][m] = cplex.numVar(bounds.getLb(c, m), bounds.getUb(c, m), idm.getName(c, m));
            }
        }

        // Create the cut vectors for sum-to-one constraints
        double[][][] pointsArray = getInitialPoints();
        // Add the initial cuts
        for (int c = 0; c < numConds; c++) {
            for (int i = 0; i < pointsArray[c].length; i++) {
                double[] probs = pointsArray[c][i];
                addSumToOneConstraint(cplex, c, probs);
            }
        }

        // Create the objective
        mp.objective = cplex.addMinimize();
        // Create the objective variables, adding them to the objective
        mp.objVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            mp.objVars[c] = new IloNumVar[numParams];
            for (int m=0; m<numParams; m++) {
                mp.objVars[c][m] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, String.format("z_{%d,%d}",c,m));
                // Negate the objVars since we are minimizing
                cplex.setLinearCoef(mp.objective, -1.0, mp.objVars[c][m]);
            }
        }
        
        // Add the coupling constraints considering only the model parameters
        // aka. the relaxed-objective-coupling-constraints
        mp.couplConsLower = new IloRange[numConds][];
        mp.couplConsUpper = new IloRange[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            mp.couplConsLower[c] = new IloRange[numParams];
            mp.couplConsUpper[c] = new IloRange[numParams];
            for (int m=0; m<numParams; m++) {
                String name;
                
                // Add the lower coupling constraint
                IloNumVar slackVarLower = cplex.numVar(-Double.MAX_VALUE, 0.0, String.format("slackVarLower_{%d,%d}",c,m));
                name = String.format("ccLb(%d,%d)", c, m);   
                double maxFreqCm = idm.getTotalMaxFreqCm(c,m);
                IloNumExpr rhsLower = cplex.sum(slackVarLower,
                                        cplex.diff(cplex.prod(maxFreqCm, mp.modelParamVars[c][m]), mp.objVars[c][m]));
                mp.couplConsLower[c][m] = cplex.eq(maxFreqCm * bounds.getLb(c,m), rhsLower, name);
                
                // Add the upper coupling constraint
                IloNumVar slackVarUpper = cplex.numVar(-Double.MAX_VALUE, 0.0, String.format("slackVarUpper_{%d,%d}",c,m));
                name = String.format("ccUb(%d,%d)", c, m);
                IloNumExpr rhsUpper = cplex.sum(cplex.prod(-1.0, mp.objVars[c][m]), slackVarUpper);
                mp.couplConsUpper[c][m] = cplex.eq(0.0, rhsUpper, name);
            }
        }        
        // We need the lower coupling constraints (and the upper) to each 
        // be added in sequence to the master problem. So we add all the upper
        // constraints afterwards
        mp.couplMatrix = cplex.addLPMatrix("couplingMatrix");
        for (int c = 0; c < numConds; c++) {
            mp.couplMatrix.addRows(mp.couplConsLower[c]);
        }
        for (int c = 0; c < numConds; c++) {
            mp.couplMatrix.addRows(mp.couplConsUpper[c]);
        }

        // ----- column-wise modeling -----

        // Create the lambda sum to one constraints
        mp.lambdaSumCons = new IloRange[sentences.size()];
        mp.lambdaVars = new ArrayList<LambdaVar>();
        mp.lambdaVarSet = new HashSet<LambdaVar>();

        // Add the initial feasible parse as the first lambda columns
        for (int s = 0; s < sentences.size(); s++) {
            DepTree tree = initFeasSol.get(s);
            addLambdaVar(cplex, s, tree);
        }
        
        return mp;
    }

    public static class CutCountComputer {
        public int getNumCuts(int numParams) {
            return (int)Math.pow(numParams, 2.0);
        }
    }
    
    private double[][][] getInitialPoints() throws IloException {
        int numConds = idm.getNumConds();
        double[][][] vectors = new double[numConds][][];

        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            // Create numParams^2 vectors
            int numVectors = initCutCountComp.getNumCuts(numParams); 
            vectors[c] = new double[numVectors][];
            for (int i = 0; i < vectors[c].length; i++) {
                double[] vector = new double[numParams];
                // Randomly initialize the parameters
                for (int m = 0; m < numParams; m++) {
                    vector[m] = Prng.nextDouble();
                }
                vectors[c][i] = vector;
            }
        }
        return vectors;
    }

    private void addSumToOneConstraint(IloMPModeler cplex, int c,
            double[] point) throws IloException {
        
        // TODO: should this respect the bounds?
        //double[] probs = projections.getProjectedParams(bounds, c, point);
        double[] probs = Projections.getProjectedParams(point);
        double[] logProbs = Vectors.getLog(probs);
        
        double vectorSum = 1.0;
        for (int m = 0; m < logProbs.length; m++) {
            if (probs[m] > 0.0) {
                // Otherwise we'd get a NaN
                vectorSum += (logProbs[m] - 1.0) * probs[m];
            }
        }

        IloLinearNumExpr vectorExpr = cplex.scalProd(probs, mp.modelParamVars[c]);
        cplex.addLe(vectorExpr, vectorSum, String.format("maxVar(%d)-%d", c, cutCounter++));
        mp.numStoCons++;
    }

    private boolean addLambdaVar(IloMPModeler cplex, int s, DepTree tree)
            throws IloException {

        LambdaVar lvTemp = new LambdaVar(null, s, tree.getParents(), null, -1);
        if (mp.lambdaVarSet.contains(lvTemp)) {
            //int lvIdx = mp.lambdaVars.lastIndexOf(lvTemp);
            //log.error("Duplicate Lambda Var: " + lvTemp);
            //throw new IllegalStateException("Duplicate LambdaVar already in the master problem");
            //log.warn("Duplicate LambdaVar already in the master problem");
            
            // Don't add the duplicate, since this probably just means its reduced cost is really close to zero
            return false;
        }
        
        int[] sentSol = idm.getSentSol(sentences.get(s), s, tree);
        int numSentVars = idm.getNumSentVars(s);
        
        IloColumn lambdaCol = cplex.column(mp.objective, 0.0);

        // Add the lambda var to its sum to one constraint
        IloNumVar lambdaVar;
        if (mp.lambdaSumCons[s] == null) {
            //TODO: lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
            lambdaVar = cplex.numVar(lambdaCol, 0.0, Double.MAX_VALUE, String.format("lambda_{%d}^{%d}", s, numLambdas++));
            mp.lambdaSumCons[s] = cplex.addEq(lambdaVar, 1.0, "lambdaSum");
        } else {
            lambdaCol = lambdaCol.and(cplex.column(mp.lambdaSumCons[s], 1.0));
            //TODO:lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
            lambdaVar = cplex.numVar(lambdaCol, 0.0, Double.MAX_VALUE, String.format("lambda_{%d}^{%d}", s, numLambdas++));
        }
        
        // Add the lambda var to the relaxed-objective-coupling-constraints matrix
        int[] ind = new int[numSentVars*2];
        double[] val = new double[numSentVars*2];
        int j=0;
        for (int i = 0; i < numSentVars; i++) {
            int c = idm.getC(s, i);
            int m = idm.getM(s, i);
            
            // Add to the lower coupling constraint
            ind[j] = mp.couplMatrix.getIndex(mp.couplConsLower[c][m]);
            val[j] = bounds.getLb(c, m) * sentSol[i];
            j++;
            
            // Add to the upper coupling constraint
            ind[j] = mp.couplMatrix.getIndex(mp.couplConsUpper[c][m]);
            val[j] = bounds.getUb(c, m) * sentSol[i];
            j++;
        }
        int colind = mp.couplMatrix.addColumn(lambdaVar, ind, val);
        
        LambdaVar lv = new LambdaVar(lambdaVar, s, tree.getParents(), sentSol, colind);
        
        mp.lambdaVars.add(lv);
        mp.lambdaVarSet.add(lv);
        
//        // TODO: remove
//        for (int i = 0; i < couplCons[s].length; i++) {
//            log.trace(couplCons[s][i]);
//        }
        return true;
    }

    public void runDWAlgo(IloCplex cplex, MasterProblem mp) throws UnknownObjectException, IloException {        
        DmvCkyParser parser = new DmvCkyParser();
        
        double prevObjVal = Double.POSITIVE_INFINITY;
        // Outer loop runs D-W and then adds cuts for sum-to-one constraints
        for (int cut=0; cut<numCutRounds; cut++) {
        
            // Solve the full D-W problem
            while (true) {
                // Solve the master problem
                cplex.solve();

                log.trace("Master solution status: " + cplex.getStatus());
                if (tempDir != null) {
                    // TODO: remove this or add a debug flag to the if
                    cplex.exportModel(new File(tempDir, "dw.lp").getAbsolutePath());
                    cplex.writeSolution(new File(tempDir, "dw.sol").getAbsolutePath());
                }
                if (cplex.getStatus() == Status.Infeasible) {
                    return;
                }
                double objVal = cplex.getObjValue();
                log.trace("Master solution value: " + objVal);
                if (objVal > prevObjVal) {
                    //throw new IllegalStateException("Master problem objective should monotonically decrease");
                    log.warn("Master problem objective should monotonically decrease: prev=" + prevObjVal + " cur=" + objVal);
                }
                prevObjVal = objVal;
                                
                // Get the simplex multipliers (shadow prices).
                // These are shared across all slaves, since each slave
                // has the same D_s matrix. 
                double[] pricesLower = cplex.getDuals(mp.couplMatrix, 0, idm.getNumTotalParams());
                double[] pricesUpper = cplex.getDuals(mp.couplMatrix, idm.getNumTotalParams(), idm.getNumTotalParams());
                int numConds = idm.getNumConds();
                double[][] weights = new double[numConds][];
                int j = 0;
                for (int c = 0; c < numConds; c++) {
                    int numParams = idm.getNumParams(c);
                    weights[c] = new double[numParams];
                    
                    for (int m=0; m<numParams; m++) {
                        // Calculate new model parameter values for parser
                        // based on the relaxed-objective-coupling-constraints
                        weights[c][m] = (pricesLower[j]*bounds.getLb(c,m) + pricesUpper[j]*bounds.getUb(c,m));
                        j++;
                        // We want to minimize the following:
                        // c^T - q^T D_s = - (pricesLower[m]*bounds.getLb(c,m) + pricesUpper[m]*bounds.getUb(c,m))
                        // but we negate this since the parser will try to maximize. In other words:
                        // minimize -q^T D_s = maximize q^T D_s
                        // 
                    }
                }

                DepProbMatrix dpm = idm.getDepProbMatrix(weights);

                // Get the simplex multipliers (shadow prices) for the lambda sentence constraints
                double[] convexPrices = cplex.getDuals(mp.lambdaSumCons);
                
                // Solve each slave problem
                int numPositiveRedCosts = 0;
                for (int s = 0; s < sentences.size(); s++) {
        
                    Pair<DepTree, Double> pair = parser.parse(sentences.get(s), dpm);
                    DepTree tree = pair.get1();
                    // We must negate pair.get2() since we were just maximizing
                    double reducedCost = -pair.get2() - convexPrices[s];
    
                    // TODO: double check that this if-statement is correct
                    //if (reducedCost < 0.0) {
                    if (reducedCost < -5e-8) {
                        // Introduce a new lambda variable
                        if (addLambdaVar(cplex, s, tree)) {
                            numPositiveRedCosts++;
                        } else {
                            log.warn(String.format("Duplicate Lambda Var: redCost=%f s=%d tree=%s ",reducedCost, s,tree.getParents().toString()));
                        }
                    } // else: do nothing
                }
                if (numPositiveRedCosts == 0) {
                    // Optimal solution found
                    break;
                } else {
                    log.debug("Added " + numPositiveRedCosts + " new trees");
                }
            }
            
            // Don't add more cuts after the final solution is found
            if (cut == numCutRounds -1) {
                break;
            }

            // Add a cut for each distribution by projecting the model parameters
            // back onto the simplex.
            double[][] params = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                // Here the params are log probs
                params[c] = cplex.getValues(mp.modelParamVars[c]);
            }
            int numNewStoConstraints = 0;
            for (int c = 0; c < idm.getNumConds(); c++) {
                Vectors.exp(params[c]);
                // Here the params are probs
                if (Vectors.sum(params[c]) > MIN_SUM_FOR_CUT) {
                    numNewStoConstraints++;
                    addSumToOneConstraint(cplex, c, params[c]);
                }
            }
            if (numNewStoConstraints == 0) {
                log.debug("No more cut rounds needed after " + cut + " rounds");
                break;
            } else {
                log.debug("Adding cuts " + numNewStoConstraints + ", round " + cut);
            }
        }

    }

    public void reverseApply(DmvBoundsDelta delta) {
        applyDelta(DmvBoundsDelta.getReverse(delta));
    }

    public void forwardApply(DmvBoundsDelta delta) {
        applyDelta(delta);
    }

    private void applyDelta(DmvBoundsDelta delta) {
        try {
            int c = delta.getC();
            int m = delta.getM();
            
            double origLb = mp.modelParamVars[c][m].getLB();
            double origUb = mp.modelParamVars[c][m].getUB();
            double newLb = origLb;
            double newUb = origUb;
            
            // TODO: all the logAdds should be relegated to the Bounds Delta Factory
            if (delta.getLu() == Lu.LOWER) {
                newLb = origLb + delta.getDelta();
            } else if (delta.getLu() == Lu.UPPER) {
                newUb = origUb + delta.getDelta();
            } else {
                throw new IllegalStateException();
            }

            assert(newLb <= newUb);
            
            // Updates the bounds of the model parameters
            bounds.set(c, m, newLb, newUb);
            mp.modelParamVars[c][m].setLB(newLb);
            mp.modelParamVars[c][m].setUB(newUb);

            // Update lambda column if it uses parameter c,m
            TIntArrayList rowind = new TIntArrayList();
            TIntArrayList colind = new TIntArrayList();
            TDoubleArrayList val = new TDoubleArrayList();
            int lowCmInd = mp.couplMatrix.getIndex(mp.couplConsLower[c][m]);
            int upCmInd = mp.couplMatrix.getIndex(mp.couplConsUpper[c][m]);
            for (LambdaVar lv : mp.lambdaVars) {
                int i = idm.getSi(lv.s, c, m);
                if (i != -1) {
                    // Using cplex.setLinearCoef() is horridly slow. Some suggestions for how to make modification 
                    // of the problem faster here:
                    // https://www.ibm.com/developerworks/forums/thread.jspa?threadID=324926

                    // Update the lower coupling constraint coefficient
                    rowind.add(lowCmInd);
                    colind.add(lv.colind);
                    val.add(bounds.getLb(c, m) * lv.sentSol[i]);
                    // Update the upper coupling constraint coefficient
                    rowind.add(upCmInd);
                    colind.add(lv.colind);
                    val.add(bounds.getUb(c, m) * lv.sentSol[i]);
                }
            }
            if (rowind.size() > 0) {
                mp.couplMatrix.setNZs(rowind.toNativeArray(), colind.toNativeArray(), val.toNativeArray());
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        double score = 0.0;
        for (int s = 0; s < sentences.size(); s++) {
            Sentence sentence = sentences.get(s);
            DepTree tree = treebank.get(s);
            int[] sentSol = idm.getSentSol(sentence, s, tree);
            for (int i=0; i<sentSol.length; i++) {
                int c = idm.getC(s, i);
                int m = idm.getM(s, i);
                if (sentSol[i] != 0) {
                    // This if-statement is to ensure that 0 * -inf == 0.
                    score += sentSol[i] * logProbs[c][m];
                }
                assert (!Double.isNaN(score));
            }
        }
        return score;
    }

    public DmvBounds getBounds() {
        return bounds;
    }

    public IndexedDmvModel getIdm() {
        return idm;
    }
        
    public void end() {
        cplex.end();
    }
    
}

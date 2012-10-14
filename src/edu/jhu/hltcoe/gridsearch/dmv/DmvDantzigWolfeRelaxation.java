package edu.jhu.hltcoe.gridsearch.dmv;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.dmv.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Sets;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

public class DmvDantzigWolfeRelaxation extends DantzigWolfeRelaxation implements DmvRelaxation {

    static Logger log = Logger.getLogger(DmvDantzigWolfeRelaxation.class);
    
    static final double DEFAULT_MIN_SUM_FOR_CUTS = 1.01;

    protected int numLambdas;
    // Stored for re-use by getRegretCm() and getTrueRelaxedObjective()
    protected double[][] optimalLogProbs;
    private double[][] optimalFeatCounts;

    protected DmvTrainCorpus corpus;
    protected IndexedDmvModel idm;
    protected CptBounds bounds;
    protected Stopwatch parsingTimer;
    protected MasterProblem mp;
    
    private int cutCounter;
    private int maxSetSizeToConstrain;
    private double minSumForCuts;
    private CutCountComputer initCutCountComp;
    private DmvObjective dmvObj;

    public DmvDantzigWolfeRelaxation(File tempDir,
            int maxCutRounds, CutCountComputer initCutCountComp) {
        super(tempDir, maxCutRounds);
        this.maxSetSizeToConstrain = 2;
        this.minSumForCuts = DEFAULT_MIN_SUM_FOR_CUTS;
        this.initCutCountComp = initCutCountComp;
        // Counter for printing 
        this.cutCounter = 0;
        this.parsingTimer = new Stopwatch();
    }
    
    public void init1(DmvTrainCorpus corpus) {
        this.corpus = corpus;
        this.idm = new IndexedDmvModel(this.corpus);
        this.dmvObj = new DmvObjective(this.corpus);
    }

    protected void clearRelaxedSolution() {
        optimalLogProbs = null;
        optimalFeatCounts = null;
    }
    
    protected RelaxedSolution extractSolution(RelaxStatus status, double objective) throws UnknownObjectException, IloException {
        log.info(String.format("Summary: #lambdas=%d #cuts=%d", mp.lambdaVars.size(), mp.numStoCons));
        
        // Store optimal model parameters
        optimalLogProbs = new double[idm.getNumConds()][];
        for (int c = 0; c < idm.getNumConds(); c++) {
            optimalLogProbs[c] = cplex.getValues(mp.modelParamVars[c]);
        }

        // Store fractional corpus parse
        double[][] fracRoots = new double[corpus.size()][];
        double[][][] fracParses = new double[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                fracRoots[s] = null;
                fracParses[s] = null;
            } else {
                Sentence sentence = corpus.getSentence(s);
                fracRoots[s] = new double[sentence.size()];
                fracParses[s] = new double[sentence.size()][sentence.size()];
            }
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
        
        return new RelaxedDmvSolution(Utilities.copyOf(optimalLogProbs), fracRoots, fracParses, objective, status);
    }

    public WarmStart getWarmStart() {
        try {
            WarmStart warmStart = new WarmStart();
            warmStart.numVars = mp.lpMatrix.getNumVars();
            warmStart.ranges = mp.lpMatrix.getRanges();
            warmStart.numVarStatuses = cplex.getBasisStatuses(warmStart.numVars);
            warmStart.rangeStatuses = cplex.getBasisStatuses(warmStart.ranges);
            return warmStart;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected ArrayList<IloNumVar> getUnknownVars(HashSet<IloNumVar> knownVars) {
        ArrayList<IloNumVar> unknownVars = new ArrayList<IloNumVar>();
        for (int i=0; i<mp.lambdaVars.size(); i++) {
            IloNumVar lv = mp.lambdaVars.get(i).lambdaVar;
            if (!knownVars.contains(lv)) {
                unknownVars.add(lv);
            }
        }
        return unknownVars;
    }
    
    public double[][] getRegretCm() {
        try {
            // If optimal model parameters \theta_{c,m} are not present, return null.
            if (optimalLogProbs == null) {
                return null;
            }
            // Lazily create the feature counts.
            if (optimalFeatCounts == null) {
                optimalFeatCounts = getFeatureCounts();
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
                    regret[c][m] = objVals[c][m] - (optimalLogProbs[c][m] * optimalFeatCounts[c][m]);
                    //TODO: this seems to be too strong:
                    //assert Utilities.gte(regret[c][m], 0.0, 1e-7) : String.format("regret[%d][%d] = %f", c, m, regret[c][m]);
                    if (!Utilities.gte(regret[c][m], 0.0, 1e-7)) {
                        log.warn(String.format("Invalid negative regret: regret[%d][%d] = %f", c, m, regret[c][m]));
                    }
                }
            }

            return regret;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    public double getTrueObjectiveForRelaxedSolution() {
        try {
            // If optimal model parameters \theta_{c,m} are not present, return null.
            if (optimalLogProbs == null) {
                throw new IllegalStateException();
            }
            // Lazily create the feature counts.
            if (optimalFeatCounts == null) {
                optimalFeatCounts = getFeatureCounts();
            }

            // Compute the true quadratic objective given the model
            // parameters and feature counts found by the relaxation.
            return dmvObj.computeTrueObjective(optimalLogProbs, optimalFeatCounts);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private double[][] getFeatureCounts() throws IloException {
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
        return featCounts;
    }

    /**
     * Convenience class for passing around Master Problem variables
     */
    protected static class MasterProblem {
        public IloObjective objective;
        public IloRange[] lambdaSumCons;
        public List<LambdaVar> lambdaVars;
        public HashSet<LambdaVar> lambdaVarSet;
        public IloNumVar[][] objVars;
        public IloRange[][] couplConsLower;
        public IloRange[][] couplConsUpper;
        public IloLPMatrix lpMatrix;
        
        // Used only by this D-W relaxation.
        public IloNumVar[][] modelParamVars;
        public int numStoCons = 0;
    }
    
    /**
     * Convenience class for storing the columns generated by
     * Dantzig-Wolfe and their corresponding parses.
     */
    protected static class LambdaVar {
        
        public IloNumVar lambdaVar;
        public int s;
        public int[] parents;
        public int[] sentSol;
        public int colind;
        
        public LambdaVar(IloNumVar lambdaVar, int s, int[] parents, int[] sentSol, int colind) {
            super();
            this.lambdaVar = lambdaVar;
            this.s = s;
            this.parents = parents;
            this.sentSol = sentSol;
            // TODO: If we start removing LambdaVars, then these column indices are wrong!!
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
         * 
         * TODO: in the D-W relaxation setting we really only care whether the 
         * sentSolutions are equal since those are the sufficient statistics for
         * the column. However, it's possible we could impose constraints that 
         * would act over the parent array such as dependency length constraints, 
         * so we'll leave this as is for now.
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

    protected void buildModel(IloMPModeler cplex, DmvSolution initFeasSol) throws IloException {
        this.bounds = new CptBounds(this.idm);

        mp = new MasterProblem();
        
        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        int numConds = idm.getNumConds();
        
        // Create the objective
        mp.objective = cplex.addMinimize();
                
        // Create the model parameter variables, adding them to the objective
        mp.modelParamVars = new IloNumVar[numConds][];
        int[][] totFreqCm = idm.getTotSupervisedFreqCm(corpus);
        for (int c = 0; c < numConds; c++) {
            mp.modelParamVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m = 0; m < mp.modelParamVars[c].length; m++) {
                mp.modelParamVars[c][m] = cplex.numVar(bounds.getLb(c, m), bounds.getUb(c, m), idm.getName(c, m));
                if (totFreqCm[c][m] != 0) {
                    // Negate the coefficients since we are minimizing
                    cplex.setLinearCoef(mp.objective, -totFreqCm[c][m], mp.modelParamVars[c][m]);
                }
            }
        }
        
        // Create the objective variables, adding them to the objective
        mp.objVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            mp.objVars[c] = new IloNumVar[numParams];
            for (int m=0; m<numParams; m++) {
                mp.objVars[c][m] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, String.format("z_{%d,%d}",c,m));
                // Negate the coefficients since we are minimizing
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
        mp.lpMatrix = cplex.addLPMatrix("couplingMatrix");
        for (int c = 0; c < numConds; c++) {
            mp.lpMatrix.addRows(mp.couplConsLower[c]);
        }
        for (int c = 0; c < numConds; c++) {
            mp.lpMatrix.addRows(mp.couplConsUpper[c]);
        }

        // ----- column-wise modeling -----

        // Create the lambda sum to one constraints
        mp.lambdaSumCons = new IloRange[corpus.getNumUnlabeled()];
        mp.lambdaVars = new ArrayList<LambdaVar>();
        mp.lambdaVarSet = new HashSet<LambdaVar>();

        // Add the initial feasible parse as the first lambda columns
        addFeasibleSolution(initFeasSol);
        
        // TODO: Use the initial solution as the points here.
        // Create the cut vectors for sum-to-one constraints
        double[][][] pointsArray = getInitialPoints();
        // Add the initial cuts
        for (int c = 0; c < numConds; c++) {
            for (int i = 0; i < pointsArray[c].length; i++) {
                double[] probs = pointsArray[c][i];
                addSumToOneConstraint(c, probs);
            }
        }
        
        for (int setSize=2; setSize <= maxSetSizeToConstrain; setSize++) {
            for (int c = 0; c < numConds; c++) {
                addSetContraints(setSize, c);
            }
        }
    }

    @Override
    public void addFeasibleSolution(DmvSolution feasSol) {
        try {
            for (int s = 0; s < corpus.size(); s++) {
                if (!corpus.isLabeled(s)) {
                    DepTree tree = feasSol.getTreebank().get(s);
                    addLambdaVar(s, tree);
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
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

    private void addSumToOneConstraint(int c, double[] point) throws IloException {
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
        IloRange constraint = cplex.le(vectorExpr, vectorSum, String.format("maxVar(%d)-%d", c, cutCounter++));
        // TODO: double check that this doesn't slow us down (by growing the LP matrix)
        mp.lpMatrix.addRow(constraint);
        mp.numStoCons++;
    }
    
    private void addSetContraints(int setSize, int c) throws IloException {

        double[] ones = new double[setSize];
        Arrays.fill(ones, 1.0);
        
        int numParams = idm.getNumParams(c);
        int counter = 0;
        
        // Make sure setSize isn't larger than numParams
        setSize = Math.min(setSize,numParams);
        
        for (int[] paramIndices : Sets.getSets(setSize, numParams)) {
            assert(paramIndices.length == setSize);
            IloNumVar[] paramVars = new IloNumVar[setSize]; 
            for (int i=0; i<setSize; i++) {
                int m = paramIndices[i];
                paramVars[i] = mp.modelParamVars[c][m];
            }

            IloLinearNumExpr vectorExpr = cplex.scalProd(ones, paramVars);
            IloRange constraint = cplex.le(vectorExpr, 1.0, String.format("setCons(%d)-%d", c, counter++));
            mp.lpMatrix.addRow(constraint);
        }
    }

    protected boolean addLambdaVar(int s, DepTree tree) throws IloException {

        LambdaVar lvTemp = new LambdaVar(null, s, tree.getParents(), null, -1);
        if (mp.lambdaVarSet.contains(lvTemp)) {
            //int lvIdx = mp.lambdaVars.lastIndexOf(lvTemp);
            //log.error("Duplicate Lambda Var: " + lvTemp);
            //throw new IllegalStateException("Duplicate LambdaVar already in the master problem");
            //log.warn("Duplicate LambdaVar already in the master problem");
            
            // Don't add the duplicate, since this probably just means its reduced cost is really close to zero
            return false;
        }
        
        int[] sentSol = idm.getSentSol(corpus.getSentence(s), s, tree);
        int numSentVars = idm.getNumSentVars(s);
        
        IloNumVar lambdaVar = cplex.numVar(0.0, Double.MAX_VALUE, String.format("lambda_{%d}^{%d}", s, numLambdas++));
        
        // Add the lambda var to the relaxed-objective-coupling-constraints matrix
        int[] ind = new int[numSentVars*2];
        double[] val = new double[numSentVars*2];
        int j=0;
        for (int i = 0; i < numSentVars; i++) {
            int c = idm.getC(s, i);
            int m = idm.getM(s, i);
            
            // Add to the lower coupling constraint
            ind[j] = mp.lpMatrix.getIndex(mp.couplConsLower[c][m]);
            val[j] = bounds.getLb(c, m) * sentSol[i];
            j++;
            
            // Add to the upper coupling constraint
            ind[j] = mp.lpMatrix.getIndex(mp.couplConsUpper[c][m]);
            val[j] = bounds.getUb(c, m) * sentSol[i];
            j++;
        }
        int colind = mp.lpMatrix.addColumn(lambdaVar, ind, val);
        

        // Add the lambda var to its sum to one constraint
        if (mp.lambdaSumCons[s] == null) {
            mp.lambdaSumCons[s] = cplex.eq(lambdaVar, 1.0, "lambdaSum");
            mp.lpMatrix.addRow(mp.lambdaSumCons[s]);
        } else {
            int rowind = mp.lpMatrix.getIndex(mp.lambdaSumCons[s]);
            mp.lpMatrix.setNZ(rowind, colind, 1.0);
        }
        
        LambdaVar lv = new LambdaVar(lambdaVar, s, tree.getParents(), sentSol, colind);
        
        mp.lambdaVars.add(lv);
        mp.lambdaVarSet.add(lv);
        
        return true;
    }
    
    protected void printSummary() {
        log.debug("Avg parsing time(ms) per solve: " + Time.totMs(parsingTimer) / getNumSolves());
    }

    protected boolean isFeasible() {
        return areFeasibleBounds(bounds);
    }
    
    protected SubproblemRetVal addColumns(IloCplex cplex) throws UnknownObjectException, IloException {
        // Get the simplex multipliers (shadow prices).
        // These are shared across all slaves, since each slave
        // has the same D_s matrix.
        double[] pricesLower = cplex.getDuals(mp.lpMatrix, 0, idm.getNumTotalParams());
        double[] pricesUpper = cplex.getDuals(mp.lpMatrix, idm.getNumTotalParams(), idm.getNumTotalParams());
        
        // Compute the parse weights, which will be shared across all subproblems
        int numConds = idm.getNumConds();
        double[][] parseWeights = new double[numConds][];
        int j = 0;
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            parseWeights[c] = new double[numParams];

            for (int m = 0; m < numParams; m++) {
                // Calculate new model parameter values for parser
                // based on the relaxed-objective-coupling-constraints
                parseWeights[c][m] = (pricesLower[j] * bounds.getLb(c, m) + pricesUpper[j] * bounds.getUb(c, m));
                j++;
                // We want to minimize the following:
                // c^T - q^T D_s = - (pricesLower[m]*bounds.getLb(c,m) + pricesUpper[m]*bounds.getUb(c,m))
                // but we negate this since the parser will try to maximize. In other words:
                // minimize -q^T D_s = maximize q^T D_s
                // 
            }
        }
        DmvModel dmv = idm.getDmvModel(parseWeights);

        // Get the simplex multipliers (shadow prices) for the lambda
        // sentence constraints
        double[] convexLambdaPrices = cplex.getDuals(mp.lambdaSumCons);
        
        // Keep track of minimum subproblem reduced cost
        double sumReducedCost = 0.0;
        
        // Solve each parsing subproblem
        DmvCkyParser parser = new DmvCkyParser();

        int numPositiveLambdaRedCosts = 0;
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                continue;
            }
            parsingTimer.start();
            Pair<DepTree, Double> pPair = parser.parse(corpus.getSentence(s), dmv);
            parsingTimer.stop();
            DepTree tree = pPair.get1();
            // We must negate pair.get2() since we were just maximizing
            double pReducedCost = -pPair.get2() - convexLambdaPrices[s];

            if (pReducedCost < NEGATIVE_REDUCED_COST_TOLERANCE) {
                // Introduce a new lambda variable
                if (addLambdaVar(s, tree)) {
                    numPositiveLambdaRedCosts++;
                } else {
                    log.warn(String.format("Duplicate Lambda Var: redCost=%f s=%d tree=%s ", pReducedCost, s, tree
                            .getParents().toString()));
                }
            } // else: do nothing
            
            sumReducedCost += pReducedCost;
        }
        if (numPositiveLambdaRedCosts > 0 && sumReducedCost > 1e-6) {
            log.warn("The sum of the reduced costs should be negative: sumPReducedCost = " + sumReducedCost);
        }
        if (numPositiveLambdaRedCosts > 0) {
            log.debug("Added " + numPositiveLambdaRedCosts + " new trees");
        }
        int numPositiveRedCosts = numPositiveLambdaRedCosts;

        return new SubproblemRetVal(sumReducedCost, numPositiveRedCosts, false);
    }

    protected int addCuts(IloCplex cplex, TDoubleArrayList iterationObjVals,
            ArrayList<Status> iterationStatus, int cut) throws UnknownObjectException, IloException {
        // Reset the objective values list, since we would expect the next iteration
        // to increase, not decrease, after adding the cut below.
        log.debug(String.format("Iteration objective values (cut=%d): %s", cut, iterationObjVals));
        iterationObjVals.clear();
        iterationStatus.clear();

        // Add a cut for each distribution by projecting the model
        // parameters
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
            if (Vectors.sum(params[c]) > minSumForCuts) {
                numNewStoConstraints++;
                addSumToOneConstraint(c, params[c]);
            }
        }
        int numCutAdded = numNewStoConstraints;
        return numCutAdded;
    }

    public void reverseApply(CptBoundsDelta delta) {
        applyDelta(CptBoundsDelta.getReverse(delta));
    }

    public void forwardApply(CptBoundsDelta delta) {
        applyDelta(delta);
    }

    protected void applyDelta(CptBoundsDelta delta) {
        try {
            int c = delta.getC();
            int m = delta.getM();
            
            double origLb = bounds.getLb(c, m);
            double origUb = bounds.getUb(c, m);
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

            assert newLb <= newUb : String.format("l,u = %f, %f", newLb, newUb);
            
            // Updates the bounds of the model parameters
            bounds.set(c, m, newLb, newUb);
            mp.modelParamVars[c][m].setLB(newLb);
            mp.modelParamVars[c][m].setUB(newUb);

            // Update lambda column if it uses parameter c,m
            TIntArrayList rowind = new TIntArrayList();
            TIntArrayList colind = new TIntArrayList();
            TDoubleArrayList val = new TDoubleArrayList();
            int lowCmInd = mp.lpMatrix.getIndex(mp.couplConsLower[c][m]);
            int upCmInd = mp.lpMatrix.getIndex(mp.couplConsUpper[c][m]);
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
                mp.lpMatrix.setNZs(rowind.toNativeArray(), colind.toNativeArray(), val.toNativeArray());
            }
            
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected boolean areFeasibleBounds(CptBounds bounds) {
        // Check that the upper bounds sum to at least 1.0
        for (int c=0; c<idm.getNumConds(); c++) {
            double logSum = Double.NEGATIVE_INFINITY;
            int numParams = idm.getNumParams(c);
            // Sum the upper bounds
            for (int m = 0; m < numParams; m++) {
                logSum = Utilities.logAdd(logSum, bounds.getUb(c, m));
            }
            
            if (logSum < -1e-10) {
                // The problem is infeasible
                return false;
            }
        }
        
        // Check that the lower bounds sum to no more than 1.0
        for (int c=0; c<idm.getNumConds(); c++) {
            double logSum = Double.NEGATIVE_INFINITY;
            int numParams = idm.getNumParams(c);
            // Sum the lower bounds
            for (int m = 0; m < numParams; m++) {
                logSum = Utilities.logAdd(logSum, bounds.getLb(c, m));
            }
            
            if (logSum > 1e-10) {
                // The problem is infeasible
                return false;
            }
        }
        return true;
    }
    
    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        return dmvObj.computeTrueObjective(logProbs, treebank);
    }

    public CptBounds getBounds() {
        return bounds;
    }

    public IndexedDmvModel getIdm() {
        return idm;
    }

    public void setMaxSetSizeToConstrain(int maxSetSizeToConstrain) {
        this.maxSetSizeToConstrain = maxSetSizeToConstrain;
    }

    public void setMinSumForCuts(double minSumForCuts) {
        this.minSumForCuts = minSumForCuts;
    }
    
}

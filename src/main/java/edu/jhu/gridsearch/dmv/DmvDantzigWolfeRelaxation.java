package edu.jhu.gridsearch.dmv;

import edu.jhu.util.collections.PDoubleArrayList;
import edu.jhu.util.collections.PIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
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

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.WallDepTreeNode;
import edu.jhu.gridsearch.DantzigWolfeRelaxation;
import edu.jhu.gridsearch.ProblemNode;
import edu.jhu.gridsearch.RelaxStatus;
import edu.jhu.gridsearch.RelaxedSolution;
import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.gridsearch.cpt.LpSumToOneBuilder;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.gridsearch.cpt.LpSumToOneBuilder.LpStoBuilderPrm;
import edu.jhu.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.util.Pair;
import edu.jhu.util.Timer;
import edu.jhu.util.Utilities;
import edu.jhu.util.cplex.CplexPrm;

public class DmvDantzigWolfeRelaxation extends DantzigWolfeRelaxation implements DmvRelaxation {

    public static interface DmvRelaxationFactory {
        public DmvRelaxation getInstance(DmvTrainCorpus corpus, DmvSolution initFeasSol);
    }
    
    public static class DmvDwRelaxPrm extends DwRelaxPrm implements DmvRelaxationFactory {
        public CplexPrm cplexPrm = new CplexPrm();
        public LpStoBuilderPrm stoPrm = new LpStoBuilderPrm();
        public DmvObjectivePrm objPrm = new DmvObjectivePrm();
        public DmvDwRelaxPrm() {
            super();
        }
        public DmvDwRelaxPrm(File tempDir, int maxCutRounds, CutCountComputer ccc) {
            this();
            this.tempDir = tempDir;
            this.maxCutRounds = maxCutRounds;
            this.rootMaxCutRounds = maxCutRounds;
            this.stoPrm.initCutCountComp = ccc;
        }
        @Override
        public DmvRelaxation getInstance(DmvTrainCorpus corpus, DmvSolution initFeasSol) {
            DmvDantzigWolfeRelaxation relax = new DmvDantzigWolfeRelaxation(this);
            relax.init1(corpus);
            relax.init2(initFeasSol);
            return relax;
        }
    }
    
    static Logger log = Logger.getLogger(DmvDantzigWolfeRelaxation.class);
    
    protected int numLambdas;
    protected DmvTrainCorpus corpus;
    protected IndexedDmvModel idm;
    protected CptBounds bounds;
    protected Timer parsingTimer;
    protected MasterProblem mp;
    protected LpSumToOneBuilder sto;
    
    private DmvObjective dmvObj;
    private DmvProblemNode activeNode;
    
    private DmvDwRelaxPrm prm;
    
    public DmvDantzigWolfeRelaxation(DmvDwRelaxPrm prm) {
        super(prm);
        this.prm = prm;
        this.sto = new LpSumToOneBuilder(prm.stoPrm);
        this.parsingTimer = new Timer();
    }
    
    public void init1(DmvTrainCorpus corpus) {
        this.corpus = corpus;
        this.idm = new IndexedDmvModel(this.corpus);
        this.dmvObj = new DmvObjective(prm.objPrm, idm);
    }

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
    
    @Override
    public DmvProblemNode getActiveNode() {
        return activeNode;
    }
    
    protected RelaxedSolution extractSolution(RelaxStatus status, double objective) throws UnknownObjectException, IloException {
        // Store optimal model parameters
        double[][] optimalLogProbs = extractRelaxedLogProbs();
        
        // Store optimal feature counts
        double[][] optimalFeatCounts = getFeatureCounts();

        // Store objective values z_{c,m}
        double[][] objVals = new double[idm.getNumConds()][];
        for (int c = 0; c < idm.getNumConds(); c++) {
            objVals[c] = cplex.getValues(mp.objVars[c]);
        }

        // Add in the counts from supervision. This program, unlike the RLT
        // relaxation, accounts for the supervised feature counts as a separate
        // portion of the objective.
        int[][] totSupFreqCm = idm.getTotSupervisedFreqCm();
        for (int c = 0; c < idm.getNumConds(); c++) {
            for (int m=0; m<idm.getNumParams(c); m++) {
                optimalFeatCounts[c][m] += totSupFreqCm[c][m];
                objVals[c][m] += optimalLogProbs[c][m] * (double)totSupFreqCm[c][m];
            }
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

    protected double[][] extractRelaxedLogProbs() throws UnknownObjectException, IloException {
        return sto.extractRelaxedLogProbs();
    }

    protected RelaxedDepTreebank extractRelaxedParse() throws UnknownObjectException, IloException {
        RelaxedDepTreebank relaxTreebank = new RelaxedDepTreebank(corpus);
        for (LambdaVar triple : mp.lambdaVars) {
            double frac = cplex.getValue(triple.lambdaVar);
            int s = triple.s;
            int[] parents = triple.parents;

            double[] fracRoot = relaxTreebank.getFracRoots()[s];
            double[][] fracParse = relaxTreebank.getFracChildren()[s];
            for (int child = 0; child < parents.length; child++) {
                int parent = parents[child];
                if (parent == WallDepTreeNode.WALL_POSITION) {
                    fracRoot[child] += frac;
                } else {
                    fracParse[parent][child] += frac;
                }
            }
        }
        return relaxTreebank;
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

    protected void buildModel(IloCplex cplex, DmvSolution initFeasSol) throws IloException {
        this.bounds = new CptBounds(this.idm);

        mp = new MasterProblem();
        
        // Add the LP matrix that will contain all the constraints.
        mp.lpMatrix = cplex.addLPMatrix("couplingMatrix");
        
        // Initialize the model parameter variables and constraints.
        sto.init(cplex, mp.lpMatrix, idm, bounds);
        
        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        int numConds = idm.getNumConds();
        
        // Create the objective
        mp.objective = cplex.addMinimize();
                        
        sto.createModelParamVars();
        
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
                double maxFreqCm = idm.getTotUnsupervisedMaxFreqCm(c,m);
                IloNumExpr rhsLower = cplex.sum(slackVarLower,
                                        cplex.diff(cplex.prod(maxFreqCm, sto.modelParamVars[c][m]), mp.objVars[c][m]));
                mp.couplConsLower[c][m] = cplex.eq(maxFreqCm * bounds.getLb(Type.PARAM,c, m), rhsLower, name);
                
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
        
        // TODO: For some strange reason...moving this method earlier breaks lots of unit tests.
        sto.addModelParamConstraints();
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
            val[j] = bounds.getLb(Type.PARAM, c, m) * sentSol[i];
            j++;
            
            // Add to the upper coupling constraint
            ind[j] = mp.lpMatrix.getIndex(mp.couplConsUpper[c][m]);
            val[j] = bounds.getUb(Type.PARAM, c, m) * sentSol[i];
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
        log.debug("Avg parsing time(ms) per solve: " + parsingTimer.totMs() / getNumSolves());
        log.info(String.format("Summary: #lambdas=%d #cuts=%d", mp.lambdaVars.size(), sto.getNumStoCons()));
    }

    protected boolean isFeasible() {
        return bounds.areFeasibleBounds();
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
                parseWeights[c][m] = (pricesLower[j] * bounds.getLb(Type.PARAM, c, m) + pricesUpper[j] * bounds.getUb(Type.PARAM, c, m));
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

            if (pReducedCost < prm.NEGATIVE_REDUCED_COST_TOLERANCE) {
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

    protected int addCuts(IloCplex cplex, PDoubleArrayList iterationObjVals,
            ArrayList<Status> iterationStatus, int cut) throws UnknownObjectException, IloException {
        // Reset the objective values list, since we would expect the next iteration
        // to increase, not decrease, after adding the cut below.
        log.debug(String.format("Iteration objective values (cut=%d): %s", cut, iterationObjVals));
        iterationObjVals.clear();
        iterationStatus.clear();

        return sto.projectModelParamsAndAddCuts().size();
    }

    public void reverseApply(CptBoundsDeltaList deltas) {
        applyDeltaList(CptBoundsDeltaList.getReverse(deltas));
    }

    public void forwardApply(CptBoundsDeltaList deltas) {
        applyDeltaList(deltas);
    }

    protected void applyDeltaList(CptBoundsDeltaList deltas) {
        for (CptBoundsDelta delta : deltas) {
            applyDelta(delta);
        }
    }
    
    protected void applyDelta(CptBoundsDelta delta) {
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

                // Update lambda column if it uses parameter c,m
                PIntArrayList rowind = new PIntArrayList();
                PIntArrayList colind = new PIntArrayList();
                PDoubleArrayList val = new PDoubleArrayList();
                int lowCmInd = mp.lpMatrix.getIndex(mp.couplConsLower[c][m]);
                int upCmInd = mp.lpMatrix.getIndex(mp.couplConsUpper[c][m]);
                for (LambdaVar lv : mp.lambdaVars) {
                    int i = idm.getSi(lv.s, c, m);
                    if (i != -1) {
                        // Using cplex.setLinearCoef() is horridly slow. Some
                        // suggestions for how to make modification
                        // of the problem faster here:
                        // https://www.ibm.com/developerworks/forums/thread.jspa?threadID=324926

                        // Update the lower coupling constraint coefficient
                        rowind.add(lowCmInd);
                        colind.add(lv.colind);
                        val.add(bounds.getLb(Type.PARAM, c, m) * lv.sentSol[i]);
                        // Update the upper coupling constraint coefficient
                        rowind.add(upCmInd);
                        colind.add(lv.colind);
                        val.add(bounds.getUb(Type.PARAM, c, m) * lv.sentSol[i]);
                    }
                }
                if (rowind.size() > 0) {
                    mp.lpMatrix.setNZs(rowind.toNativeArray(), colind.toNativeArray(), val.toNativeArray());
                }
            } else {
                //TODO: Implement this
                throw new RuntimeException("not implemented");
            }

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public void updateTimeRemaining(double timeoutSeconds) {
        CplexPrm.updateTimeoutSeconds(cplex, timeoutSeconds);
    }
    
}

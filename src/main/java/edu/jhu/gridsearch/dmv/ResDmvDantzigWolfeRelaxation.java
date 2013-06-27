package edu.jhu.gridsearch.dmv;

import edu.jhu.util.collections.PDoubleArrayList;
import edu.jhu.util.collections.PIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BasisStatus;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.gridsearch.cpt.Projections;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.gridsearch.cpt.Projections.ProjectionsPrm;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.util.Pair;
import edu.jhu.util.Timer;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;

/**
 * Alternative to DmvDantzigWolfeRelaxation which doesn't use any cuts. This is accomplished by 
 * pushing the sum-to-one constraints into a subproblem which enforces that they sum to <= 1.0. 
 * Doing this requires an application of the resolution theorem, where instead of having a
 * finite set of integer points, we have an infinite set of points on a convex region in 
 * log-probability space. 
 * 
 * @author mgormley
 */
public class ResDmvDantzigWolfeRelaxation extends DmvDantzigWolfeRelaxation implements DmvRelaxation {

    public static class ResDmvDwRelaxPrm extends DmvDwRelaxPrm implements DmvRelaxationFactory {
        public ProjectionsPrm projPrm = new ProjectionsPrm();
        public ResDmvDwRelaxPrm() {
            super();
        }
        @Override
        public DmvRelaxation getInstance(DmvTrainCorpus corpus, DmvSolution initFeasSol) {
            ResDmvDantzigWolfeRelaxation relax = new ResDmvDantzigWolfeRelaxation(this);
            relax.init1(corpus);
            relax.init2(initFeasSol);
            return relax;
        }
    }
    
    private static final Logger log = Logger.getLogger(ResDmvDantzigWolfeRelaxation.class);
    
    private int numGammas;
    private boolean hasInfeasibleBounds;
    private Timer stoTimer;
    private Projections projections;
    private int[][] supervisedFreqCm;
    private MasterProblemRes mpr;
    
    private ResDmvDwRelaxPrm prm;
    
    public ResDmvDantzigWolfeRelaxation(ResDmvDwRelaxPrm prm) {
        super(prm);
        this.sto = null;
        this.prm = prm;
        this.projections = new Projections(prm.projPrm);
        this.hasInfeasibleBounds = false;
        this.parsingTimer = new Timer();
        this.stoTimer = new Timer();
    }

    public void init1(DmvTrainCorpus corpus) {
        super.init1(corpus);
        supervisedFreqCm = idm.getTotSupervisedFreqCm(corpus);
    }

    protected double[][] extractRelaxedLogProbs() throws UnknownObjectException, IloException {
        double[][] optimalLogProbs = new double[idm.getNumConds()][];
        for (int i = 0; i < mpr.gammaVars.size(); i++) {
            GammaVar gv = mpr.gammaVars.get(i);
            double gammaValue = cplex.getValue(gv.gammaVar);
            for (int c = 0; c < idm.getNumConds(); c++) {
                int numParams = idm.getNumParams(c);
                if (optimalLogProbs[c] == null) {
                    optimalLogProbs[c] = new double[numParams];
                }
                for (int m = 0; m < numParams; m++) {
                    optimalLogProbs[c][m] += gammaValue * gv.logProbs[c][m];
                }
            }
        }
        // Assert that the model parameters sum to <= 1.0
        for (int c = 0; c < idm.getNumConds(); c++) {
            double[] probs = Vectors.getExp(optimalLogProbs[c]);
            //assert Utilities.lte(Vectors.sum(probs), 1.0, 1e-8) : String.format("sum(probs[%d]) = %.15g", c, Vectors.sum(probs));
            if (!Utilities.lte(Vectors.sum(probs), 1.0, 1e-8)) {
                log.warn(String.format("Sum of log probs must be <= 1.0: sum(probs[%d]) = %.15g", c, Vectors.sum(probs)));
            }
        }
        return optimalLogProbs;
    }

    protected ArrayList<IloNumVar> getUnknownVars(HashSet<IloNumVar> knownVars) {
        ArrayList<IloNumVar> unknownVars = new ArrayList<IloNumVar>();
        for (int i=0; i<mp.lambdaVars.size(); i++) {
            IloNumVar lv = mp.lambdaVars.get(i).lambdaVar;
            if (!knownVars.contains(lv)) {
                unknownVars.add(lv);
            }
        }
        for (int i=0; i<mpr.gammaVars.size(); i++) {
            IloNumVar gv = mpr.gammaVars.get(i).gammaVar;
            if (!knownVars.contains(gv)) {
                unknownVars.add(gv);
            }
        }
        return unknownVars;
    }
    
    /**
     * Convenience class for passing around Master Problem variables
     */
    protected static class MasterProblemRes extends MasterProblem {
        public IloRange gammaSumCons;
        public List<GammaVar> gammaVars;
    }
    
    /**
     * Convenience class for storing the columns generated by
     * Dantzig-Wolfe and their corresponding model parameters
     */
    private static class GammaVar {
        
        public IloNumVar gammaVar;
        public double[][] logProbs;

        public GammaVar(IloNumVar gammaVar, double[][] logProbs) {
            super();
            this.gammaVar = gammaVar;
            this.logProbs = logProbs;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("logProbs:\n");
            for (int c=0; c<logProbs.length; c++){
                sb.append(Arrays.toString(logProbs[c]));
                sb.append("\n");
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GammaVar other = (GammaVar) obj;
            if (logProbs.length != other.logProbs.length) {
                return false;
            }
            for (int c=0; c<logProbs.length; c++) {
                if (logProbs[c].length != other.logProbs[c].length) {
                    return false;
                }
                for (int m=0; m<logProbs[c].length; m++) {
                    if (!Utilities.equals(logProbs[c][m], other.logProbs[c][m], 1e-13)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
    }
    
    private static class GammaVarHashObj00 extends GammaVar {
        
        protected double[][] logProbs;
        
        public GammaVarHashObj00(double[][] logProbs) {
            super(null, logProbs);
            this.logProbs = logProbs;            
        }
        
        @Override
        public int hashCode() {
            return roundingHashCode(logProbs, 0.0);
        }
        
        public static int roundingHashCode(double[][] a, double addend) {
            if (a == null)
                return 0;

            int result = 1;
            for (double[] element : a) {
                result = 31 * result + roundingHashCode(element, addend);
            }
            return result;
        }
        
        /**
         * Rounds any double to a long before taking its hash.
         * Before rounding, one can add addend
         */
        public static int roundingHashCode(double a[], double addend) {
            if (a == null)
                return 0;

            int result = 1;
            for (double element : a) {
                long bits = Math.round(element + addend);
                result = 31 * result + (int) (bits ^ (bits >>> 32));
            }
            return result;
        }
        
    }
    

    private static class GammaVarHashObj05 extends GammaVarHashObj00 {

        public GammaVarHashObj05(double[][] logProbs) {
            super(logProbs);
        }

        @Override
        public int hashCode() {
            return roundingHashCode(logProbs, 0.5);
        }
        
    }
        
    
    protected void buildModel(IloCplex cplex, DmvSolution initFeasSol) throws IloException {
        this.bounds = new CptBounds(this.idm);

        mpr = new MasterProblemRes();
        mp = mpr;
        
        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        int numConds = idm.getNumConds();

        // Create the objective
        mp.objective = cplex.addMinimize();
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
                IloNumExpr rhsLower = cplex.diff(slackVarLower, mp.objVars[c][m]);
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

        addFeasibleSolution(initFeasSol);
    }

    @Override
    public void addFeasibleSolution(DmvSolution feasSol) {
        try {
            int numConds = idm.getNumConds();

            // Add the initial feasible parse as the first lambda columns
            for (int s = 0; s < corpus.size(); s++) {
                if (!corpus.isLabeled(s)) {
                    DepTree tree = feasSol.getTreebank().get(s);
                    addLambdaVar(s, tree);
                }
            }

            // Create the gamma sum to one constraints
            mpr.gammaVars = new ArrayList<GammaVar>();
            // Add the initial feasible solution as the first gamma column
            double[][] initLogProbs = feasSol.getLogProbs();
            for (int c = 0; c < numConds; c++) {
                // Project the initial solution onto the feasible region
                double[] params = Vectors.getExp(initLogProbs[c]);
                params = projections.getBoundedProjection(bounds, c, params);
                if (params == null) {
                    throw new IllegalStateException("The initial bounds are infeasible");
                }
                initLogProbs[c] = Vectors.getLog(params);
            }
            addGammaVar(initLogProbs);

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean addGammaVar(double[][] logProbs) throws IloException {
        GammaVar gvTemp = new GammaVar(null, logProbs);
        if (mpr.gammaVars.contains(gvTemp)) {
            // Don't add the duplicate, since this probably just means its reduced cost is really close to zero
            return false;
        }
        
        IloNumVar gammaVar = cplex.numVar(0.0, Double.MAX_VALUE, String.format("gamma^{%d}", numGammas++));
        
        // Add the gamma var to the relaxed-objective-coupling-constraints
        // matrix
        int[] ind = new int[idm.getNumNonZeroUnsupMaxFreqCms()];
        double[] val = new double[idm.getNumNonZeroUnsupMaxFreqCms()];
        int j = 0;
        for (int c = 0; c < idm.getNumConds(); c++) {
            for (int m = 0; m < idm.getNumParams(c); m++) {
                // Only update non zero rows
                int totMaxFreqCm = idm.getTotUnsupervisedMaxFreqCm(c, m);
                if (totMaxFreqCm > 0) {
                    // Add to the lower coupling constraint
                    ind[j] = mp.lpMatrix.getIndex(mp.couplConsLower[c][m]);
                    val[j] = totMaxFreqCm * logProbs[c][m];
                    j++;
                }
            }
        }
        int colind = mp.lpMatrix.addColumn(gammaVar, ind, val);

        double objCoefficient = 0.0;
        for (int c = 0; c < idm.getNumConds(); c++) {
            for (int m = 0; m < idm.getNumParams(c); m++) {
                // Negate the coefficients since we are minimizing.
                objCoefficient += -supervisedFreqCm[c][m] * logProbs[c][m];
            }
        }
        cplex.setLinearCoef(mp.objective, objCoefficient, gammaVar);

        // Add the gamma var to its sum to one constraint
        if (mpr.gammaSumCons == null) {
            mpr.gammaSumCons = cplex.eq(gammaVar, 1.0, "gammaSum");
            mp.lpMatrix.addRow(mpr.gammaSumCons);
        } else {
            int rowind = mp.lpMatrix.getIndex(mpr.gammaSumCons);
            mp.lpMatrix.setNZ(rowind, colind, 1.0);
        }
        
        GammaVar gv = new GammaVar(gammaVar, logProbs);
        mpr.gammaVars.add(gv);
        return true;
    }

    private void removeGammaVar(GammaVar gv)  throws IloException {

        //TODO: remove these printouts
        if (prm.tempDir != null) {
            // TODO: remove this or add a debug flag to the if
            cplex.exportModel(new File(prm.tempDir, "dw.beforeremoval.lp").getAbsolutePath());
        }        
        
        //TODO: this might be wrong
        mp.lpMatrix.removeColumn(mp.lpMatrix.getIndex(gv.gammaVar));
       
        if (prm.tempDir != null) {
            // TODO: remove this or add a debug flag to the if
            cplex.exportModel(new File(prm.tempDir, "dw.afterremoval.lp").getAbsolutePath());
        }
        mpr.gammaVars.remove(gv);

    }
    

    protected void printSummary() {
        log.debug("Avg parsing time(ms) per solve: " + parsingTimer.totMs() / getNumSolves());
        log.debug("Avg sum-to-one time(ms) per solve: " + stoTimer.totMs() / getNumSolves());
        log.info(String.format("Summary: #lambdas=%d #gammas=%d", mp.lambdaVars.size(), mpr.gammaVars.size()));
    }

    protected boolean isFeasible() {
        return !hasInfeasibleBounds && bounds.areFeasibleBounds();
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
           
        // Compute the model parameter weights, used by the model parameters subproblem
        double[][] modelWeights = new double[numConds][];
        j = 0;
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            modelWeights[c] = new double[numParams];
            for (int m = 0; m < numParams; m++) {
                modelWeights[c][m] = -supervisedFreqCm[c][m] - pricesLower[j] * idm.getTotUnsupervisedMaxFreqCm(c, m);
                j++;
            }
        }
                 
        // Get the simplex multipliers (shadow prices) for the gamma 
        double convexGammaPrice = cplex.getDual(mpr.gammaSumCons);
        
        // Keep track of minimum subproblem reduced cost
        double sumReducedCost = 0.0;
        
        // Solve the model parameters subproblem
        int numPositiveGammaRedCosts = 0;
        ModelParamSubproblem mps = new ModelParamSubproblem();
        stoTimer.start();
        Pair<double[][], Double> mPair = mps.solveModelParamSubproblemJOptimizeLogProb(modelWeights, bounds);
        stoTimer.stop();
        if (mPair == null) {
            hasInfeasibleBounds = true;
            return new SubproblemRetVal(0.0, 0, true);
        }
        double[][] logProbs = mPair.get1();
        double mReducedCost = mPair.get2() - convexGammaPrice;
        if (log.isDebugEnabled()) {
            int index = mpr.gammaVars.indexOf(new GammaVar(null, logProbs));
            if (index != -1) {
                GammaVar gv = mpr.gammaVars.get(index);
                log.debug(String.format("CPLEX redcost=%.13f, My redcost=%.13f", cplex.getReducedCost(gv.gammaVar), mReducedCost));
                assert(Utilities.equals(cplex.getReducedCost(gv.gammaVar), mReducedCost, 1e-8));
            }
        }  
        if (mReducedCost < prm.NEGATIVE_REDUCED_COST_TOLERANCE) {
            // Introduce a new gamma variable
            if (addGammaVar(logProbs)) {
                numPositiveGammaRedCosts++;

                if (mpr.gammaVars.size() > mp.lpMatrix.getNrows()) {
                    // Remove the non-basic gamma variables 
                    // TODO: remove the gamma variables that price out the highest
                    for (int i=0; i<mpr.gammaVars.size(); i++) {
                        GammaVar gv = mpr.gammaVars.get(i);
                        BasisStatus bstatus = cplex.getBasisStatus(gv.gammaVar);
                        if (bstatus != BasisStatus.Basic) {
                            removeGammaVar(gv);
                        }
                    }
                    assert(mpr.gammaVars.size() > 0);
                }
            }
        }
        sumReducedCost += mReducedCost;
        
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
        if (numPositiveLambdaRedCosts > 0 || numPositiveGammaRedCosts > 0) {
            log.debug(String.format("Added %d new trees and %d new gammas", numPositiveLambdaRedCosts, numPositiveGammaRedCosts));
        }
        int numPositiveRedCosts = numPositiveLambdaRedCosts + numPositiveGammaRedCosts;
        
        return new SubproblemRetVal(sumReducedCost, numPositiveRedCosts, false);
    }

    protected int addCuts(IloCplex cplex, PDoubleArrayList iterationObjVals,
            ArrayList<Status> iterationStatus, int cut) throws UnknownObjectException, IloException {
        return 0;
    }

    @Override
    protected void applyDelta(CptBoundsDelta delta) {
        try {
            int c = delta.getC();
            int m = delta.getM();
            
            double origLb = bounds.getLb(Type.PARAM, c, m);
            double origUb = bounds.getUb(Type.PARAM, c, m);
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
            bounds.set(Type.PARAM, c, m, newLb, newUb);

            // Update lambda column if it uses parameter c,m
            PIntArrayList rowind = new PIntArrayList();
            PIntArrayList colind = new PIntArrayList();
            PDoubleArrayList val = new PDoubleArrayList();
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
            
            // Reset this flag
            hasInfeasibleBounds = false;
            
            // The gamma columns may need to be projected back onto the feasible region 
            for (int i=0; i<mpr.gammaVars.size(); i++) {
                GammaVar gv = mpr.gammaVars.get(i);
                if (gv.logProbs[c][m] < newLb || newUb < gv.logProbs[c][m]) {
                    // TODO: This isn't blazing fast, but we could make it faster if necessary
                    double[] params = Vectors.getExp(gv.logProbs[c]);
                    params = projections.getBoundedProjection(bounds, c, params);
                    if (params == null) {
                        //throw new IllegalStateException("The bounds are infeasible");
                        hasInfeasibleBounds = true;
                        break;
                    }
                    for (int mm=0; mm<params.length; mm++) {
                        assert params[mm] >= 0 : String.format("params[%d] = %g", mm, params[mm]);
                    }
                    double[] lps = Vectors.getLog(params);
                    gv.logProbs[c] = lps;
                    
                    // Update gamma var column  
                    updateGammaVar(gv, c);
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void updateGammaVar(GammaVar gv, int c) throws IloException {
        PIntArrayList rowind = new PIntArrayList();
        PIntArrayList colind = new PIntArrayList();
        PDoubleArrayList val = new PDoubleArrayList();

        int colindForGv = mp.lpMatrix.getIndex(gv.gammaVar);

        for (int m = 0; m < idm.getNumParams(c); m++) {
            // Only update non zero rows
            int totMaxFreqCm = idm.getTotUnsupervisedMaxFreqCm(c, m);
            if (totMaxFreqCm > 0) {
                // Add to the lower coupling constraint
                rowind.add(mp.lpMatrix.getIndex(mp.couplConsLower[c][m]));
                colind.add(colindForGv);
                val.add(totMaxFreqCm * gv.logProbs[c][m]);
            }
        }
        if (rowind.size() > 0) {
            mp.lpMatrix.setNZs(rowind.toNativeArray(), colind.toNativeArray(), val.toNativeArray());
        }
    }
    
}

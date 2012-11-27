package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

import java.io.File;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltProgram;
import edu.jhu.hltcoe.lp.CplexFactory;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder.DmvTreeProgram;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public class DmvLpRelaxation implements DmvRelaxation {

    private static final Logger log = Logger.getLogger(DmvLpRelaxation.class);

    protected IloCplex cplex;
    protected File tempDir;
    
    private static final double INTERNAL_BEST_SCORE = Double.NEGATIVE_INFINITY;
    private static final double INTERNAL_WORST_SCORE = Double.POSITIVE_INFINITY;
    private CplexFactory cplexFactory;
    private int maxCutRounds;
    private int numSolves;
    private Stopwatch simplexTimer;

    protected DmvTrainCorpus corpus;
    protected IndexedDmvModel idm;
    protected CptBounds bounds;
    protected Stopwatch parsingTimer;
    protected MasterProblem mp;
    private LpSumToOneBuilder sto;    
    private DmvObjective dmvObj;

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
        this.cplex = cplexFactory.getInstance();
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
    protected static class MasterProblem {
        public IloObjective objective;
        public IloNumVar[][] objVars;
        public IloLPMatrix origMatrix;
        public RltProgram rltProg;
    }
        
    private void buildModel(IloCplex cplex, DmvSolution initFeasSol) throws IloException {
        this.bounds = new CptBounds(this.idm);

        mp = new MasterProblem();
        
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
        DmvParseLpBuilder builder = new DmvParseLpBuilder(cplex, IlpFormulation.FLOW_PROJ_LPRELAX);
        DmvTreeProgram pp = builder.buildDmvTreeProgram(corpus);
        builder.addConsToMatrix(pp, mp.origMatrix);
                
        if (true) {
            // Add the convex/concave envelope.
            mp.rltProg = Rlt.getConvexConcaveEnvelope(cplex, mp.origMatrix);
            IloLPMatrix rltMat = mp.rltProg.getRltMatrix();
            mp.origMatrix.addRows(rltMat.getRanges());
            cplex.add(mp.origMatrix);
        } else {
            // Add the first-order RLT constraints.
            mp.rltProg = Rlt.getFirstOrderRlt(cplex, mp.origMatrix);
            // TODO: store this matrix.
            IloLPMatrix rltMat = mp.rltProg.getRltMatrix();
            cplex.add(rltMat);
        }
        
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
                mp.objVars[c][m] = mp.rltProg.getRltVar(sto.modelParamVars[c][m], pp.featCountVars[c][m]);
                // Negate the coefficients since we are minimizing
                cplex.setLinearCoef(mp.objective, -1.0, mp.objVars[c][m]);
            }
        }
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
    
    @Override
    public RelaxedSolution solveRelaxation(double incumbentScore) {
        // TODO Auto-generated method stub
        return null;
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
                // TODO: NOT COMPLETED.
                //mp.rltProg.updateBounds();
                throw new RuntimeException("not implemented");
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
            warmStart.numVars = mp.origMatrix.getNumVars();
            warmStart.ranges = mp.origMatrix.getRanges();
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

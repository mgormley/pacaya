package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.StringParam;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.CplexIlpSolver;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;

public class DmvDantzigWolfeRelaxation {

    private static Logger log = Logger.getLogger(CplexIlpSolver.class);

    private DmvBounds bounds;
    private File tempDir;
    private double workMemMegs;
    private int numThreads;
    private DmvModelFactory modelFactory;
    private SentenceCollection sentences;
    private IndexedDmvModel model;

    private int numLambdas;

    public DmvDantzigWolfeRelaxation(DmvBounds bounds, DmvModelFactory modelFactory, SentenceCollection sentences) {
        this.bounds = bounds;
        this.modelFactory = modelFactory;
        this.sentences = sentences;
        this.model = modelFactory.getInstance(sentences, bounds);
    }

    public void updateBounds(DmvBounds bounds) {
        // TODO: update bounds
        // TODO: recreate the model
    }

    public double solveRelaxation() {
        try {
            IloCplex cplex = new IloCplex();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, "cplex.log")));
            try {

                // TODO: decide how to get the initial feasible solution
                DmvSolution initFeasSol = new DmvSolution(null, null, 0.0);
                buildModel(cplex, initFeasSol);
                // TODO: add the initial feasible solution to cplex object

                // Specifies an upper limit on the amount of central memory, in
                // megabytes, that CPLEX is permitted to use for working memory
                // before swapping to disk files, compressing memory, or taking
                // other actions.
                // Values: Any nonnegative number, in megabytes; default: 128.0
                cplex.setParam(DoubleParam.WorkMem, workMemMegs);
                cplex.setParam(StringParam.WorkDir, tempDir.getAbsolutePath());

                cplex.setParam(IntParam.Threads, numThreads);

                // -1 = oportunistic, 0 = auto (default), 1 = deterministic
                // In this context, deterministic means that multiple runs with
                // the
                // same model at the same parameter settings on the same
                // platform
                // will reproduce the same solution path and results.
                cplex.setParam(IntParam.ParallelMode, 1);

                // TODO: can we use Dual instead of Primal? what advantage would
                // this give?
                // cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

                // TODO: For v12.3 only: cplex.setParam(IntParam.CloneLog, 1);
                cplex.setOut(out);
                cplex.setWarning(out);

                runDW(cplex);
                log.info("Solution status: " + cplex.getStatus());
                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value = " + cplex.getObjValue());
                double objective = cplex.getObjValue();

                // The use of importModel guarantees exactly one LP matrix
                // object.
                IloLPMatrix lp = (IloLPMatrix) cplex.LPMatrixIterator().next();
                IloNumVar[] vars = lp.getNumVars();
                double[] vals = cplex.getValues(lp);

                Map<String, Double> result = new HashMap<String, Double>();
                assert (vars.length == vals.length);
                for (int i = 0; i < vars.length; i++) {
                    result.put(vars[i].getName(), vals[i]);
                }

                cplex.writeSolution(new File(tempDir, "dw.sol").getAbsolutePath());
                return objective;
            } finally {
                cplex.end();
                out.close();
            }
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

    private void runDW(IloCplex master) throws IloException {

        while (true) {
            // Solve the master problem
            master.solve();

            for (int s = 0; s < sentences.size(); s++) {
                master.getDuals(matrix);
            }
        }

    }

    private void buildModel(IloMPModeler cplex, DmvSolution initFeasSol) throws IloException {

        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        // No contribution is made to the objective except by the slave problems
        IloObjective objective = cplex.addMaximize();

        int numConds = model.getNumConds();
        IloNumVar[][] modelParamVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            modelParamVars[c] = new IloNumVar[model.getNumParams(c)];
            for (int m = 0; m < modelParamVars[c].length; m++) {
                modelParamVars[c][m] = cplex.numVar(bounds.getLb(c, m), bounds.getUb(c, m), model.getName(c, m));
            }
        }

        IloNumVar[] sumVars = new IloNumVar[numConds];
        for (int c = 0; c < numConds; c++) {
            sumVars[c] = cplex.numVar(Double.MIN_VALUE, 1.0, "w_{" + c + "}");
        }

        // TODO: create these vectors at some point... but when?
        int numVectors = 0;
        double[][][] sumVectors = new double[numConds][numVectors][];

        for (int c = 0; c < numConds; c++) {
            for (int i = 0; i < numVectors; i++) {
                double vectorSum = 0.0;
                for (int m = 0; m < sumVectors[c][i].length; m++) {
                    vectorSum += (sumVectors[c][i][m] - 1.0) * Utilities.exp(sumVectors[c][i][m]);
                }
                IloLinearNumExpr vectorExpr = cplex.scalProd(sumVectors[c][i], modelParamVars[c]);
                vectorExpr.addTerm(-1.0, sumVars[c]);
                cplex.addLe(vectorExpr, vectorSum, String.format("maxVar(%d,%d)", c, i));
            }
        }

        // Add the coupling constraints considering only the model parameters
        IloRange[][] couplCons = new IloRange[sentences.size()][];
        for (int s = 0; s < sentences.size(); s++) {
            int sentLen = sentences.get(s).size();
            int numSentParams = model.getNumSentParams(sentLen);
            couplCons[s] = new IloRange[numSentParams];
            for (int i = 0; i < numSentParams; i++) {
                int c = model.getC(s, i);
                int m = model.getM(s, i);
                String name = String.format("minVar(%d,%d)", s, i);
                cplex.addLe(bounds.getLb(c, m), cplex.prod(1.0, modelParamVars[c][m]), name);
            }
        }

        // ----- column-wise modeling -----

        // Create the lambda sum to one constraints
        IloRange[] lambdaSumCons = new IloRange[sentences.size()];
        List<IloNumVar> lambdaVars = new ArrayList<IloNumVar>();

        // Add the initial feasible parse as a the first lambda column
        for (int s = 0; s < couplCons.length; s++) {
            IloColumn lambdaCol = cplex.column(objective, initFeasSol.getScore(s));
            for (int i = 0; i < couplCons[s].length; i++) {
                int c = model.getC(s, i);
                int m = model.getM(s, i);
                int e_sij = model.getSolValue(initFeasSol, s, i);
                double value = (bounds.getLb(c, m) - bounds.getUb(c, m)) * e_sij;
                lambdaCol = lambdaCol.and(cplex.column(couplCons[s][i], value));
            }

            // Add the lambda var to its sum to one constraint
            IloNumVar lambdaVar;
            if (lambdaSumCons[s] == null) {
                lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
                lambdaVars.add(lambdaVar);
                lambdaSumCons[s] = cplex.addLe(lambdaVar, 1.0);
            } else {
                throw new IllegalStateException(
                        "This point will not be reached unless we factor this section out into a method for later calls");
                // TODO: add these back in when refactored
                lambdaCol = lambdaCol.and(cplex.column(lambdaSumCons[s], 1.0));
                lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
            }
            lambdaVars.add(lambdaVar);
        }

        IloCplex cplex = (IloCplex) cplex;

        while (true) {
            // Solve the master problem
            cplex.solve();

            // Solve each slave problem
            int numPositiveRedCosts = 0;
            for (int s = 0; s < sentences.size(); s++) {
                // Get the simplex multipliers (shadow prices)
                double[] sentPrices = cplex.getDuals(couplCons[s]);
                double convexPrice = cplex.getDual(lambdaSumCons[s]);

                // Calculate new model parameter values for parser
                int numSentParams = couplCons[s].length;
                double[] sentParams = new double[numSentParams];
                for (int i = 0; i < numSentParams; i++) {
                    int c = model.getC(s, i);
                    int m = model.getM(s, i);

                    // zValue = 1.0 - (-1.0 * sentPrices[i]);
                    double zValue = 1.0 + sentPrices[i]; 
                    // eValue = 0.0 - (bounds.getLb(c, m) * sentPrices[i])
                    double eValue = -(bounds.getLb(c, m) * sentPrices[i]);
                    
                    sentParams[i] = zValue * bounds.getUb(c, m) + eValue;
                }

                Pair<int[], Double> pair = runSlaveProblem(s, sentParams);
                int[] sentSol = pair.get1(); 
                double reducedCost = pair.get2() - convexPrice;
                
                // TODO: double check that this is correct
                if (reducedCost > 0.0) {
                    // TODO: introduce a new lambda variable
                } // else: do nothing
            }
        }

    }

    public DepTreebank getProjectedParses() {

        // double[][][] fractionalParses;
        // for (int s=0; s<fractionalParses.length; s++) {
        // double[][] weights = fractionalParses[s];
        // // For non-projective case we'd do something like this.
        // // int[] parents = new int[weights.length];
        // // Edmonds eds = new Edmonds();
        // // CompleteGraph graph = new CompleteGraph(weights);
        // // eds.getMaxBranching(graph, 0, parents);
        // // For projective case we use a DP parser
        // }
        return null;
    }

    public DmvModel getProjectedModel() {
        // TODO Auto-generated method stub
        return null;
    }

    public double computeTrueObjective(DmvModel model, DepTreebank treebank) {
        // TODO Auto-generated method stub
        return 0;
    }

}

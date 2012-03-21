package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.StringParam;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.ilp.CplexIlpSolver;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;

public class DmvDantzigWolfeRelaxation {

    private static Logger log = Logger.getLogger(CplexIlpSolver.class);

    private DmvBounds bounds;
    private File tempDir;
    private double workMemMegs;
    private int numThreads;
    private SentenceCollection sentences;
    private IndexedDmvModel idm;

    private int numLambdas;

    private int cutCount;

    private IloCplex cplex;

    private MasterProblem mp;

    public DmvDantzigWolfeRelaxation(SentenceCollection sentences, DepTreebank initFeasSol) {
        this.sentences = sentences;
        this.idm = new IndexedDmvModel(sentences);
        this.bounds = new DmvBounds(this.idm);
        
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
        
    public void end() {
        cplex.end();
    }

    public RelaxedDmvSolution solveRelaxation() {
        try {
            runDWAlgo(cplex, mp);

            log.info("Solution status: " + cplex.getStatus());
            cplex.output().println("Solution status = " + cplex.getStatus());
            cplex.output().println("Solution value = " + cplex.getObjValue());
            double objective = cplex.getObjValue();

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
            for (int s = 0; s < sentences.size(); s++) {
                assert (Math.abs(Vectors.sum(fracParses[s]) + Vectors.sum(fracRoots[s]) - sentences.get(s).size()) < 1e-13);
            }

            cplex.writeSolution(new File(tempDir, "dw.sol").getAbsolutePath());
            return new RelaxedDmvSolution(logProbs, fracRoots, fracParses, objective);
        } catch (IloException e) {
            if (e instanceof ilog.cplex.CpxException) {
                ilog.cplex.CpxException cpxe = (ilog.cplex.CpxException) e;
                System.err.println("STATUS CODE: " + cpxe.getStatus());
                System.err.println("ERROR MSG:   " + cpxe.getMessage());
            }
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
        // From the CPLEX documentation: the Dual algorithm can take better advantage of a previous solve. 
        // http://ibm.co/GHorLT
        // cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);
        
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
        public IloRange[][] couplCons;
        public IloRange[] lambdaSumCons;
        public List<LambdaVar> lambdaVars;
        public IloNumVar[][] modelParamVars;
        public IloNumVar[] sumVars;
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
        private double objCoef;
        
        public LambdaVar(IloNumVar lambdaVar, int s, int[] parents, int[] sentSol, double objCoef) {
            super();
            this.lambdaVar = lambdaVar;
            this.s = s;
            this.parents = parents;
            this.sentSol = sentSol;
            // Note: if we stored the objective as an IloLinearNumExpr, we would be
            // able to iterate through the objective coefficients when updating lambda variables
            // but this would likely be slow for when there are a large number of columns.
            this.objCoef = objCoef;
        }
        
    }

    private MasterProblem buildModel(IloMPModeler cplex, DepTreebank initFeasSol) throws IloException {

        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        // No contribution is made to the objective except by the slave problems
        IloObjective objective = cplex.addMaximize();

        int numConds = idm.getNumConds();
        IloNumVar[][] modelParamVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            modelParamVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m = 0; m < modelParamVars[c].length; m++) {
                modelParamVars[c][m] = cplex.numVar(bounds.getLb(c, m), bounds.getUb(c, m), idm.getName(c, m));
            }
        }
        
        IloNumVar[] sumVars = new IloNumVar[numConds];
        for (int c = 0; c < numConds; c++) {
            sumVars[c] = cplex.numVar(Double.MIN_VALUE, 1.0, "w_{" + c + "}");
        }

        // Create the cut vectors for sum-to-one constraints
        double[][][] pointsArray = getInitialPoints();
        cutCount = 0;
        // Add the initial cuts
        for (int c = 0; c < numConds; c++) {
            for (int i = 0; i < pointsArray[c].length; i++) {
                double[] probs = pointsArray[c][i];
                addSumToOneConstraint(cplex, modelParamVars, sumVars, c, probs);
            }
        }

        // Add the coupling constraints considering only the model parameters
        // aka. the relaxed-objective-coupling-constraint
        IloRange[][] couplCons = new IloRange[sentences.size()][];
        for (int s = 0; s < sentences.size(); s++) {
            int numSentVars = idm.getNumSentVars(s);
            couplCons[s] = new IloRange[numSentVars];
            for (int i = 0; i < numSentVars; i++) {
                int c = idm.getC(s, i);
                int m = idm.getM(s, i);
                String name = String.format("minVar(%d,%d)", s, i);
                double maxFreqScm = idm.getMaxFreq(s,i);
                cplex.addLe(maxFreqScm * bounds.getLb(c, m), cplex.prod(maxFreqScm, modelParamVars[c][m]), name);
            }
        }

        // ----- column-wise modeling -----

        // Create the lambda sum to one constraints
        IloRange[] lambdaSumCons = new IloRange[sentences.size()];
        List<LambdaVar> lambdaVars = new ArrayList<LambdaVar>();

        // Add the initial feasible parse as the first lambda columns
        for (int s = 0; s < couplCons.length; s++) {
            DepTree tree = initFeasSol.get(s);
            addLambdaVar(cplex, objective, couplCons, lambdaSumCons, lambdaVars, s, tree);
        }

        MasterProblem mp = new MasterProblem();
        mp.objective = objective;
        mp.modelParamVars = modelParamVars;
        mp.sumVars = sumVars;
        mp.couplCons = couplCons;
        mp.lambdaSumCons = lambdaSumCons;
        mp.lambdaVars = lambdaVars;

        return mp;
    }

    private double[][][] getInitialPoints() throws IloException {
        int numConds = idm.getNumConds();
        double[][][] vectors = new double[numConds][][];

        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            // Create numParams^2 vectors
            int numVectors = (int) Math.pow(numParams, 2.0);
            vectors[c] = new double[numVectors][];
            for (int i = 0; i < vectors[c].length; i++) {
                double[] vector = new double[numParams];
                // Randomly initialize the parameters
                for (int m = 0; m < numParams; m++) {
                    vector[m] = Prng.random.nextDouble();
                }
                vectors[c][i] = vector;
            }
        }
        return vectors;
    }

    private void addSumToOneConstraint(IloMPModeler cplex, IloNumVar[][] modelParamVars, IloNumVar[] sumVars, int c,
            double[] point) throws IloException {
        
        // TODO: should this respect the bounds?
        double[] probs = Projections.getProjectedParams(bounds, c, point);
        double[] logProbs = Vectors.getLogForIlp(probs);
        
        double vectorSum = 0.0;
        for (int m = 0; m < logProbs.length; m++) {
            vectorSum += (logProbs[m] - 1.0) * probs[m];
        }

        IloLinearNumExpr vectorExpr = cplex.scalProd(probs, modelParamVars[c]);
        vectorExpr.addTerm(-1.0, sumVars[c]);
        cplex.addLe(vectorExpr, vectorSum, String.format("maxVar(%d)-%d", c, cutCount++));
    }

    private void addLambdaVar(IloMPModeler cplex, IloObjective objective, IloRange[][] couplCons,
            IloRange[] lambdaSumCons, List<LambdaVar> lambdaVars, int s, DepTree tree)
            throws IloException {

        int[] sentSol = idm.getSentSol(sentences.get(s), s, tree);
        int numSentVars = couplCons[s].length;
        double objCoef = 0.0;
        for (int i = 0; i < numSentVars; i++) {
            int c = idm.getC(s, i);
            int m = idm.getM(s, i);
            objCoef += sentSol[i] * bounds.getUb(c, m);
        }
        IloColumn lambdaCol = cplex.column(objective, objCoef);

        // Add the lambda var to the relaxed-objective-coupling-constraint
        for (int i = 0; i < couplCons[s].length; i++) {
            int c = idm.getC(s, i);
            int m = idm.getM(s, i);
            // bounds.getLb(c, m) * freqScmVal - zScmVal
            double value = (bounds.getLb(c, m) - bounds.getUb(c, m)) * sentSol[i];
            lambdaCol = lambdaCol.and(cplex.column(couplCons[s][i], value));
        }

        // Add the lambda var to its sum to one constraint
        IloNumVar lambdaVar;
        if (lambdaSumCons[s] == null) {
            lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
            lambdaSumCons[s] = cplex.addLe(lambdaVar, 1.0);
        } else {
            lambdaCol = lambdaCol.and(cplex.column(lambdaSumCons[s], 1.0));
            lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
        }
        lambdaVars.add(new LambdaVar(lambdaVar, s, tree.getParents(), sentSol, objCoef));
    }

    public void runDWAlgo(IloCplex cplex, MasterProblem mp) throws UnknownObjectException, IloException {
        IloObjective objective = mp.objective;
        IloNumVar[][] modelParamVars = mp.modelParamVars;
        IloNumVar[] sumVars = mp.sumVars;
        IloRange[][] couplCons = mp.couplCons;
        IloRange[] lambdaSumCons = mp.lambdaSumCons;
        List<LambdaVar> lambdaVars = mp.lambdaVars;

        int numCuts = 10;
        
        // Outer loop runs D-W and then adds cuts for sum-to-one constraints
        for (int cut=0; cut<numCuts; cut++) {
        
            // Solve the full D-W problem
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
                    // based on the relaxed-objective-coupling-constraint
                    int numSentVars = couplCons[s].length;
                    double[] sentParams = new double[numSentVars];
                    for (int i = 0; i < numSentVars; i++) {
                        int c = idm.getC(s, i);
                        int m = idm.getM(s, i);
    
                        // zValue = 1.0 - (-1.0 * sentPrices[i]);
                        // eValue = 0.0 - (bounds.getLb(c, m) * sentPrices[i])
                        double zValue = 1.0 + sentPrices[i];
                        double eValue = -(bounds.getLb(c, m) * sentPrices[i]);
    
                        sentParams[i] = zValue * bounds.getUb(c, m) + eValue;
                    }
    
                    Pair<DepTree, Double> pair = solveSlaveProblem(s, sentParams);
                    DepTree tree = pair.get1();
                    double reducedCost = pair.get2() - convexPrice;
    
                    // TODO: double check that this if-statement is correct
                    if (reducedCost > 0.0) {
                        numPositiveRedCosts++;
                        // Introduce a new lambda variable
                        addLambdaVar(cplex, objective, couplCons, lambdaSumCons, lambdaVars, s, tree);
                    } // else: do nothing
                }
                if (numPositiveRedCosts == 0) {
                    // Optimal solution found
                    break;
                }
            }
            
            // Add a cut for each distribution by projecting the model parameters
            // back onto the simplex.
            for (int c = 0; c < idm.getNumConds(); c++) {
                double[] params = cplex.getValues(mp.modelParamVars[c]);
                Vectors.exp(params);
                addSumToOneConstraint(cplex, modelParamVars, sumVars, cut, params);
            }
        }

    }

    private Pair<DepTree, Double> solveSlaveProblem(int s, double[] sentParams) {
        DmvCkyParser parser = new DmvCkyParser();
        Sentence sentence = sentences.get(s);
        DepSentenceDist sd = idm.getDepSentenceDist(sentence, s, sentParams);
        return parser.parse(sentence, sd);
    }

    public void reverseApply(DmvBoundsDelta deltas) {
        double lbDelt = - deltas.getLbDelta();
        double ubDelt = - deltas.getUbDelta();
        applyDelta(deltas.getC(), deltas.getM(), lbDelt, ubDelt);
    }

    public void forwardApply(DmvBoundsDelta deltas) {
        double lbDelt = deltas.getLbDelta();
        double ubDelt = deltas.getUbDelta();
        applyDelta(deltas.getC(), deltas.getM(), lbDelt, ubDelt);
    }

    private void applyDelta(int c, int m, double lbDelt, double ubDelt) {
        try {
            double newLb = mp.modelParamVars[c][m].getLB() + lbDelt;
            double newUb = mp.modelParamVars[c][m].getUB() + ubDelt;

            // Updates the bounds of the model parameters
            bounds.set(c, m, newLb, newUb);
            mp.modelParamVars[c][m].setLB(newLb);
            mp.modelParamVars[c][m].setUB(newUb);

            // Update lambda column if it uses parameter c,m
            for (LambdaVar lv : mp.lambdaVars) {
                int i = idm.getSi(lv.s, c, m);
                if (i != -1) {
                    // Update the objective coefficient
                    lv.objCoef += lv.sentSol[i] * ubDelt;
                    cplex.setLinearCoef(mp.objective, lv.objCoef, lv.lambdaVar);

                    // Update the coupling constraint coefficient
                    double value = (bounds.getLb(c, m) - bounds.getUb(c, m)) * lv.sentSol[i];
                    cplex.setLinearCoef(mp.couplCons[lv.s][i], value, lv.lambdaVar);
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    public DmvBounds getBounds() {
        return bounds;
    }

    public IndexedDmvModel getIdm() {
        return idm;
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
                score += sentSol[i] * logProbs[c][m];
            }
        }
        return score;
    }
    
}

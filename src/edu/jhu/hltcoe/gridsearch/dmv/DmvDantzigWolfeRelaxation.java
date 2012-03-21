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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Triple;
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

    private int cutCount;

    public DmvDantzigWolfeRelaxation(DmvModelFactory modelFactory, SentenceCollection sentences) {
        this.bounds = new DmvBounds();
        this.modelFactory = modelFactory;
        this.sentences = sentences;
        this.model = new IndexedDmvModel(sentences);
        // TODO: do we need this model factory?
        // modelFactory.getInstance(sentences, bounds);
    }

    public RelaxedDmvSolution solveRelaxation() {
        try {
            // TODO: reuse cplex object? This might be complicated with all the
            // upper lower bound usage
            IloCplex cplex = new IloCplex();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, "cplex.log")));
            try {

                // TODO: decide how to get the initial feasible solution
                DmvSolution initFeasSol = new DmvSolution(null, null, 0.0);
                MasterProblem mp = buildModel(cplex, initFeasSol);
                // TODO: add the initial feasible solution to cplex object

                setCplexParams(cplex, out);

                runDWAlgo(cplex, mp);

                log.info("Solution status: " + cplex.getStatus());
                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value = " + cplex.getObjValue());
                double objective = cplex.getObjValue();

                // Store optimal model parameters
                double[][] modelParams = new double[model.getNumConds()][];
                for (int c = 0; c < model.getNumConds(); c++) {
                    modelParams[c] = cplex.getValues(mp.modelParamVars[c]);
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
                return new RelaxedDmvSolution(modelParams, fracRoots, fracParses, objective);
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

    private void setCplexParams(IloCplex cplex, OutputStream out) throws IloException {
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
        
        public LambdaVar(IloNumVar lambdaVar, int s, int[] parents) {
            super();
            this.lambdaVar = lambdaVar;
            this.s = s;
            this.parents = parents;
        }
        
    }

    private MasterProblem buildModel(IloMPModeler cplex, DmvSolution initFeasSol) throws IloException {

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
            int numSentVars = model.getNumSentVars(s);
            couplCons[s] = new IloRange[numSentVars];
            for (int i = 0; i < numSentVars; i++) {
                int c = model.getC(s, i);
                int m = model.getM(s, i);
                String name = String.format("minVar(%d,%d)", s, i);
                double maxFreqScm = model.getMaxFreq(s,i);
                cplex.addLe(maxFreqScm * bounds.getLb(c, m), cplex.prod(maxFreqScm, modelParamVars[c][m]), name);
            }
        }

        // ----- column-wise modeling -----

        // Create the lambda sum to one constraints
        IloRange[] lambdaSumCons = new IloRange[sentences.size()];
        List<LambdaVar> lambdaVars = new ArrayList<LambdaVar>();

        // Add the initial feasible parse as a the first lambda column
        for (int s = 0; s < couplCons.length; s++) {
            DepTree tree = initFeasSol.getDepTreebank().get(s);
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
        int numConds = model.getNumConds();
        double[][][] vectors = new double[numConds][][];

        for (int c = 0; c < numConds; c++) {
            int numParams = model.getNumParams(c);
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

        int[] sentSol = model.getSentSol(sentences.get(s), s, tree);
        int numSentVars = couplCons[s].length;
        double sentScore = 0.0;
        for (int i = 0; i < numSentVars; i++) {
            int c = model.getC(s, i);
            int m = model.getM(s, i);
            sentScore += sentSol[i] * bounds.getUb(c, m);
        }
        IloColumn lambdaCol = cplex.column(objective, sentScore);

        // Add the lambda var to the relaxed-objective-coupling-constraint
        for (int i = 0; i < couplCons[s].length; i++) {
            int c = model.getC(s, i);
            int m = model.getM(s, i);
            int freqScmVal = sentSol[i];
            // bounds.getLb(c, m) * freqScmVal - zScmVal
            double value = (bounds.getLb(c, m) - bounds.getUb(c, m)) * freqScmVal;
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
        lambdaVars.add(new LambdaVar(lambdaVar, s, tree.getParents()));
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
                        int c = model.getC(s, i);
                        int m = model.getM(s, i);
    
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
            for (int c = 0; c < model.getNumConds(); c++) {
                double[] params = cplex.getValues(mp.modelParamVars[c]);
                Vectors.exp(params);
                addSumToOneConstraint(cplex, modelParamVars, sumVars, cut, params);
            }
        }

    }

    private Pair<DepTree, Double> solveSlaveProblem(int s, double[] sentParams) {
        DmvCkyParser parser = new DmvCkyParser();
        Sentence sentence = sentences.get(s);
        DepSentenceDist sd = model.getDepSentenceDist(sentence, s, sentParams);
        return parser.parse(sentence, sd);
    }
    
    public double computeTrueObjective(DmvModel model, DepTreebank treebank) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void reverseApply(DmvBoundsDelta bounds2) {
        // TODO Auto-generated method stub
        
    }

    public void forwardApply(DmvBoundsDelta bounds2) {
        // TODO Auto-generated method stub
        
    }

    public DmvBounds getBounds() {
        return bounds;
    }

    public IndexedDmvModel getIdm() {
        return model;
    }

}

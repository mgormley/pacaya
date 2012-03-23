package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
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
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.EagerBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Dir;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class DmvDantzigWolfeRelaxation {

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

    public DmvDantzigWolfeRelaxation(SentenceCollection sentences, DepTreebank initFeasSol) {
        this(sentences, initFeasSol, null, 20, new CutCountComputer());
    }
    
    public DmvDantzigWolfeRelaxation(SentenceCollection sentences, DepTreebank initFeasSol, File tempDir,
            int numCutRounds, CutCountComputer initCutCountComp) {
        this.sentences = sentences;
        this.tempDir = tempDir;

        this.idm = new IndexedDmvModel(sentences);
        this.bounds = new DmvBounds(this.idm);
        this.projections = new Projections(tempDir);
        
        // TODO: pass these through
        this.numThreads = 1;
        this.workMemMegs = 128;
        this.numCutRounds = numCutRounds;
        this.initCutCountComp = initCutCountComp;
        // Counter for printing 
        this.cutCounter = 0;
        
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
            }
            
            log.info("Solution status: " + cplex.getStatus());
            cplex.output().println("Solution status = " + cplex.getStatus());
            if (cplex.getStatus() != Status.Optimal) {
                return new RelaxedDmvSolution(null, null, null, EagerBranchAndBoundSolver.WORST_SCORE);
            }
            log.info("Solution value: " + cplex.getObjValue());
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
            
            if (tempDir != null) {
                cplex.writeSolution(new File(tempDir, "dw.sol").getAbsolutePath());
            }
            
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
        public IloNumVar[][] modelParamVars;
        public IloNumVar[] sumVars;
        public IloRange[][] couplConsLower;
        public IloRange[][] couplConsUpper;
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
        
        public LambdaVar(IloNumVar lambdaVar, int s, int[] parents, int[] sentSol) {
            super();
            this.lambdaVar = lambdaVar;
            this.s = s;
            this.parents = parents;
            this.sentSol = sentSol;
        }
        
    }

    private MasterProblem buildModel(IloMPModeler cplex, DepTreebank initFeasSol) throws IloException {

        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        // Create the model parameter variables
        int numConds = idm.getNumConds();
        IloNumVar[][] modelParamVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            modelParamVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m = 0; m < modelParamVars[c].length; m++) {
                modelParamVars[c][m] = cplex.numVar(bounds.getLb(c, m), bounds.getUb(c, m), idm.getName(c, m));
            }
        }
        
        // Create sum-to-one constraint helper variables        
        IloNumVar[] sumVars = new IloNumVar[numConds];
        for (int c = 0; c < numConds; c++) {
            sumVars[c] = cplex.numVar(-Double.MAX_VALUE, 1.0, "w_{" + c + "}");
        }

        // Create the cut vectors for sum-to-one constraints
        double[][][] pointsArray = getInitialPoints();
        // Add the initial cuts
        for (int c = 0; c < numConds; c++) {
            for (int i = 0; i < pointsArray[c].length; i++) {
                double[] probs = pointsArray[c][i];
                addSumToOneConstraint(cplex, modelParamVars, sumVars, c, probs);
            }
        }

        // Create the objective
        IloObjective objective = cplex.addMaximize();
        // Create the objective variables, adding them to the objective
        IloNumVar[][] objVars = new IloNumVar[numConds][];
        IloLinearNumExpr objExpr = cplex.linearNumExpr();
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            objVars[c] = new IloNumVar[numParams];
            for (int m=0; m<numParams; m++) {
                objVars[c][m] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, String.format("z_{%d,%d}",c,m));
                cplex.setLinearCoef(objective, 1.0, objVars[c][m]);
            }
        }
        
        // Add the coupling constraints considering only the model parameters
        // aka. the relaxed-objective-coupling-constraints
        IloRange[][] couplConsLower = new IloRange[numConds][];
        IloRange[][] couplConsUpper = new IloRange[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            couplConsLower[c] = new IloRange[numParams];
            couplConsUpper[c] = new IloRange[numParams];
            for (int m=0; m<numParams; m++) {
                String name;
                
                // Add the lower coupling constraint
                name = String.format("ccLb(%d,%d)", c, m);   
                double maxFreqCm = idm.getTotalMaxFreqCm(c,m);
                IloNumExpr rhs = cplex.sum(cplex.prod(-1.0, objVars[c][m]), 
                                            cplex.prod(maxFreqCm, modelParamVars[c][m]));
                couplConsLower[c][m] = cplex.addLe(maxFreqCm * bounds.getLb(c,m), rhs, name);
                
                // Add the upper coupling constraint
                name = String.format("ccUb(%d,%d)", c, m);
                couplConsUpper[c][m] = cplex.addLe(0.0, cplex.prod(-1.0, objVars[c][m]), name);
            }
        }

        // ----- column-wise modeling -----

        // Create the lambda sum to one constraints
        IloRange[] lambdaSumCons = new IloRange[sentences.size()];
        List<LambdaVar> lambdaVars = new ArrayList<LambdaVar>();

        // Add the initial feasible parse as the first lambda columns
        for (int s = 0; s < sentences.size(); s++) {
            DepTree tree = initFeasSol.get(s);
            addLambdaVar(cplex, objective, couplConsLower, couplConsUpper, lambdaSumCons, lambdaVars, s, tree);
        }

        MasterProblem mp = new MasterProblem();
        mp.objective = objective;
        mp.modelParamVars = modelParamVars;
        mp.sumVars = sumVars;
        mp.couplConsLower = couplConsLower;
        mp.couplConsUpper = couplConsUpper;
        mp.lambdaSumCons = lambdaSumCons;
        mp.lambdaVars = lambdaVars;

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

    private void addSumToOneConstraint(IloMPModeler cplex, IloNumVar[][] modelParamVars, IloNumVar[] sumVars, int c,
            double[] point) throws IloException {
        
        // TODO: should this respect the bounds?
        double[] probs = projections.getProjectedParams(bounds, c, point);
        double[] logProbs = Vectors.getLog(probs);
        
        double vectorSum = 0.0;
        for (int m = 0; m < logProbs.length; m++) {
            vectorSum += (logProbs[m] - 1.0) * probs[m];
        }

        IloLinearNumExpr vectorExpr = cplex.scalProd(probs, modelParamVars[c]);
        vectorExpr.addTerm(-1.0, sumVars[c]);
        cplex.addLe(vectorExpr, vectorSum, String.format("maxVar(%d)-%d", c, cutCounter++));
    }

    private void addLambdaVar(IloMPModeler cplex, IloObjective objective, IloRange[][] couplConsLower, IloRange[][] couplConsUpper,
            IloRange[] lambdaSumCons, List<LambdaVar> lambdaVars, int s, DepTree tree)
            throws IloException {

        int[] sentSol = idm.getSentSol(sentences.get(s), s, tree);
        int numSentVars = idm.getNumSentVars(s);
        
        IloColumn lambdaCol = cplex.column(objective, 0.0);

        // Add the lambda var to the relaxed-objective-coupling-constraints
        for (int i = 0; i < numSentVars; i++) {
            int c = idm.getC(s, i);
            int m = idm.getM(s, i);
            
            double value;
            // Add to the lower coupling constraint
            // TODO: this used to cause numerical precision errors close to zero
            value = bounds.getLb(c, m) * sentSol[i];
            lambdaCol = lambdaCol.and(cplex.column(couplConsLower[c][m], value));
            
            // Add to the upper coupling constraint
            value = bounds.getUb(c, m) * sentSol[i];
            lambdaCol = lambdaCol.and(cplex.column(couplConsUpper[c][m], value));
        }

        // Add the lambda var to its sum to one constraint
        IloNumVar lambdaVar;
        if (lambdaSumCons[s] == null) {
            lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
            lambdaSumCons[s] = cplex.addEq(lambdaVar, 1.0, "lambdaSum");
        } else {
            lambdaCol = lambdaCol.and(cplex.column(lambdaSumCons[s], 1.0));
            lambdaVar = cplex.numVar(lambdaCol, 0.0, 1.0, String.format("lambda_{%d}^{%d}", s, numLambdas++));
        }
        lambdaVars.add(new LambdaVar(lambdaVar, s, tree.getParents(), sentSol));
        
//        // TODO: remove
//        for (int i = 0; i < couplCons[s].length; i++) {
//            log.trace(couplCons[s][i]);
//        }
    }

    public void runDWAlgo(IloCplex cplex, MasterProblem mp) throws UnknownObjectException, IloException {
        IloObjective objective = mp.objective;
        IloNumVar[][] modelParamVars = mp.modelParamVars;
        IloNumVar[] sumVars = mp.sumVars;
        IloRange[][] couplConsLower = mp.couplConsLower;
        IloRange[][] couplConsUpper = mp.couplConsUpper;
        IloRange[] lambdaSumCons = mp.lambdaSumCons;
        List<LambdaVar> lambdaVars = mp.lambdaVars;
        
        DmvCkyParser parser = new DmvCkyParser();
        
        // Outer loop runs D-W and then adds cuts for sum-to-one constraints
        for (int cut=0; cut<numCutRounds; cut++) {
        
            // Solve the full D-W problem
            while (true) {
                // Solve the master problem
                cplex.solve();

                log.info("Master solution status: " + cplex.getStatus());
                if (cplex.getStatus() == Status.Infeasible) {
                    return;
                }
                log.info("Master solution value: " + cplex.getObjValue());
                
                // Get the simplex multipliers (shadow prices).
                // These are shared across all slaves, since each slave
                // has the same D_s matrix. 
                int numConds = idm.getNumConds();
                double[][] prices = new double[numConds][];
                for (int c = 0; c < numConds; c++) {
                    int numParams = idm.getNumParams(c);
                    prices[c] = new double[numParams];
                    double[] pricesLower = cplex.getDuals(couplConsLower[c]);
                    double[] pricesUpper = cplex.getDuals(couplConsUpper[c]);
                    
                    for (int m=0; m<numParams; m++) {
                        // Calculate new model parameter values for parser
                        // based on the relaxed-objective-coupling-constraints
                        prices[c][m] = - (pricesLower[m]*bounds.getLb(c,m) + pricesUpper[m]*bounds.getUb(c,m));
                    }
                }

                DepProbMatrix dpm = idm.getDepProbMatrix(prices);

                // Get the simplex multipliers (shadow prices) for the lambda sentence constraints
                double[] convexPrices = cplex.getDuals(lambdaSumCons);
                
                // Solve each slave problem
                int numPositiveRedCosts = 0;
                for (int s = 0; s < sentences.size(); s++) {
        
                    Pair<DepTree, Double> pair = parser.parse(sentences.get(s), dpm);
                    DepTree tree = pair.get1();
                    double reducedCost = pair.get2() - convexPrices[s];
    
                    // TODO: double check that this if-statement is correct
                    if (reducedCost > 0.0) {
                        numPositiveRedCosts++;
                        // Introduce a new lambda variable
                        addLambdaVar(cplex, objective, couplConsLower, couplConsUpper, lambdaSumCons, lambdaVars, s, tree);
                    } // else: do nothing
                }
                if (numPositiveRedCosts == 0) {
                    // Optimal solution found
                    break;
                }
            }
            
            // Don't add more cuts after the final solution is found
            if (cut == numCutRounds -1) {
                break;
            }

            // Add a cut for each distribution by projecting the model parameters
            // back onto the simplex.
            log.debug("Adding cuts, round " + cut);
            double[][] logProbs = new double[idm.getNumConds()][];
            for (int c = 0; c < idm.getNumConds(); c++) {
                logProbs[c] = cplex.getValues(mp.modelParamVars[c]);
            }
            for (int c = 0; c < idm.getNumConds(); c++) {
                Vectors.exp(logProbs[c]);
                addSumToOneConstraint(cplex, modelParamVars, sumVars, c, logProbs[c]);
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
            if (delta.getLu() == Lu.LOWER && delta.getDir() == Dir.ADD) {
                newLb = Utilities.logAdd(origLb, delta.getDelta());
            } else if (delta.getLu() == Lu.LOWER && delta.getDir() == Dir.SUBTRACT) {
                newLb = Utilities.logSubtract(origLb, delta.getDelta());
            } else if (delta.getLu() == Lu.UPPER && delta.getDir() == Dir.ADD) {
                newUb = Utilities.logAdd(origUb, delta.getDelta());
            } else if (delta.getLu() == Lu.UPPER && delta.getDir() == Dir.SUBTRACT) {
                newUb = Utilities.logSubtract(origUb, delta.getDelta());
            } else {
                throw new IllegalStateException();
            }

            // Updates the bounds of the model parameters
            bounds.set(c, m, newLb, newUb);
            mp.modelParamVars[c][m].setLB(newLb);
            mp.modelParamVars[c][m].setUB(newUb);

            // Update lambda column if it uses parameter c,m
            for (LambdaVar lv : mp.lambdaVars) {
                int i = idm.getSi(lv.s, c, m);
                if (i != -1) {
                    double value;
                    // Update the lower coupling constraint coefficient
                    value = bounds.getLb(c, m) * lv.sentSol[i];
                    cplex.setLinearCoef(mp.couplConsLower[c][m], value, lv.lambdaVar);
                    // Update the upper coupling constraint coefficient
                    value = bounds.getUb(c, m) * lv.sentSol[i];
                    cplex.setLinearCoef(mp.couplConsUpper[c][m], value, lv.lambdaVar);
                }
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
                score += sentSol[i] * logProbs[c][m];
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

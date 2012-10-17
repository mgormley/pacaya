package edu.jhu.hltcoe.gridsearch.dmv;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.hltcoe.gridsearch.cpt.Projections;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.ilp.ZimplRunner;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.jhu.hltcoe.parse.ZimplXmlCodeContainerReader;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;

public class DmvLpRelaxation implements DmvRelaxation {

    static final double MIN_SUM_FOR_CUT = 1.01;

    private static Logger log = Logger.getLogger(DmvLpRelaxation.class);

    private CptBounds bounds;
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
    
    public DmvLpRelaxation(File tempDir,
            int numCutRounds, CutCountComputer initCutCountComp) {
        this.tempDir = tempDir;
        this.projections = new Projections(tempDir);
        
        // TODO: pass these through
        this.numThreads = 1;
        this.workMemMegs = 256;
        this.numCutRounds = numCutRounds;
        this.initCutCountComp = initCutCountComp;
        // Counter for printing 
        this.cutCounter = 0;
    }

    public RelaxedDmvSolution solveRelaxation() {
        // TODO: write this
        throw new RuntimeException("not implemented");    
    }

    public double[][] getRegretCm() {
        // TODO: write this
        throw new RuntimeException("not implemented");    
    }

    private void setCplexParams(IloCplex cplex) throws IloException, FileNotFoundException {
        // TODO: write this
        throw new RuntimeException("not implemented");    
    }

    /**
     * Convenience class for passing around Master Problem variables
     */
    private static class MasterProblem {
        public IloObjective objective;

        public IloNumVar[][] modelParamVars;
        public IloNumVar[][] objVars;
        public IloRange[][] couplConsLower;
        public IloRange[][] couplConsUpper;
        public IloLPMatrix couplMatrix;
        public int numStoCons = 0;
        
        // arc[s,i,j] where i is parent and j is child
        public IloNumVar[][][] arcVars;
        // stopAdj[s,i,lr]
        public IloNumVar[][][] stopAdjVars;
        // stopNonAdj[s,i,lr]
        public IloNumVar[][][] stopNonAdjVars;
        // genAdj[s,i,lr]
        public IloNumVar[][][] genAdjVars;
        // numNonAdj[s,i,lr]
        public IloNumVar[][][] numNonAdjVars;
    }

    protected File createZimplFile() throws FileNotFoundException {
        // TODO: parameterize formulation to try different options
        IlpFormulation formulation = IlpFormulation.FLOW_PROJ_LPRELAX;
        
        ZimplXmlCodeContainerReader codeMap = new ZimplXmlCodeContainerReader(formulation);
        codeMap.loadZimplCodeFromResource(IlpViterbiParser.ZIMPL_CODE_XML);
        
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(codeMap.getCodeSnippet("setup"));
        if (formulation != IlpFormulation.MFLOW_NONPROJ && formulation != IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet("deptree-general"));
        }
        if (formulation == IlpFormulation.FLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.FLOW_NONPROJ));
        } else if (formulation == IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.MFLOW_NONPROJ));
        }
        String formulationId = formulation.toString().replace("-lprelax","");
        zimplWriter.write(codeMap.getCodeSnippet(formulationId));
        
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-support"));
        zimplWriter.write(codeMap.getCodeSnippet("bnb-lprelax-objective"));
        zimplWriter.close();
        return zimplFile;
    }
    
    private MasterProblem buildModel(IloCplex cplex, DepTreebank initFeasSol) throws IloException, IOException {
        mp = new MasterProblem();
        
        // Add tree constraints

        File zimplFile = createZimplFile();        
        ZimplRunner zimpl = new ZimplRunner(zimplFile, tempDir, "lp");
        zimpl.runZimpl();
        cplex.importModel(zimpl.getIlpFile().getAbsolutePath());
        
        // ----- row-wise modeling -----
        // Add x_0 constraints in the original model space first

        // Create the model parameter variables
        int numConds = idm.getNumConds();
        mp.modelParamVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            mp.modelParamVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m = 0; m < mp.modelParamVars[c].length; m++) {
                mp.modelParamVars[c][m] = cplex.numVar(bounds.getLb(Type.PARAM, c, m), bounds.getUb(Type.PARAM, c, m), idm.getName(c, m));
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
                double maxFreqCm = idm.getUnsupervisedMaxTotalFreqCm(c,m);
                IloNumExpr rhsLower = cplex.sum(slackVarLower,
                                        cplex.diff(cplex.prod(maxFreqCm, mp.modelParamVars[c][m]), mp.objVars[c][m]));
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
        mp.couplMatrix = cplex.addLPMatrix("couplingMatrix");
        for (int c = 0; c < numConds; c++) {
            mp.couplMatrix.addRows(mp.couplConsLower[c]);
        }
        for (int c = 0; c < numConds; c++) {
            mp.couplMatrix.addRows(mp.couplConsUpper[c]);
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

    public void runDWAlgo(IloCplex cplex, MasterProblem mp) throws UnknownObjectException, IloException {        
        DmvCkyParser parser = new DmvCkyParser();
        
        double prevObjVal = Double.POSITIVE_INFINITY;
        // Outer loop runs D-W and then adds cuts for sum-to-one constraints
        for (int cut=0; cut<numCutRounds; cut++) {
            // Solve the LP relaxation
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
    
    private void applyDelta(CptBoundsDelta delta) {
        // TODO: write this
        throw new RuntimeException("not implemented");
    }
    
    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        // TODO: write this
        throw new RuntimeException("not implemented");
    }

    public CptBounds getBounds() {
        return bounds;
    }

    public IndexedDmvModel getIdm() {
        return idm;
    }
        
    public void end() {
        cplex.end();
    }

    @Override
    public WarmStart getWarmStart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init2(DmvSolution initFeasSol) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void init1(DmvTrainCorpus corpus) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setWarmStart(WarmStart warmStart) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public RelaxedDmvSolution solveRelaxation(double incumbentScore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addFeasibleSolution(DmvSolution initFeasSol) {
        // TODO Auto-generated method stub
        
    }
    
}

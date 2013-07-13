package edu.jhu.gridsearch.cpt;

import edu.jhu.prim.list.IntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.gridsearch.cpt.Projections.ProjectionsPrm;
import edu.jhu.util.Prng;
import edu.jhu.util.Sets;
import edu.jhu.util.math.Vectors;

public class LpSumToOneBuilder {
    
    public static class LpStoBuilderPrm {
        public int maxSetSizeToConstrain = 0;
        public double minSumForCuts = DEFAULT_MIN_SUM_FOR_CUTS;
        public int maxStoCuts = 10000;
        public CutCountComputer initCutCountComp = new CutCountComputer();
        public ProjectionsPrm projPrm = new ProjectionsPrm();
    }
    
    public static class CutCountComputer {
        public int getNumCuts(int numParams) {
            return (int)Math.pow(numParams, 2.0);
        }
    }

    public final static Logger log = Logger.getLogger(LpSumToOneBuilder.class);
    
    public static final double DEFAULT_MIN_SUM_FOR_CUTS = 1.01;
    
    private LpStoBuilderPrm prm;
    
    private IloCplex cplex;
    private IloLPMatrix lpMatrix;
    private IndexedCpt idm;
    private CptBounds bounds;
    private Projections projections;
    
    // TODO: this shouldn't be public, but it's a convenient hack for now.
    public IloNumVar[][] modelParamVars;
    private int numStoCons;


    public LpSumToOneBuilder(LpStoBuilderPrm prm) {
        this.prm = prm;
        this.projections = new Projections(prm.projPrm);
    }

    public void init(IloCplex cplex, IloLPMatrix lpMatrix, IndexedCpt idm, CptBounds bounds) throws IloException {
        // Store inputs.
        this.cplex = cplex;
        this.lpMatrix = lpMatrix;
        this.idm = idm;
        this.bounds = bounds;
        
        // Initialize the model parameter variables and constraints.
        this.numStoCons = 0;
        
        // TODO: these should be called here, but then we would break a bunch of the D-W unit tests.
        //createModelParamVars();
        //addModelParamConstraints();
    }
    
    public void createModelParamVars() throws IloException {
        int numConds = idm.getNumConds();

        // Create the model parameter variables, adding them to the objective
        modelParamVars = new IloNumVar[numConds][];
        for (int c = 0; c < numConds; c++) {
            modelParamVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m = 0; m < modelParamVars[c].length; m++) {
                modelParamVars[c][m] = cplex.numVar(bounds.getLb(Type.PARAM, c, m), bounds.getUb(Type.PARAM, c, m), idm.getName(c, m));
            }
        }
    }

    public void addModelParamConstraints() throws IloException {
        int numConds = idm.getNumConds();

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
        
        if (prm.maxSetSizeToConstrain > 1) {
            log.warn("Ignoring parameter maxSetSizeToConstrain. This feature is deprecated.");
            //        for (int setSize=2; setSize <= prm.maxSetSizeToConstrain; setSize++) {
            //            for (int c = 0; c < numConds; c++) {
            //                addSetContraints(setSize, c);
            //            }
            //        }
        }
    }
    
    private double[][][] getInitialPoints() throws IloException {
        int numConds = idm.getNumConds();
        double[][][] vectors = new double[numConds][][];

        for (int c = 0; c < numConds; c++) {
            int numParams = idm.getNumParams(c);
            // Create numParams^2 vectors
            int numVectors = prm.initCutCountComp.getNumCuts(numParams); 
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

    private int addSumToOneConstraint(int c, double[] point) throws IloException {
        double[] probs;
        probs = projections.getDefaultProjection(bounds, c, point);
        double[] logProbs = Vectors.getLog(probs);
        
        double vectorSum = 1.0;
        for (int m = 0; m < logProbs.length; m++) {
            if (probs[m] > 0.0) {
                // Otherwise we'd get a NaN
                vectorSum += (logProbs[m] - 1.0) * probs[m];
            }
        }

        IloLinearNumExpr vectorExpr = cplex.scalProd(probs, modelParamVars[c]);
        IloRange constraint = cplex.le(vectorExpr, vectorSum, String.format("maxVar(%d)-%d", c, numStoCons++));
        // TODO: double check that this doesn't slow us down (by growing the LP matrix)
        return lpMatrix.addRow(constraint);
    }
    
    // TODO: This seems like it should be something like \sum_{m=1}^M \theta_m \leq -M log M.
    // But the current setup of \sum_{m=1}^M \theta_m <= 1 doesn't make any sense, since theta_m is always negative.
    @Deprecated
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
                paramVars[i] = modelParamVars[c][m];
            }

            IloLinearNumExpr vectorExpr = cplex.scalProd(ones, paramVars);
            IloRange constraint = cplex.le(vectorExpr, 1.0, String.format("setCons(%d)-%d", c, counter++));
            lpMatrix.addRow(constraint);
        }
    }
    
    /**
     * @param newRows
     * @return The number of new constraints added.
     */
    public IntArrayList projectModelParamsAndAddCuts() throws UnknownObjectException, IloException {
        if (numStoCons >= prm.maxStoCuts) {
            return new IntArrayList();
        }
        
        // Add a cut for each distribution by projecting the model
        // parameters
        // back onto the simplex.
        double[][] params = new double[idm.getNumConds()][];
        for (int c = 0; c < idm.getNumConds(); c++) {
            // Here the params are log probs
            params[c] = cplex.getValues(modelParamVars[c]);
        }
        IntArrayList rows = new IntArrayList();
        for (int c = 0; c < idm.getNumConds(); c++) {
            Vectors.exp(params[c]);
            // Here the params are probs
            double sum = Vectors.sum(params[c]);
            if (sum > prm.minSumForCuts && numStoCons < prm.maxStoCuts) {
                log.debug(String.format("Adding cuts for c=%d with sum=%f", c, sum));
                rows.add(addSumToOneConstraint(c, params[c]));
            }
        }
        log.debug("STO cuts added: " + rows.size());
        return rows;
    }
    
    public double[][] extractRelaxedLogProbs() throws UnknownObjectException, IloException {
        double[][] optimalLogProbs = new double[idm.getNumConds()][];
        for (int c = 0; c < idm.getNumConds(); c++) {
            optimalLogProbs[c] = cplex.getValues(modelParamVars[c]);
        }
        return optimalLogProbs;
    }

    public void updateModelParamBounds(int c, int m, double newLb, double newUb) throws IloException {
        modelParamVars[c][m].setLB(newLb);
        modelParamVars[c][m].setUB(newUb);
    }

    public int getNumStoCons() {
        return numStoCons;
    }    
    
}

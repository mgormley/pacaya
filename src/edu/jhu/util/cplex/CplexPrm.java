package edu.jhu.hltcoe.util.cplex;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;

import java.io.FileNotFoundException;

public class CplexPrm {

    public enum SimplexAlgorithm { 
        AUTO(0), PRIMAL(1), DUAL(2), NETWORK(3), BARRIER(4), SIFTING(5), CONCURRENT(6);
        public int cplexId;
        private SimplexAlgorithm(int cplexId) {
            this.cplexId = cplexId;
        }
    }
    
    public double workMemMegs = 1024;
    public int numThreads = 1;
    public int maxSimplexIterations = 2100000000;
    public int simplexAlgorithm = IloCplex.Algorithm.Auto;   
    public double timeoutSeconds = 1e+75; //TODO: Invalid default.
    public int simplexDisplay = 1;
    public int barrierDisplay = 1;
    
    public CplexPrm() { }

    public CplexPrm(double workMemMegs, int numThreads, int maxSimplexIterations, double timeoutSeconds, int simplexAlgorithm) {
        this.workMemMegs = workMemMegs;
        this.numThreads = numThreads;
        this.maxSimplexIterations = maxSimplexIterations;
        this.timeoutSeconds = timeoutSeconds;
        this.simplexAlgorithm = simplexAlgorithm;
    }

    public IloCplex getIloCplexInstance() {
        try {
            IloCplex cplex = new IloCplex();
            setCplexParams(cplex);
            return cplex;
        } catch (IloException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void setCplexParams(IloCplex cplex) throws IloException, FileNotFoundException {
        // Specifies an upper limit on the amount of central memory, in
        // megabytes, that CPLEX is permitted to use for working memory
        // before swapping to disk files, compressing memory, or taking
        // other actions.
        // 
        // A user-written Java application and CPLEX internals use separate
        // memory heaps.
        //
        // Values: Any nonnegative number, in megabytes; default: 128.0
        cplex.setParam(DoubleParam.WorkMem, workMemMegs);
        // cplex.setParam(StringParam.WorkDir, tempDir.getAbsolutePath());

        cplex.setParam(IntParam.Threads, numThreads);

        // -1 = oportunistic, 0 = auto (default), 1 = deterministic
        // In this context, deterministic means that multiple runs with
        // the same model at the same parameter settings on the same
        // platform will reproduce the same solution path and results.
        cplex.setParam(IntParam.ParallelMode, 1);

        // From the CPLEX documentation: the Dual algorithm can take better
        // advantage of a previous basis
        // after adding new constraints.
        // http://ibm.co/GHorLT
        // However it may be that Primal can better take advantage of a feasible
        // basis after adding
        // new variables.
        cplex.setParam(IntParam.RootAlg, simplexAlgorithm);

        // Maximum number of simplex iterations.
        cplex.setParam(IntParam.ItLim, maxSimplexIterations);
        
        // Maximum amount of wall clock time, in seconds, to run.
        cplex.setParam(DoubleParam.TiLim, timeoutSeconds);
        
        //    For continuous models solved with simplex, setting 1 (one) will use
        //    the currently loaded basis. If a basis is available only for the
        //    original, unpresolved model, or if CPLEX has a start vector rather
        //    than a simplex basis, then the simplex algorithm will proceed on the
        //    unpresolved model. With setting 2, CPLEX will first perform presolve
        //    on the model and on the basis or start vector, and then proceed with
        //    optimization on the presolved problem.
        cplex.setParam(IntParam.AdvInd, 1);

        // Value 0, will display no iteration messages until solved.
        // Value 1, will display iteration information after each refactoring (default).
        // When set to 2, will display at each iteration.
        cplex.setParam(IntParam.SimDisplay, simplexDisplay);
        cplex.setParam(IntParam.BarDisplay, barrierDisplay);
        
        // Whether or not to presolve (default true).
        // cplex.setParam(BooleanParam.PreInd, false);

        // Parameter for increasing optimality tolerance:
        //cplex.setParam(DoubleParam.EpOpt, 1e-06);
        // Parameter for increasing feasibility tolerance:
        // cplex.setParam(DoubleParam.EpRHS, 1e-06);
        // Sets the tolerance on complementarity for convergence. The barrier algorithm terminates with an optimal solution if the relative complementarity is smaller than this value.
        // cplex.setParam(DoubleParam.BarEpComp, 1e-08);
        
        // Numerical precision emphasis (default false)
        // cplex.setParam(BooleanParam.NumericalEmphasis, true);
        
        // OutputStream out = new BufferedOutputStream(new FileOutputStream(new
        // File(tempDir, "cplex.log")));
        // cplex.setOut(out);
        // cplex.setWarning(out);
    }

    public static void updateTimeoutSeconds(IloCplex cplex, double timeoutSeconds) {
        try {
            timeoutSeconds = Math.max(0.1, timeoutSeconds);
            cplex.setParam(DoubleParam.TiLim, timeoutSeconds);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

}

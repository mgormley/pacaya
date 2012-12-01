package edu.jhu.hltcoe.lp;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;

import java.io.FileNotFoundException;

public class CplexPrm {

    public double workMemMegs = 1024;
    public int numThreads = 1;
    public int maxSimplexIterations = 2100000000;
        
    public CplexPrm() { }

    public CplexPrm(double workMemMegs, int numThreads, int maxSimplexIterations) {
        this.workMemMegs = workMemMegs;
        this.numThreads = numThreads;
        this.maxSimplexIterations = maxSimplexIterations;
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
        // the
        // same model at the same parameter settings on the same
        // platform
        // will reproduce the same solution path and results.
        cplex.setParam(IntParam.ParallelMode, 1);

        // From the CPLEX documentation: the Dual algorithm can take better
        // advantage of a previous basis
        // after adding new constraints.
        // http://ibm.co/GHorLT
        // However it may be that Primal can better take advantage of a feasible
        // basis after adding
        // new variables.
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        // Note: we'd like to reuse basis information by explicitly storing it
        // with the Fork nodes as in SCIP. However, this is only possible if the
        // number of rows/columns in the problem remains the same, which it will
        // not for our master problem.
        // http://ibm.co/GCQ709
        // By default, the solver will make use of basis information internally
        // even when we update the problem. This is (hopefully) good enough.

        // TODO: For v12.3 only: cplex.setParam(IntParam.CloneLog, 1);

        cplex.setParam(IntParam.ItLim, maxSimplexIterations);

        // For continuous models solved with simplex, setting 1 (one) will use
        // the
        // currently loaded basis. If a basis is available only for the
        // original, unpresolved
        // model, or if CPLEX has a start vector rather than a simplex basis,
        // then the
        // simplex algorithm will proceed on the unpresolved model. With setting
        // 2,
        // CPLEX will first perform presolve on the model and on the basis or
        // start vector,
        // and then proceed with optimization on the presolved problem.
        cplex.setParam(IntParam.AdvInd, 1);

        // Whether or not to presolve.
        // cplex.setParam(BooleanParam.PreInd, false);

        // OutputStream out = new BufferedOutputStream(new FileOutputStream(new
        // File(tempDir, "cplex.log")));
        // cplex.setOut(out);
        // cplex.setWarning(out);
    }

}

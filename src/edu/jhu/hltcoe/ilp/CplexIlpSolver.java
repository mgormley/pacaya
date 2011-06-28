package edu.jhu.hltcoe.ilp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.StringParam;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.parse.IlpViterbiParser;

public class CplexIlpSolver implements IlpSolver {
    
    private static Logger log = Logger.getLogger(CplexIlpSolver.class);

    private File tempDir;
    private int numThreads;
    private double workMemMegs;
    
    public CplexIlpSolver(File tempDir, int numThreads, double workMemMegs) {
        this.tempDir = tempDir;
        this.numThreads = numThreads;
        this.workMemMegs = workMemMegs;
    }

    private Map<String,Double> result;

    @Override
    public Map<String, Double> getResult() {
        return result;
    }

    @Override
    public void solve(File lpFile) {
        if (!lpFile.getPath().endsWith(".lp")) {
            throw new IllegalStateException("Expecting lpFile to end with .lp: " + lpFile.getPath());
        }
        
        result = new HashMap<String,Double>();

        try {
            IloCplex cplex = new IloCplex();
            cplex.importModel(lpFile.getAbsolutePath());
            File ordFile = new File(lpFile.getAbsolutePath().replace(".lp", ".ord"));
            if (ordFile.exists()) {
                log.debug("Reading ORD file: " + ordFile.getPath());
                cplex.readOrder(ordFile.getAbsolutePath());
            }
            File mstFile = new File(lpFile.getAbsolutePath().replace(".lp", ".mst"));
            if (mstFile.exists()) {
                log.debug("Reading MST file: " + mstFile.getPath());
                cplex.readMIPStart(mstFile.getAbsolutePath());
            }
            
            // Specifies an upper limit on the amount of central memory, in
            // megabytes, that CPLEX is permitted to use for working memory
            // before swapping to disk files, compressing memory, or taking
            // other actions.
            // Values: Any nonnegative number, in megabytes; default: 128.0
            cplex.setParam(DoubleParam.WorkMem, workMemMegs);
            cplex.setParam(StringParam.WorkDir, tempDir.getAbsolutePath());
            cplex.setParam(DoubleParam.TreLim, 32000.0);

            cplex.setParam(IntParam.Threads, numThreads);

            // -1 = oportunistic, 0 = auto (default), 1 = deterministic
            // In this context, deterministic means that multiple runs with the
            // same model at the same parameter settings on the same platform
            // will reproduce the same solution path and results.
            cplex.setParam(IntParam.ParallelMode, 1);
            
            // TODO: this was chosen arbitrarily to try to fix the problem with priority order
            cplex.setParam(IntParam.MIPOrdType, 1);
                                    
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, "cplex.log")));
            cplex.setOut(out);
            cplex.setWarning(out);

            if (cplex.solve()) {
                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value = " + cplex.getObjValue());
                
                // The use of importModel guarantees exactly one LP matrix object.
                IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
                IloNumVar[] vars = lp.getNumVars();
                double[] vals = cplex.getValues(lp);
                
                assert(vars.length == vals.length);
                for (int i=0; i<vars.length; i++) {
                    //System.out.println(vars[i].getName() + " " + vals[i]);
                    result.put(vars[i].getName(), vals[i]);
                }
                
                cplex.writeSolution(lpFile.getAbsolutePath().replace(".lp", ".sol"));
            }
            cplex.end();
            out.close();

        } catch (IloException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

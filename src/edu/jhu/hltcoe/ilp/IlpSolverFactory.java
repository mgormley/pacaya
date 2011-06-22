package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IlpSolverFactory {
    
    private IlpSolverId ilpSolver;
    private int numThreads;
    private double workMemMegs;

    public IlpSolverFactory(IlpSolverId ilpSolver, int numThreads, double workMemMegs) {
        this.ilpSolver = ilpSolver;
        this.numThreads = numThreads;
        this.workMemMegs = workMemMegs;
    }

    public IlpSolver getInstance(File tempDir) {
        if (ilpSolver == IlpSolverId.CPLEX) {
            return new CplexIlpSolver(tempDir, numThreads, workMemMegs);
        } else if (ilpSolver == IlpSolverId.GUROBI_CL) {
            // TODO: use workMemMegs
            return new ClGurobiIlpSolver(tempDir, numThreads);
        } else if (ilpSolver == IlpSolverId.GUROBI_JAVA) {
            // TODO: use numThreads, workMemMegs
            return new JavaGurobiIlpSolver(tempDir);
        } else {
            throw new RuntimeException("unsupported IlpSolverId: " + ilpSolver);
        }
    }

    public enum IlpSolverId {
        CPLEX("cplex"),
        GUROBI_CL("gurobi-cl"),
        GUROBI_JAVA("gurobi-java");
        
        private String id;
        
        private IlpSolverId(String id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return id;
        }
        
        public static IlpSolverId getById(String id) {
            for (IlpSolverId f : values()) {
                if (f.id.equals(id)) {
                    return f;
                }
            }
            throw new IllegalArgumentException("Unrecognized IlpSolverId id: " + id);
        }
        
        public static List<String> getIdList() {
            List<String> idList = new ArrayList<String>();
            for (IlpSolverId f : values()) {
                idList.add(f.id);
            }
            return idList;
        }
    }

}

package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.hltcoe.ilp.decomp.BlockFileWriter;
import edu.jhu.hltcoe.ilp.decomp.DipMilpBlockSolver;
import edu.jhu.hltcoe.ilp.decomp.DipParseSolver;

public class IlpSolverFactory {
    
    private IlpSolverId ilpSolver;
    private int numThreads;
    private double workMemMegs;
    private BlockFileWriter blockFileWriter;

    public IlpSolverFactory(IlpSolverId ilpSolver, int numThreads, double workMemMegs) {
        this.ilpSolver = ilpSolver;
        this.numThreads = numThreads;
        this.workMemMegs = workMemMegs;
    }

    public IlpSolver getInstance(File tempDir) {
        if (ilpSolver == IlpSolverId.CPLEX) {
            return new CplexIlpSolver(tempDir, numThreads, workMemMegs);
        } else if (ilpSolver == IlpSolverId.GUROBI_CL) {
            return new ClGurobiIlpSolver(tempDir, numThreads, workMemMegs);
        } else if (ilpSolver == IlpSolverId.GUROBI_JAVA) {
            // TODO: use numThreads, workMemMegs
            return new JavaGurobiIlpSolver(tempDir);
        } else if (ilpSolver == IlpSolverId.DIP_MILPBLOCK_CPM) {
            if (blockFileWriter == null) {
                throw new IllegalStateException("Block file writer must be set");
            }
            // TODO: use numThreads, workMemMegs
            return new DipMilpBlockSolver(tempDir, numThreads, workMemMegs, blockFileWriter, 1, 0);                
        } else if (ilpSolver == IlpSolverId.DIP_MILPBLOCK_PC) {
            if (blockFileWriter == null) {
                throw new IllegalStateException("Block file writer must be set");
            }
            // TODO: use numThreads, workMemMegs
            return new DipMilpBlockSolver(tempDir, numThreads, workMemMegs, blockFileWriter, 0, 1);          
        } else if (ilpSolver == IlpSolverId.DIP_PARSE_PC) {
            if (blockFileWriter == null) {
                throw new IllegalStateException("Block file writer must be set");
            }
            // TODO: use numThreads, workMemMegs
            return new DipParseSolver(tempDir, numThreads, workMemMegs, blockFileWriter, 0, 1);     
        } else {
            throw new RuntimeException("unsupported IlpSolverId: " + ilpSolver);
        }
    }

    public void setBlockFileWriter(BlockFileWriter blockFileWriter) {
        this.blockFileWriter = blockFileWriter;
    }
    
    public enum IlpSolverId {
        CPLEX("cplex"),
        GUROBI_CL("gurobi-cl"),
        GUROBI_JAVA("gurobi-java"),
        DIP_MILPBLOCK_CPM("dip-milpblock-cpm"),
        DIP_MILPBLOCK_PC("dip-milpblock-pc"),
        DIP_PARSE_PC("dip-parse-pc");
        
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

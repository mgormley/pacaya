package edu.jhu.ilp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.ilp.decomp.AbstractDipSolver;
import edu.jhu.ilp.decomp.DipMilpBlockSolver;

public class ZimplSolver  {
    
    private File tempDir;
    private Map<String,Double> result;
    private IlpSolver ilpSolver;

    private double objective;

    private MstFileUpdater mstFileUpdater;

    public ZimplSolver(File tempDir, IlpSolver ilpSolver) {
        this.tempDir = tempDir;
        this.ilpSolver = ilpSolver;
        mstFileUpdater = new DefaultMstFileUpdater();
    }

    public void solve(File zimplFile) {
        result = new HashMap<String,Double>();
        
        // Run Zimpl
        ZimplRunner zimplRunner = new ZimplRunner(zimplFile, tempDir, ilpSolver.getType());
        zimplRunner.runZimpl();
        if (mstFileUpdater != null) {
            mstFileUpdater.updateMstFile(zimplRunner);
        }
        File ilpFile = zimplRunner.getIlpFile();
        File tblFile = zimplRunner.getTblFile();
        
        // TODO: this is a hack
        if (ilpSolver instanceof AbstractDipSolver) {
            ((AbstractDipSolver)ilpSolver).setTblFile(tblFile);
        }
        
        // Run ILP Solver
        if (!ilpSolver.solve(ilpFile)) {
            throw new RuntimeException("no solution found");
        }
        objective = ilpSolver.getObjective();
        Map<String,Double> solMap = ilpSolver.getResult();

        // Read tbl file and map variable values to original names
        try {
            Map<String,String> tblMap = ZimplRunner.readTblMapToZimpl(tblFile);
            
            for (Entry<String, Double> entry : solMap.entrySet()) {
                String gurobiVar = entry.getKey();
                Double value = entry.getValue();
                
                // Change negative zero to zero just for canonicalization
                value = value == -0.0 ? 0.0 : value;
                
                String zimplVar = tblMap.get(gurobiVar);
                // TODO: speedup: filter which vars are included in the result with an optional regex 
                result.put(zimplVar, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String,Double> getResult() {
        return result;
    }
    
    public double getObjective() {
        return objective;
    }
    
    public void setMstFileUpdater(MstFileUpdater mstFileUpdater) {
        this.mstFileUpdater  = mstFileUpdater;
    }

}

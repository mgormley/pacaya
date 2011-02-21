package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class JavaGurobiIlpSolver implements IlpSolver {

    private File tempDir;
    private Map<String, Double> result;

    public JavaGurobiIlpSolver(File tempDir) {
        this.tempDir = tempDir;
    }

    @Override
    public Map<String, Double> getResult() {
        return result;
    }

    @Override
    public void solve(File lpFile) {
        result = new HashMap<String, Double>();
        
        // Run Gurobi
        File gurobiLog = new File(tempDir, "gurobi.log");
        
        try {
            GRBEnv env = new GRBEnv(gurobiLog.getAbsolutePath());
            GRBModel  model = new GRBModel(env, lpFile.getAbsolutePath());
            model.optimize();
            for (GRBVar var : model.getVars()) {
                String gurobiVar = var.get(GRB.StringAttr.VarName);
                double value = var.get(GRB.DoubleAttr.X);
                result.put(gurobiVar, value);
            }
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
        
    }
    
}

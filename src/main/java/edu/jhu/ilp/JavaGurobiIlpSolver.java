package edu.jhu.ilp;

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
    private double objective;

    public JavaGurobiIlpSolver(File tempDir) {
        this.tempDir = tempDir;
    }

    @Override
    /**
     * TODO: currently returns only optimal solution, but should also return feasible solutions.
     */
    public boolean solve(File lpFile) {
        result = new HashMap<String, Double>();
        
        // Run Gurobi
        File gurobiLog = new File(tempDir, "gurobi.log");
        
        try {
            GRBEnv env = new GRBEnv(gurobiLog.getAbsolutePath());
            GRBModel  model = new GRBModel(env, lpFile.getAbsolutePath());
            model.optimize();
            if (model.get(GRB.IntAttr.Status) != GRB.OPTIMAL) {
                return false;
            }
            objective = model.get(GRB.DoubleAttr.ObjVal);
            for (GRBVar var : model.getVars()) {
                String gurobiVar = var.get(GRB.StringAttr.VarName);
                double value = var.get(GRB.DoubleAttr.X);
                result.put(gurobiVar, value);
            }
            return true;
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
        
    }

    @Override
    public Map<String, Double> getResult() {
        return result;
    }

    @Override
    public double getObjective() {
        return objective;
    }

    public String getType() {
        return "lp";
    }
        
}

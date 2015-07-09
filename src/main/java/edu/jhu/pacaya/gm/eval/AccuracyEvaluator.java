package edu.jhu.pacaya.gm.eval;

import java.util.List;

import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;

public class AccuracyEvaluator {

    /** Computes the accuracy on the PREDICTED variables. */
    public double evaluate(List<VarConfig> goldConfigs, List<VarConfig> predictedConfigs) {
        int numCorrect = 0;
        int numTotal = 0;
        
        assert (goldConfigs.size() == predictedConfigs.size());
        for (int i=0; i<goldConfigs.size(); i++) {
            VarConfig gold = goldConfigs.get(i);
            VarConfig pred = predictedConfigs.get(i);
            //assert (gold.getVars().equals(pred.getVars()));
            for (Var v : gold.getVars()) {
                if (v.getType() == VarType.PREDICTED) {
                    int goldState = gold.getState(v);
                    int predState = pred.getState(v);
                    if (goldState == predState) {
                        numCorrect++;
                    }
                    numTotal++;
                }
            }
        }
        
        return (double) numCorrect / numTotal;        
    }
    
}

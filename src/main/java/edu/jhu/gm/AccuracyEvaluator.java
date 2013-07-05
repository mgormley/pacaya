package edu.jhu.gm;

import java.util.List;

public class AccuracyEvaluator {

    public double evaluate(List<VarConfig> goldConfigs, List<VarConfig> predictedConfigs) {
        int numCorrect = 0;
        int numTotal = 0;
        
        assert (goldConfigs.size() == predictedConfigs.size());
        for (int i=0; i<goldConfigs.size(); i++) {
            VarConfig gold = goldConfigs.get(i);
            VarConfig pred = predictedConfigs.get(i);
            assert (gold.getVars().equals(pred.getVars()));
            for (Var v : gold.getVars()) {
                int goldState = gold.getState(v);
                int predState = pred.getState(v);
                if (goldState == predState) {
                    numCorrect++;
                }
                numTotal++;
            }
        }
        
        return (double) numCorrect / numTotal;        
    }
    
}

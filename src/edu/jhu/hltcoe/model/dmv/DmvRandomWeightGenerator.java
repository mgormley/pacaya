/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.model.dmv.DmvModel.ChooseRhs;
import edu.jhu.hltcoe.model.dmv.DmvModel.StopRhs;
import edu.jhu.hltcoe.util.Prng;

public class DmvRandomWeightGenerator implements DmvWeightGenerator {

    private double lambda;

    public DmvRandomWeightGenerator(double lambda) {
        this.lambda = lambda;
    }
    
    @Override
    public double getStopWeight(StopRhs triple) {
        double stop = 0.0;
        while (stop == 0.0) {
            stop = Prng.nextDouble();
        }
        return stop;
    }
    
    @Override
    public LabeledMultinomial<Label> getChooseMulti(ChooseRhs pair, List<Label> children) {
        // TODO: these should be randomly generated from a prior
        double[] chooseMulti = Multinomials.randomMultinomial(children.size());
        for (int i=0; i<chooseMulti.length; i++) {
            chooseMulti[i] += lambda;
        }
        Multinomials.normalizeProps(chooseMulti);
        
        return new LabeledMultinomial<Label>(children, chooseMulti);
    }
    
}
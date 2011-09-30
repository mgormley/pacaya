/**
 * 
 */
package edu.jhu.hltcoe.model;

import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Triple;

public class DmvRandomWeightGenerator implements DmvWeightGenerator {

    private double lambda;

    public DmvRandomWeightGenerator(double lambda) {
        this.lambda = lambda;
    }
    
    @Override
    public double getStopWeight(Triple<Label, String, Boolean> triple) {
        double stop = 0.0;
        while (stop == 0.0) {
            stop = Prng.random.nextDouble();
        }
        return stop;
    }
    
    @Override
    public LabeledMultinomial<Label> getChooseMulti(Pair<Label, String> pair, List<Label> children) {
        // TODO: these should be randomly generated from a prior
        double[] chooseMulti = Multinomials.randomMultinomial(children.size());
        for (int i=0; i<chooseMulti.length; i++) {
            chooseMulti[i] += lambda;
        }
        Multinomials.normalizeProps(chooseMulti);
        
        return new LabeledMultinomial<Label>(children, chooseMulti);
    }
    
}
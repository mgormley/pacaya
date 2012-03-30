/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import java.util.List;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class SmoothedDmvWeightCopier implements DmvWeightGenerator {

    private DmvModel dmv;
    private double epsilon;
    
    /**
     * Using epsilon we can smooth the parameters slightly, but 
     * note this is only to avoid -inf, it does not do add-lambda smoothing.
     */
    public SmoothedDmvWeightCopier(DmvModel dmv, double epsilon) {
        this.dmv = dmv;
        this.epsilon = epsilon;
    }

    @Override
    public LabeledMultinomial<Label> getChooseMulti(Pair<Label,String> pair, List<Label> children) {
        LabeledMultinomial<Label> mult = dmv.getChooseWeights(pair);
        double[] probs = new double[children.size()];
        for (int i=0; i<probs.length; i++) {
            probs[i] = mult.get(children.get(i)) + epsilon;
        }
        Multinomials.normalizeProps(probs);
        return new LabeledMultinomial<Label>(children, probs);
    }

    @Override
    public double getStopWeight(Triple<Label, String, Boolean> triple) {
        return (dmv.getStopWeights().get(triple) + epsilon) / (1.0 + 2*epsilon);
    }
    
}
/**
 * 
 */
package edu.jhu.hltcoe.parse;

import java.util.List;
import java.util.Map;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.model.DmvModelFactory.WeightGenerator;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

class WeightCopier implements WeightGenerator {

    private DmvModel dmv;
    
    public WeightCopier(DmvModel dmv) {
        this.dmv = dmv;
    }

    @Override
    public double[] getChooseMulti(Pair<Label,String> pair, List<Label> children) {
        Map<Triple<Label,String,Label>, Double> cw_map = dmv.getChooseWeights();
        double[] mult = new double[children.size()];
        
        Label parent = pair.get1();
        String lr = pair.get2();
        for (int i=0; i<mult.length; i++) {
            mult[i] = cw_map.get(new Triple<Label,String,Label>(parent, lr, children.get(i)));
        }
        // Do NOT normalize the multinomial
        return mult;
    }

    @Override
    public double getStopWeight(Triple<Label, String, Boolean> triple) {
        return dmv.getStopWeights().get(triple);
    }
    
}
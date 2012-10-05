/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Utilities;

public class RandomDmvModelFactory extends AbstractDmvModelFactory implements DmvModelFactory {

    private double lambda;
    
    public RandomDmvModelFactory(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public DmvModel getInstance(Alphabet<Label> alphabet) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.setRandom();
        dmv.backoff(Utilities.log(lambda));
        dmv.logNormalize();
        return dmv;
    }
    
}
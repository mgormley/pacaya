/**
 * 
 */
package edu.jhu.model.dmv;

import edu.jhu.util.Alphabet;
import edu.jhu.data.Label;
import edu.jhu.util.Utilities;

public class RandomDmvModelFactory extends AbstractDmvModelFactory implements DmvModelFactory {

    private double lambda;
    
    public RandomDmvModelFactory(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public DmvModel getInstance(Alphabet<Label> alphabet) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.setRandom();
        dmv.logNormalize();
        dmv.logAddConstant(Utilities.log(lambda));
        dmv.logNormalize();
        return dmv;
    }
    
}
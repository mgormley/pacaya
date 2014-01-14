/**
 * 
 */
package edu.jhu.induce.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Alphabet;

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
        dmv.logAddConstant(FastMath.log(lambda));
        dmv.logNormalize();
        return dmv;
    }
    
}
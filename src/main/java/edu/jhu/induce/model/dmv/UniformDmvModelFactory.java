/**
 * 
 */
package edu.jhu.induce.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;

public class UniformDmvModelFactory extends AbstractDmvModelFactory implements DmvModelFactory {

    public UniformDmvModelFactory() { }

    @Override
    public DmvModel getInstance(Alphabet<Label> alphabet) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.fill(0.0);
        dmv.logNormalize();
        return dmv;
    }
    
}
/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;

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
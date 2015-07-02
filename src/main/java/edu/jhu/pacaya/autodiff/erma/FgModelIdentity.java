package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class FgModelIdentity extends Identity<MVecFgModel> {

    public FgModelIdentity(FgModel model) {
        super(new MVecFgModel(model));
        assert this.y.getAlgebra() == RealAlgebra.getInstance();
    }
    
}

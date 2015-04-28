package edu.jhu.pacaya.gm.model.globalfac;

import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.IFgModel;
import edu.jhu.pacaya.gm.model.VarTensor;

/**
 * A constraint global factor.
 * 
 * Unlike a full global factor, this does not have any parameters or features.
 * 
 * @author mgormley
 */
public abstract class AbstractConstraintFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
    private int id = -1;
    
    public AbstractConstraintFactor() {  }

    public void updateFromModel(FgModel model) {
        // Currently, global factors do not support features, and
        // therefore have no model parameters.
    }

    public void addExpectedFeatureCounts(IFgModel counts, VarTensor factorMarginal, double multiplier) {
        // No op since this type of factor doesn't have any features.
    }

    public void addExpectedFeatureCounts(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        // No op since this type of factor doesn't have any features.
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
    
}

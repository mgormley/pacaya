package edu.jhu.gm.feat;

import edu.jhu.gm.model.ObsFeatureCarrier;
import edu.jhu.gm.model.TemplateFactor;
import edu.jhu.gm.model.VarSet;

public class ObsFeExpFamFactor extends ObsCjExpFamFactor implements ObsFeatureCarrier, TemplateFactor {
    
    private static final long serialVersionUID = 1L;
    private ObsFeatureExtractor obsFe;
    
    public ObsFeExpFamFactor(VarSet vars, Object templateKey, ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {
        super(vars, templateKey, ofc);
        this.obsFe = obsFe;
    }
    
    @Override
    public FeatureVector getObsFeatures() {
        return this.obsFe.calcObsFeatureVector(this);
    }
    
}
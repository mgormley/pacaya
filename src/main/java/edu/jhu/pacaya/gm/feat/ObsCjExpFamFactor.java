package edu.jhu.pacaya.gm.feat;

import edu.jhu.pacaya.gm.model.ExpFamFactor;
import edu.jhu.pacaya.gm.model.ObsFeatureCarrier;
import edu.jhu.pacaya.gm.model.TemplateFactor;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.prim.util.Lambda.FnIntDoubleToVoid;

/**
 * An exponential family factor which takes an ObsFeatureExtractor at
 * construction time and uses it to extract "observation features" which are
 * subsequently conjoined with an indicator on the predicted variables to
 * form the final features.
 * 
 * This class requires access to the internally stored indexing of
 * ObsFeatureConjoiner.
 * 
 * @author mgormley
 */
public abstract class ObsCjExpFamFactor extends ExpFamFactor implements ObsFeatureCarrier, TemplateFactor {

    private static final long serialVersionUID = 1L;
    private ObsFeatureConjoiner ofc;

    // The unique key identifying the template for this factor.
    protected Object templateKey;
    // The ID of the template for this factor -- which is only ever set by the
    // FeatureTemplateList.
    private int templateId = -1;
    
    public ObsCjExpFamFactor(VarSet vars, Object templateKey, ObsFeatureConjoiner ofc) {
        super(vars);
        this.ofc = ofc;
        this.templateKey = templateKey;
        // TODO: while it's appealing to do this here, it may inadvertantly add FactorTemplates for 
        // factors with non-lat-pred factor graphs.
        // setTemplateId(ofc.getTemplates().getTemplateId(this));
    }
    
    public ObsCjExpFamFactor(VarTensor other, Object templateKey, ObsFeatureConjoiner ofc) {
        super(other);
        this.ofc = ofc;
        this.templateKey = templateKey;
    }

    @Override
    public abstract FeatureVector getObsFeatures();
    
    @Override
    public FeatureVector getFeatures(final int config) {
        if (!ofc.isInitialized()) {
            throw new IllegalStateException("ObsFeatureConjoiner not initialized");
        }
        ObsCjExpFamFactor factor = this;
        final int ft = factor.getTemplateId();
        FeatureVector obsFv = ((ObsFeatureCarrier) factor).getObsFeatures();
        final FeatureVector fv = new FeatureVector(obsFv.getUsed());
        obsFv.iterate(new FnIntDoubleToVoid() {            
            @Override
            public void call(int feat, double val) {
                if (ofc.isIncluded(ft, config, feat)) {
                    fv.add(ofc.getFeatIndex(ft, config, feat), val);
                }
            }
        });
        return fv;
    }

    @Override
    public Object getTemplateKey() {
        return templateKey;
    }
    
    @Override
    public int getTemplateId() {
        return templateId;
    }
    
    @Override
    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }
    
}
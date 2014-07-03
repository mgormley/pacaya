package edu.jhu.srl;

import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.model.FgModel;
import edu.jhu.srl.JointNlpFgExamplesBuilder.JointNlpFeatureExtractorPrm;

public class JointNlpFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;
    private JointNlpFeatureExtractorPrm fePrm;
    private ObsFeatureConjoiner ofc;
    
    public JointNlpFgModel(CorpusStatistics cs, ObsFeatureConjoiner ofc, JointNlpFeatureExtractorPrm fePrm) {
        super(ofc.getNumParams(), ofc.getParamNames());
        this.cs = cs;
        this.ofc = ofc;
        this.fePrm = fePrm;
    }

    public CorpusStatistics getCs() {
        return cs;
    }

    public JointNlpFeatureExtractorPrm getFePrm() {
        return fePrm;
    }
    
    public ObsFeatureConjoiner getOfc() {
        return ofc;
    }
    
}

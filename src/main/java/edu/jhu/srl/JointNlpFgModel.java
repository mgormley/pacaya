package edu.jhu.srl;

import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.model.FgModel;
import edu.jhu.srl.JointNlpFeatureExtractor.JointNlpFeatureExtractorPrm;

public class JointNlpFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;
    private JointNlpFeatureExtractorPrm srlFePrm;
    private ObsFeatureConjoiner ofc;
    
    public JointNlpFgModel(CorpusStatistics cs, ObsFeatureConjoiner ofc, JointNlpFeatureExtractorPrm srlFePrm) {
        super(ofc.getNumParams());
        this.cs = cs;
        this.ofc = ofc;
        this.srlFePrm = srlFePrm;
    }

    public CorpusStatistics getCs() {
        return cs;
    }

    public JointNlpFeatureExtractorPrm getSrlFePrm() {
        return srlFePrm;
    }
    
    public ObsFeatureConjoiner getOfc() {
        return ofc;
    }
    
}

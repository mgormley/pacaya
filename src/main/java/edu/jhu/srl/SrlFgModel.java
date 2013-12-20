package edu.jhu.srl;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.model.FgModel;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;

public class SrlFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;
    private SrlFeatureExtractorPrm srlFePrm;
    
    public SrlFgModel(FgExampleList data, boolean includeUnsupportedFeatures, CorpusStatistics cs, SrlFeatureExtractorPrm srlFePrm) {
        super(data, includeUnsupportedFeatures);
        this.cs = cs;
        this.srlFePrm = srlFePrm;
    }
    
    public SrlFgModel(FactorTemplateList fts, CorpusStatistics cs) {
        super(fts);
        this.cs = cs;
    }

    public CorpusStatistics getCs() {
        return cs;
    }

    public SrlFeatureExtractorPrm getSrlFePrm() {
        return srlFePrm;
    }
    
}

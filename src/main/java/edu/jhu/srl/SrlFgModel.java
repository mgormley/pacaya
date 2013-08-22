package edu.jhu.srl;

import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;

public class SrlFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;
    
    public SrlFgModel(FgExamples data, boolean includeUnsupportedFeatures, CorpusStatistics cs) {
        super(data, includeUnsupportedFeatures);
        this.cs = cs;
    }
    
    public SrlFgModel(FeatureTemplateList fts, CorpusStatistics cs) {
        super(fts);
        this.cs = cs;
    }

    public CorpusStatistics getCs() {
        return cs;
    }
    
}

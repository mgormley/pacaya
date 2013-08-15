package edu.jhu.srl;

import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgModel;

public class SrlFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;

    public SrlFgModel(FeatureTemplateList fts, boolean includeUnsupportedFeatures, CorpusStatistics cs) {
        super(fts, includeUnsupportedFeatures);
        this.cs = cs;
    }

    public CorpusStatistics getCs() {
        return cs;
    }
    
}

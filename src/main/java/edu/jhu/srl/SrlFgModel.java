package edu.jhu.srl;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.model.FgModel;

public class SrlFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;
    
    public SrlFgModel(FgExampleList data, boolean includeUnsupportedFeatures, CorpusStatistics cs) {
        super(data, includeUnsupportedFeatures);
        this.cs = cs;
    }
    
    public SrlFgModel(FactorTemplateList fts, CorpusStatistics cs) {
        super(fts);
        this.cs = cs;
    }

    public CorpusStatistics getCs() {
        return cs;
    }
    
}

package edu.jhu.srl;

import java.util.List;

import edu.jhu.gm.Feature;
import edu.jhu.gm.FgModel;
import edu.jhu.util.Alphabet;

public class SrlFgModel extends FgModel {

    private static final long serialVersionUID = 5827437917567173421L;
    private CorpusStatistics cs;

    public SrlFgModel(Alphabet<Feature> alphabet, CorpusStatistics cs) {
        super(alphabet);
        this.cs = cs;
    }

    public SrlFgModel(List<Feature> feats, CorpusStatistics cs) {
        super(feats);
        this.cs = cs;
    }
    
    public CorpusStatistics getCs() {
        return cs;
    }
    
}

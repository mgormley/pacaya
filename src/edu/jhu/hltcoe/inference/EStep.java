package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.SentenceCollection;

public interface EStep<M,C> {

    C getCounts(SentenceCollection sentences, M model);
    
}

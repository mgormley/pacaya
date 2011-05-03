package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;

public interface EStep<C> {

    C getCounts(SentenceCollection sentences, Model model);
    
}

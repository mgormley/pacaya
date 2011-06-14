package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;

public interface ViterbiParser {

    DepTreebank getViterbiParse(SentenceCollection sentences, Model model);
    
}

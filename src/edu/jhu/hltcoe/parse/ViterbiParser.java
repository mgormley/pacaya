package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public interface ViterbiParser {

    DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model model);
    
    DepTreebank getViterbiParse(SentenceCollection sentences, Model model);
    
    double getLastParseWeight();
    
}

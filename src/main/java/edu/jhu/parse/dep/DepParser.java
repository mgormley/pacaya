package edu.jhu.parse.dep;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.SentenceCollection;
import edu.jhu.model.Model;
import edu.jhu.train.dmv.DmvTrainCorpus;

public interface DepParser {

    DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model model);
    
    DepTreebank getViterbiParse(SentenceCollection sentences, Model model);
    
    double getLastParseWeight();
    
}

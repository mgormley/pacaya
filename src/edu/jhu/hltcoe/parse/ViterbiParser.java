package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;

public interface ViterbiParser {

    DepTreebank getViterbiParse(SentenceCollection sentences, Model model);
    
    DepTree getViterbiParse(Sentence sentence, Model model);

}

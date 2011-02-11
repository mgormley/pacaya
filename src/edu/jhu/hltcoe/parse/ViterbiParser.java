package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;

public interface ViterbiParser {

    DepTree getViterbiParse(Sentence sentence);

}

package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.stanford.nlp.parser.ViterbiParser;

public interface Trainer {

    void train(SentenceCollection sentences);

}

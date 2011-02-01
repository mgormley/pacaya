package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.stanford.nlp.parser.ViterbiParser;

public interface Trainer extends ViterbiParser {

    void train(SentenceCollection sentences);

}

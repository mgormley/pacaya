package edu.jhu.hltcoe;

import edu.stanford.nlp.parser.ViterbiParser;
import edu.stanford.nlp.trees.Treebank;

public interface Model extends ViterbiParser {

    void train(Treebank treebank);

}

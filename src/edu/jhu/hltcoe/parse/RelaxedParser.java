package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public interface RelaxedParser {

    RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model model);

    double getLastParseWeight();
}

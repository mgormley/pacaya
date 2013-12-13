package edu.jhu.parse.relax;

import edu.jhu.globalopt.dmv.RelaxedDepTreebank;
import edu.jhu.model.Model;
import edu.jhu.train.dmv.DmvTrainCorpus;

public interface RelaxedDepParser {

    RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model model);

    double getLastParseWeight();

    void reset();
}

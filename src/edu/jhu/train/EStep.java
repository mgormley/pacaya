package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.util.Pair;

public interface EStep<C> {

    Pair<C,Double> getCountsAndLogLikelihood(TrainCorpus corpus, Model model, int iteration);
    
}

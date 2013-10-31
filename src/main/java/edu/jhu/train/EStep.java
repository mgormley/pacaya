package edu.jhu.train;

import edu.jhu.model.Model;
import edu.jhu.util.tuple.Pair;

public interface EStep<C> {

    Pair<C,Double> getCountsAndLogLikelihood(TrainCorpus corpus, Model model, int iteration);
    
}

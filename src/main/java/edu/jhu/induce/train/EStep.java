package edu.jhu.induce.train;

import edu.jhu.induce.model.Model;
import edu.jhu.prim.tuple.Pair;

public interface EStep<C> {

    Pair<C,Double> getCountsAndLogLikelihood(SemiSupervisedCorpus corpus, Model model, int iteration);
    
}

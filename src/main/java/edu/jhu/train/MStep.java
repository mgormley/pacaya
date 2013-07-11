package edu.jhu.train;

import edu.jhu.model.Model;

public interface MStep<C> {

    Model getModel(TrainCorpus corpus, C counts, Model oldModel);

}

package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.model.Model;

public interface MStep<C> {

    Model getModel(TrainCorpus corpus, C counts);

}

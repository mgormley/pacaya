package edu.jhu.induce.train;

import edu.jhu.induce.model.Model;

public interface MStep<C> {

    Model getModel(SemiSupervisedCorpus corpus, C counts, Model oldModel);

}

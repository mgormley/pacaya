package edu.jhu.model;

import edu.jhu.train.SemiSupervisedCorpus;

public interface ModelFactory {

    Model getInstance(SemiSupervisedCorpus corpus);

}

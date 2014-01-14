package edu.jhu.induce.model;

import edu.jhu.induce.train.SemiSupervisedCorpus;

public interface ModelFactory {

    Model getInstance(SemiSupervisedCorpus corpus);

}

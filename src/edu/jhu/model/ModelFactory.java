package edu.jhu.model;

import edu.jhu.train.TrainCorpus;

public interface ModelFactory {

    Model getInstance(TrainCorpus corpus);

}

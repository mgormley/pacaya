package edu.jhu.hltcoe.model;

import edu.jhu.hltcoe.train.TrainCorpus;

public interface ModelFactory {

    Model getInstance(TrainCorpus corpus);

}

package edu.jhu.hltcoe.model.dmv;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.train.TrainCorpus;

public interface DmvModelFactory extends ModelFactory {

    Model getInstance(TrainCorpus corpus);
    DmvModel getInstance(Alphabet<Label> alphabet);

}

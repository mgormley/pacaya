package edu.jhu.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.model.Model;
import edu.jhu.model.ModelFactory;
import edu.jhu.train.TrainCorpus;
import edu.jhu.util.Alphabet;

public interface DmvModelFactory extends ModelFactory {

    Model getInstance(TrainCorpus corpus);
    DmvModel getInstance(Alphabet<Label> alphabet);

}

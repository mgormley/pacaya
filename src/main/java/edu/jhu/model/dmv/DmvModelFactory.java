package edu.jhu.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.model.Model;
import edu.jhu.model.ModelFactory;
import edu.jhu.train.SemiSupervisedCorpus;
import edu.jhu.util.Alphabet;

public interface DmvModelFactory extends ModelFactory {

    Model getInstance(SemiSupervisedCorpus corpus);
    DmvModel getInstance(Alphabet<Label> alphabet);

}

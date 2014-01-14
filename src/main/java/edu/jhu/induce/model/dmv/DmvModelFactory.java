package edu.jhu.induce.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.model.ModelFactory;
import edu.jhu.induce.train.SemiSupervisedCorpus;
import edu.jhu.util.Alphabet;

public interface DmvModelFactory extends ModelFactory {

    Model getInstance(SemiSupervisedCorpus corpus);
    DmvModel getInstance(Alphabet<Label> alphabet);

}

package edu.jhu.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.model.Model;
import edu.jhu.train.SemiSupervisedCorpus;
import edu.jhu.util.Alphabet;

public abstract class AbstractDmvModelFactory implements DmvModelFactory {

    public AbstractDmvModelFactory() {
        super();
    }

    @Override
    public Model getInstance(SemiSupervisedCorpus corpus) {
        return getInstance(corpus.getLabelAlphabet());
    }
    
    public abstract DmvModel getInstance(Alphabet<Label> alphabet);
    
}
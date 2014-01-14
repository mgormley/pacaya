package edu.jhu.induce.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.train.SemiSupervisedCorpus;
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
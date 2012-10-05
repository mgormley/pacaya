package edu.jhu.hltcoe.model.dmv;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.TrainCorpus;

public abstract class AbstractDmvModelFactory implements DmvModelFactory {

    public AbstractDmvModelFactory() {
        super();
    }

    @Override
    public Model getInstance(TrainCorpus corpus) {
        return getInstance(corpus.getLabelAlphabet());
    }
    
    public abstract DmvModel getInstance(Alphabet<Label> alphabet);
    
}
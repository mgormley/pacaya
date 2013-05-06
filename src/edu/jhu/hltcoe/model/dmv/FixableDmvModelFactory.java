package edu.jhu.hltcoe.model.dmv;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.TrainCorpus;
import edu.jhu.hltcoe.util.Alphabet;

public class FixableDmvModelFactory implements DmvModelFactory {

    private DmvModelFactory modelFactory;
    private DmvModel model;
    
    public FixableDmvModelFactory(DmvModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    public DmvModel getInstance(Alphabet<Label> alphabet) {
        if (model == null) {
            return modelFactory.getInstance(alphabet);
        } else {
            return model;
        }
    }
    
    public void fixModel(DmvModel model) {
        this.model = model;
    }

    @Override
    public Model getInstance(TrainCorpus corpus) {
        return getInstance(corpus.getLabelAlphabet());
    }

}

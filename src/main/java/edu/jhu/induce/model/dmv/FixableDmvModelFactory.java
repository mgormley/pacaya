package edu.jhu.induce.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.train.SemiSupervisedCorpus;
import edu.jhu.util.Alphabet;

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
    public Model getInstance(SemiSupervisedCorpus corpus) {
        return getInstance(corpus.getLabelAlphabet());
    }

}

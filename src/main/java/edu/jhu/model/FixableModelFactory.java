package edu.jhu.model;

import edu.jhu.train.SemiSupervisedCorpus;

public class FixableModelFactory implements ModelFactory {

    private ModelFactory modelFactory;
    private Model model;
    
    public FixableModelFactory(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Override
    public Model getInstance(SemiSupervisedCorpus corpus) {
        if (model == null) {
            return modelFactory.getInstance(corpus);
        } else {
            return model;
        }
    }
    
    public void fixModel(Model model) {
        this.model = model;
    }

}

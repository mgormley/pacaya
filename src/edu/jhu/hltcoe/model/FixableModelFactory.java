package edu.jhu.hltcoe.model;

import edu.jhu.hltcoe.data.SentenceCollection;

public class FixableModelFactory implements ModelFactory {

    private ModelFactory modelFactory;
    private Model model;
    
    public FixableModelFactory(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Override
    public Model getInstance(SentenceCollection sentences) {
        if (model == null) {
            return modelFactory.getInstance(sentences);
        } else {
            return model;
        }
    }
    
    public void fixModel(Model model) {
        this.model = model;
    }

}

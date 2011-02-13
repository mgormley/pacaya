package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;

public class ViterbiTrainer implements Trainer {

    private ViterbiParser parser;
    private EMTrainer<DepTreebank> emTrainer;
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations) {
        this.parser = parser;
        emTrainer = new EMTrainer<DepTreebank>(new ViterbiEStep(), mStep, modelFactory, iterations);
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        emTrainer.train(sentences);
    }

    private class ViterbiEStep implements EStep<DepTreebank> {

        @Override
        public DepTreebank getCounts(SentenceCollection sentences, Model model) {
            DepTreebank treebank = new DepTreebank();
            for (Sentence sentence: sentences) {
                DepTree tree = parser.getViterbiParse(sentence, model);
                treebank.add(tree);
            }
            return treebank;
        }
        
    }
    
}

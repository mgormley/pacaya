package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;

public class ViterbiTrainer<M extends Model> implements Trainer {

    private ViterbiParser parser;
    private EMTrainer<M,DepTreebank> emTrainer;
    
    public ViterbiTrainer(ViterbiParser parser, MStep<M,DepTreebank> mStep, ModelFactory<M> modelFactory, int iterations) {
        this.parser = parser;
        emTrainer = new EMTrainer<M,DepTreebank>(new ViterbiEStep(), mStep, modelFactory, iterations);
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        emTrainer.train(sentences);
    }

    private class ViterbiEStep implements EStep<M,DepTreebank> {

        @Override
        public DepTreebank getCounts(SentenceCollection sentences, M model) {
            DepTreebank treebank = new DepTreebank();
            for (Sentence sentence: sentences) {
                DepTree tree = parser.getViterbiParse(sentence);
                treebank.add(tree);
            }
            return treebank;
        }
        
    }
    
}

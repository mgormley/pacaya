package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Pair;

public class ViterbiTrainer implements Trainer {

    private static Logger log = Logger.getLogger(ViterbiTrainer.class);
    private ViterbiParser parser;
    private EMTrainer<DepTreebank> emTrainer;
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations, double convergenceRatio) {
        this.parser = parser;
        emTrainer = new EMTrainer<DepTreebank>(new ViterbiEStep(), mStep, modelFactory, iterations, convergenceRatio);
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        emTrainer.train(sentences);
    }

    private class ViterbiEStep implements EStep<DepTreebank> {

        @Override
        public Pair<DepTreebank,Double> getCountsAndLogLikelihood(SentenceCollection sentences, Model model) {
            DepTreebank depTreebank = parser.getViterbiParse(sentences, model);
            return new Pair<DepTreebank,Double>(depTreebank, parser.getLastParseWeight());
        }
        
    }
    
    @Override
    public Model getModel() {
        return emTrainer.getModel();
    }
    
    public ViterbiParser getParser() {
        return parser;
    }
    
    public int getIterationsCompleted() {
        return emTrainer.getIterationsCompleted();
    }
    
}

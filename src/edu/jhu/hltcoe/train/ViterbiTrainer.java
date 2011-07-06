package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;

public class ViterbiTrainer implements Trainer {

    private static Logger log = Logger.getLogger(ViterbiTrainer.class);
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
            DepTreebank depTreebank = parser.getViterbiParse(sentences, model);
            log.info("iteration = " + emTrainer.getCurrentIteration());
            log.info("logLikelihood = " + parser.getLastParseWeight());
            return depTreebank;
        }
        
    }
    
    @Override
    public Model getModel() {
        return emTrainer.getModel();
    }
    
    public ViterbiParser getParser() {
        return parser;
    }
    
}

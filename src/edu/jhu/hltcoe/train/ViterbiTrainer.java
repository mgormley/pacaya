package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Pair;

public class ViterbiTrainer extends EMTrainer<DepTreebank> implements Trainer {

    private static Logger log = Logger.getLogger(ViterbiTrainer.class);
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations, double convergenceRatio) {
        this(new ViterbiEStep(parser), mStep, modelFactory, iterations, convergenceRatio);
    }
    
    public ViterbiTrainer(EStep<DepTreebank> eStep, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations, double convergenceRatio) {
        super(eStep, mStep, modelFactory, iterations, convergenceRatio);
    }

    private static class ViterbiEStep implements EStep<DepTreebank> {

        private ViterbiParser parser;

        
        public ViterbiEStep(ViterbiParser parser) {
            this.parser = parser;
        }

        @Override
        public Pair<DepTreebank,Double> getCountsAndLogLikelihood(SentenceCollection sentences, Model model) {
            DepTreebank depTreebank = parser.getViterbiParse(sentences, model);
            return new Pair<DepTreebank,Double>(depTreebank, parser.getLastParseWeight());
        }
        
    }
    
}

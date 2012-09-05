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
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, 
            int iterations, double convergenceRatio) {
        this(parser, mStep, modelFactory, iterations, convergenceRatio, 0, Double.POSITIVE_INFINITY);
    }
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, 
            int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds) {
        this(new ViterbiEStep(parser), mStep, modelFactory, iterations, convergenceRatio, numRestarts, timeoutSeconds);
    }
    
    protected ViterbiTrainer(EStep<DepTreebank> eStep, MStep<DepTreebank> mStep, ModelFactory modelFactory, 
            int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds) {
        super(eStep, mStep, modelFactory, iterations, convergenceRatio, numRestarts, timeoutSeconds);
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

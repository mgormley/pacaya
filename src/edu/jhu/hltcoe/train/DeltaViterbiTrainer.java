package edu.jhu.hltcoe.train;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.FixableModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Pair;

public class DeltaViterbiTrainer extends EMTrainer<DepTreebank> implements Trainer {

    private static Logger log = Logger.getLogger(DeltaViterbiTrainer.class);
    
    public DeltaViterbiTrainer(ViterbiParser deltaParser, ViterbiParser fastParser, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations, double convergenceRatio) {
        super(new ViterbiEStepForDeltas(deltaParser, fastParser, mStep, modelFactory, iterations, convergenceRatio), 
                mStep, modelFactory, iterations, convergenceRatio, 0);
    }

    private static class ViterbiEStepForDeltas implements EStep<DepTreebank> {
        
        private ViterbiTrainer fastTrainer;
        private FixableModelFactory fixableModelFactory;
        private ViterbiParser deltaParser;
        
        public ViterbiEStepForDeltas(ViterbiParser deltaParser, ViterbiParser fastParser, MStep<DepTreebank> mStep, ModelFactory modelFactory, int iterations, double convergenceRatio) {
            this.fixableModelFactory = new FixableModelFactory(modelFactory);
            this.fastTrainer = new ViterbiTrainer(fastParser, mStep, fixableModelFactory, Integer.MAX_VALUE, convergenceRatio);
            this.deltaParser = deltaParser;
            
            Logger ftLogger = Logger.getLogger(ViterbiTrainer.class.getName() + "(fastTrainer)");
            ftLogger.setLevel(Level.INFO);
            fastTrainer.setLogger(ftLogger);
        }
        
        @Override
        public Pair<DepTreebank,Double> getCountsAndLogLikelihood(SentenceCollection sentences, Model model) {
            fixableModelFactory.fixModel(model);
            fastTrainer.train(sentences);
            DepTreebank depTreebank = deltaParser.getViterbiParse(sentences, fastTrainer.getModel());
            log.info("logLikelihood (delta) = " + deltaParser.getLastParseWeight());
            
            // TODO: It's not clear that just returning the parse is what we want to do. The Viterbi
            // iteration often falls back down in likelihood afterwards.
            return new Pair<DepTreebank,Double>(depTreebank, deltaParser.getLastParseWeight());
        }
        
    }
    
    
}

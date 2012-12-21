package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Pair;

public class ViterbiTrainer extends EMTrainer<DepTreebank> implements Trainer<DepTreebank> {

    public static class ViterbiTrainerPrm {
        public double lambda = 0.1;
        public DependencyParserEvaluator evaluator = null;
        public EMTrainerPrm emPrm = new EMTrainerPrm();
        public ViterbiTrainerPrm() { }
        public ViterbiTrainerPrm(int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds, 
                double lambda, DependencyParserEvaluator evaluator) {
            
        }
    }
        
    private static final Logger log = Logger.getLogger(ViterbiTrainer.class);
    private ViterbiTrainerPrm prm;
    
    public ViterbiTrainer(ViterbiTrainerPrm prm, ViterbiParser parser, ModelFactory modelFactory) { 
        super(prm.emPrm, new ViterbiEStep(parser), new DmvMStep(prm.lambda), modelFactory);
        this.prm = prm;
    }

    @Override
    protected void evalIncumbent(Model bestModel, DepTreebank bestParses, double bestLogLikelihood) {
        if (prm.evaluator != null) {
            log.info("Incumbent logLikelihood: " + bestLogLikelihood);
            log.info("Incumbent accuracy: " + prm.evaluator.evaluate(bestParses));
        }
    }

    private static class ViterbiEStep implements EStep<DepTreebank> {

        private ViterbiParser parser;

        public ViterbiEStep(ViterbiParser parser) {
            this.parser = parser;
        }

        @Override
        public Pair<DepTreebank,Double> getCountsAndLogLikelihood(TrainCorpus corpus, Model model) {
            DepTreebank depTreebank = parser.getViterbiParse((DmvTrainCorpus)corpus, model);
            return new Pair<DepTreebank,Double>(depTreebank, parser.getLastParseWeight());
        }
        
    }
    
}

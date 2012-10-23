package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Pair;

public class ViterbiTrainer extends EMTrainer<DepTreebank> implements Trainer<DepTreebank> {

    private static final Logger log = Logger.getLogger(ViterbiTrainer.class);
    private DependencyParserEvaluator evaluator;
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, 
            int iterations, double convergenceRatio) {
        this(parser, mStep, modelFactory, iterations, convergenceRatio, 0, Double.POSITIVE_INFINITY, null);
    }
    
    public ViterbiTrainer(ViterbiParser parser, MStep<DepTreebank> mStep, ModelFactory modelFactory, 
            int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds, DependencyParserEvaluator evaluator) {
        this(new ViterbiEStep(parser), mStep, modelFactory, iterations, convergenceRatio, numRestarts, timeoutSeconds, evaluator);
    }
    
    protected ViterbiTrainer(EStep<DepTreebank> eStep, MStep<DepTreebank> mStep, ModelFactory modelFactory, 
            int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds, DependencyParserEvaluator evaluator) {
        super(eStep, mStep, modelFactory, iterations, convergenceRatio, numRestarts, timeoutSeconds);
        this.evaluator = evaluator;
    }

    @Override
    protected void evalIncumbent(Model bestModel, DepTreebank bestParses, double bestLogLikelihood) {
        if (evaluator != null) {
            log.info("Incumbent logLikelihood: " + bestLogLikelihood);
            log.info("Incumbent accuracy: " + evaluator.evaluate(bestParses));
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

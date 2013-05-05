package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.UniformDmvModelFactory;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Pair;

public class DmvViterbiEMTrainer extends EMTrainer<DepTreebank> implements Trainer<DepTreebank> {

    public static class DmvViterbiEMTrainerPrm {
        public double lambda = 0.1;
        public DependencyParserEvaluator evaluator = null;
        public EMTrainerPrm emPrm = new EMTrainerPrm();
        public ViterbiParser parser = null; // = new DmvCkyParser();
        public ModelFactory modelFactory = null; // new UniformDmvModelFactory();
        public DmvViterbiEMTrainerPrm() { }
        public DmvViterbiEMTrainerPrm(int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds, 
                double lambda, DependencyParserEvaluator evaluator, ViterbiParser parser, DmvModelFactory modelFactory) {
            this.lambda = lambda;
            this.evaluator = evaluator;
            this.parser = parser;
            this.modelFactory = modelFactory;
            this.emPrm.iterations = iterations;
            this.emPrm.convergenceRatio = convergenceRatio;
            this.emPrm.numRestarts = numRestarts;
            this.emPrm.timeoutSeconds = timeoutSeconds;
        }
        /** Copy constructor */
        public DmvViterbiEMTrainerPrm(DmvViterbiEMTrainerPrm other) {
            this.lambda = other.lambda;
            this.evaluator = other.evaluator;
            this.parser = other.parser;
            this.modelFactory = other.modelFactory;
            this.emPrm = new EMTrainerPrm(other.emPrm);
        }
    }
        
    private static final Logger log = Logger.getLogger(DmvViterbiEMTrainer.class);
    private DmvViterbiEMTrainerPrm prm;
    
    public DmvViterbiEMTrainer(DmvViterbiEMTrainerPrm prm) {
        super(prm.emPrm, new ViterbiEStep(prm.parser), new DmvMStep(prm.lambda), prm.modelFactory);
        if (prm.parser == null || prm.modelFactory == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }
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

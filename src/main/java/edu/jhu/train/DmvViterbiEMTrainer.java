package edu.jhu.train;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.eval.DependencyParserEvaluator;
import edu.jhu.model.Model;
import edu.jhu.model.ModelFactory;
import edu.jhu.model.dmv.DmvMStep;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.UniformDmvModelFactory;
import edu.jhu.parse.DepParser;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.parse.relax.RelaxedParserWrapper;
import edu.jhu.prim.tuple.Pair;

public class DmvViterbiEMTrainer extends EMTrainer<DepTreebank> implements Trainer<DepTreebank> {

    public static class DmvViterbiEMTrainerPrm {
        public double lambda = 0.1;
        public DependencyParserEvaluator evaluator = null;
        public EMTrainerPrm emPrm = new EMTrainerPrm();
        public DepParser parser = new DmvCkyParser();
        public ModelFactory modelFactory = new UniformDmvModelFactory();
        public DmvViterbiEMTrainerPrm() { }
        public DmvViterbiEMTrainerPrm(int iterations, double convergenceRatio, int numRestarts, double timeoutSeconds, 
                double lambda, DependencyParserEvaluator evaluator, DepParser parser, DmvModelFactory modelFactory) {
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

        private DepParser parser;

        public ViterbiEStep(DepParser parser) {
            this.parser = parser;
        }

        @Override
        public Pair<DepTreebank,Double> getCountsAndLogLikelihood(TrainCorpus corpus, Model model, int iteration) {
            //TODO: remove this hacky reset function: 
            //if (iteration == 1 && parser instanceof RelaxedParserWrapper) {
                //((RelaxedParserWrapper)parser).reset();
            //}
            DepTreebank depTreebank = parser.getViterbiParse((DmvTrainCorpus)corpus, model);
            return new Pair<DepTreebank,Double>(depTreebank, parser.getLastParseWeight());
        }
        
    }
    
}

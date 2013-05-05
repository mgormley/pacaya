package edu.jhu.hltcoe.train;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.model.FixableModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;
import edu.jhu.hltcoe.util.Pair;

public class DeltaViterbiTrainer extends EMTrainer<DepTreebank> implements Trainer<DepTreebank> {

    private static final Logger log = Logger.getLogger(DeltaViterbiTrainer.class);

    public DeltaViterbiTrainer(ViterbiParser deltaParser, ViterbiParser fastParser, ModelFactory modelFactory,
            int iterations, double convergenceRatio, double lambda) {
        super(new EMTrainerPrm(iterations, convergenceRatio, 0, Double.POSITIVE_INFINITY), new ViterbiEStepForDeltas(
                deltaParser, fastParser, modelFactory, iterations, convergenceRatio), new DmvMStep(lambda),
                modelFactory);
    }

    private static class ViterbiEStepForDeltas implements EStep<DepTreebank> {

        private DmvViterbiEMTrainer fastTrainer;
        private FixableModelFactory fixableModelFactory;
        private ViterbiParser deltaParser;

        public ViterbiEStepForDeltas(ViterbiParser deltaParser, ViterbiParser fastParser, ModelFactory modelFactory,
                int iterations, double convergenceRatio) {
            this.fixableModelFactory = new FixableModelFactory(modelFactory);
            DmvViterbiEMTrainerPrm prm = new DmvViterbiEMTrainerPrm();
            prm.parser = fastParser;
            prm.modelFactory = fixableModelFactory;
            prm.emPrm.iterations = Integer.MAX_VALUE;
            prm.emPrm.convergenceRatio = convergenceRatio;
            this.fastTrainer = new DmvViterbiEMTrainer(prm);
            this.deltaParser = deltaParser;

            Logger ftLogger = Logger.getLogger(DmvViterbiEMTrainer.class.getName() + "(fastTrainer)");
            ftLogger.setLevel(Level.INFO);
            fastTrainer.setLogger(ftLogger);
        }

        @Override
        public Pair<DepTreebank, Double> getCountsAndLogLikelihood(TrainCorpus c, Model model) {
            DmvTrainCorpus corpus = (DmvTrainCorpus) c;
            fixableModelFactory.fixModel(model);
            fastTrainer.train(corpus);
            DepTreebank depTreebank = deltaParser.getViterbiParse(corpus, fastTrainer.getModel());
            log.info("logLikelihood (delta) = " + deltaParser.getLastParseWeight());

            // TODO: It's not clear that just returning the parse is what we
            // want to do. The Viterbi
            // iteration often falls back down in likelihood afterwards.
            return new Pair<DepTreebank, Double>(depTreebank, deltaParser.getLastParseWeight());
        }

    }

}

package edu.jhu.train.dmv;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.model.FixableModelFactory;
import edu.jhu.model.Model;
import edu.jhu.model.ModelFactory;
import edu.jhu.model.dmv.DmvMStep;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.train.EMTrainer;
import edu.jhu.train.EStep;
import edu.jhu.train.SemiSupervisedCorpus;
import edu.jhu.train.Trainer;
import edu.jhu.train.EMTrainer.EMTrainerPrm;
import edu.jhu.train.dmv.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;

public class DeltaDmvViterbiEMTrainer extends EMTrainer<DepTreebank> implements Trainer<DepTreebank> {

    private static final Logger log = Logger.getLogger(DeltaDmvViterbiEMTrainer.class);

    public DeltaDmvViterbiEMTrainer(DepParser deltaParser, DepParser fastParser, ModelFactory modelFactory,
            int iterations, double convergenceRatio, double lambda) {
        super(new EMTrainerPrm(iterations, convergenceRatio, 0, Double.POSITIVE_INFINITY), new ViterbiEStepForDeltas(
                deltaParser, fastParser, modelFactory, iterations, convergenceRatio), new DmvMStep(lambda),
                modelFactory);
    }

    private static class ViterbiEStepForDeltas implements EStep<DepTreebank> {

        private DmvViterbiEMTrainer fastTrainer;
        private FixableModelFactory fixableModelFactory;
        private DepParser deltaParser;

        public ViterbiEStepForDeltas(DepParser deltaParser, DepParser fastParser, ModelFactory modelFactory,
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
        public Pair<DepTreebank, Double> getCountsAndLogLikelihood(SemiSupervisedCorpus c, Model model, int iteration) {
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

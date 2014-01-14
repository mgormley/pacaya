package edu.jhu.induce.train.dmv;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.induce.model.FixableModelFactory;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.model.ModelFactory;
import edu.jhu.induce.model.dmv.DmvMStep;
import edu.jhu.induce.train.EMTrainer;
import edu.jhu.induce.train.EStep;
import edu.jhu.induce.train.SemiSupervisedCorpus;
import edu.jhu.induce.train.Trainer;
import edu.jhu.induce.train.EMTrainer.EMTrainerPrm;
import edu.jhu.induce.train.dmv.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.prim.tuple.Pair;

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

package edu.jhu.parse.relax;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.SentenceCollection;
import edu.jhu.gridsearch.dmv.BasicDmvProjector;
import edu.jhu.gridsearch.dmv.BasicDmvProjector.DmvProjectorPrm;
import edu.jhu.gridsearch.dmv.DmvObjective;
import edu.jhu.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.model.Model;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.parse.DepParser;
import edu.jhu.parse.relax.LpDmvRelaxedParser.LpDmvRelaxedParserPrm;
import edu.jhu.train.DmvTrainCorpus;

public class RelaxedParserWrapper implements DepParser {
    
    public static class RelaxedDepParserWrapperPrm {
        // TODO: This should be a factory.
        // TODO: We should be careful that the objective and the parser match up.
        public RelaxedDepParser relaxedParser = new LpDmvRelaxedParser(new LpDmvRelaxedParserPrm());
        public DmvProjectorPrm projPrm = new DmvProjectorPrm();
        public DmvObjectivePrm objPrm = new DmvObjectivePrm();       
    }
    
    private RelaxedDepParserWrapperPrm prm;
    private BasicDmvProjector projector;
    private double lastParseWeight;
    private DmvObjective obj;
    
    public RelaxedParserWrapper(RelaxedDepParserWrapperPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public double getLastParseWeight() {
        return lastParseWeight;
    }

    @Override
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model genericModel) {
        return getViterbiParse(corpus, (DmvModel) genericModel);
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model genericModel) {
        return getViterbiParse(new DmvTrainCorpus(sentences), (DmvModel) genericModel);   
    }
    
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, DmvModel model) {
        // Lazily create the projector and objective.
        if (projector == null) {
            projector = new BasicDmvProjector(new DmvProjectorPrm(), corpus);
        }
        if (obj == null) {
            obj = new DmvObjective(prm.objPrm, new IndexedDmvModel(corpus));
        }

        // Solve the relaxation.
        RelaxedDepTreebank relaxTrees = prm.relaxedParser.getRelaxedParse(corpus, model);
        // Project onto the feasible region.
        DepTreebank projTrees = projector.getProjectedParses(relaxTrees);
        // Compute the parse score.
        lastParseWeight = obj.computeTrueObjective(model, projTrees);
        return projTrees;
    }

    public void reset() {
        prm.relaxedParser.reset();
    }

}

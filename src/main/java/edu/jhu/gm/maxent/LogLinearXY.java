package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureCache;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.StringIterable;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.maxent.LogLinearXYData.LogLinearExample;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.optimize.L2;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Alphabet;

/**
 * Log-linear model trainer and decoder.
 * 
 * @author mgormley
 */
public class LogLinearXY {

    private static final Logger log = Logger.getLogger(LogLinearXY.class);

    public static class LogLinearXYPrm {
        /** Variance of L2 regularizer or -1 for automatic. */
        public double l2Variance = -1;
    }
    
    private LogLinearXYPrm prm;
    
    public LogLinearXY(LogLinearXYPrm prm) {
        this.prm = prm;
    }
    
    private Alphabet<String> alphabet = null;
    private List<String> stateNames = null;
    
    /**
     * Trains a log-linear model.
     * 
     * @param data The log-linear model training examples created by
     *            LogLinearData.
     * @return Trained model.
     */
    public FgModel train(LogLinearXYData data) {
        return train(data.getAlphabet(), data.getData());
    }
    
    private FgModel train(Alphabet<String> alphabet, List<LogLinearExample> exList) {
        FgExampleList data = getData(alphabet, exList);
        log.info("Number of unweighted train instances: " + data.size());
        BeliefPropagationPrm bpPrm = getBpPrm();
        
        CrfTrainerPrm crfPrm = new CrfTrainerPrm();
        crfPrm.infFactory = bpPrm;        
        if (prm.l2Variance == -1) {
            crfPrm.regularizer = new L2(data.size());
        } else {
            crfPrm.regularizer = new L2(prm.l2Variance);
        }
        
        FgModel model = new FgModel(alphabet.size(), new StringIterable(alphabet.getObjects()));
        CrfTrainer trainer = new CrfTrainer(crfPrm);
        trainer.train(model, data);
        return model;
    }

    /**
     * Decodes a single example.
     * 
     * @param model The log-linear model.
     * @param ex The example to decode.
     * @return A pair containing the most likely label (i.e. value of y) and the
     *         distribution over y values.
     */
    public Pair<String, DenseFactor> decode(FgModel model, LogLinearExample llex) {
        FgExample ex = getFgExample(llex);
        
        MbrDecoderPrm prm = new MbrDecoderPrm();
        prm.infFactory = getBpPrm(); 
        MbrDecoder decoder = new MbrDecoder(prm);
        decoder.decode(model, ex);
        List<DenseFactor> marginals = decoder.getVarMarginals();
        VarConfig vc = decoder.getMbrVarConfig();
        String stateName = vc.getStateName(ex.getFgLatPred().getVar(0));
        if (marginals.size() != 1) {
            throw new IllegalStateException("Example is not from a LogLinearData factory");
        }
        return new Pair<String,DenseFactor>(stateName, marginals.get(0));
    }

    private FgExampleList getData(LogLinearXYData data) {
        return getData(data.getAlphabet(), data.getData());
    }
    
    private FgExampleList getData(Alphabet<String> alphabet, List<LogLinearExample> exList) {    
        if (this.alphabet == null) {
            this.alphabet = alphabet;
            this.stateNames = getStateNames(exList);
        }
        
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        for (final LogLinearExample desc : exList) {
            for (int i=0; i<desc.getWeight(); i++) {
                FgExample ex = getFgExample(desc);
                data.add(ex);
            }
        }
        
        return data;
    }

    private FgExample getFgExample(final LogLinearExample desc) {
        if (alphabet == null) {
            throw new IllegalStateException("decode can only be called after train");
        }
        
        Var v0 = getVar();
        final VarConfig trainConfig = new VarConfig();
        trainConfig.put(v0, desc.getY());
        
        FactorGraph fg = new FactorGraph();
        VarSet vars = new VarSet(v0);
        ExpFamFactor f0 = new ExpFamFactor(vars) {
            
            @Override
            public FeatureVector getFeatures(int config) {
                return desc.getFeatures(config);
            }
            
        };
        fg.addFactor(f0);
        return new FgExample(fg, trainConfig);
    }

    private Var getVar() {
        return new Var(VarType.PREDICTED, stateNames.size(), "v0", stateNames);
    }
    
    private static List<String> getStateNames(List<LogLinearExample> exList) {
        Set<String> names = new HashSet<String>();
        for (LogLinearExample desc : exList) {
            names.add(Integer.toString(desc.getY()));
        }
        return new ArrayList<String>(names);
    }
    
    private BeliefPropagationPrm getBpPrm() {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = true;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        return bpPrm;
    }
    
}

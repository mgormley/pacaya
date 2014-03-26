package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.StringIterable;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.maxent.LogLinearXYData.LogLinearExample;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.hlt.optimize.functions.L2;
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
        public CrfTrainerPrm crfPrm = new CrfTrainerPrm();
        public LogLinearXYPrm() {
            crfPrm.infFactory = new BruteForceInferencerPrm(true);
            //crfPrm.batchMaximizer = new SGD(new SGDPrm());
            //crfPrm.maximizer = null;
        }
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
        Alphabet<String> alphabet = data.getFeatAlphabet();
        FgExampleList list = getData(data);
        log.info("Number of unweighted train instances: " + list.size());
        
        if (prm.l2Variance == -1) {
            prm.crfPrm.regularizer = new L2(list.size());
        } else {
            prm.crfPrm.regularizer = new L2(prm.l2Variance);
        }
        log.info("Number of model parameters: " + alphabet.size());
        FgModel model = new FgModel(alphabet.size(), new StringIterable(alphabet.getObjects()));
        CrfTrainer trainer = new CrfTrainer(prm.crfPrm);
        trainer.train(model, list);
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

    /**
     * For testing only. Converts to the graphical model's representation of the data.
     */
    public FgExampleList getData(LogLinearXYData data) {
        Alphabet<String> alphabet = data.getFeatAlphabet();
        List<LogLinearExample> exList = data.getData();    
        if (this.alphabet == null) {
            this.alphabet = alphabet;
            this.stateNames = getStateNames(exList, data.getYAlphabet());
        }
        
        // Because we don't directly support weights in the CrfObjective, 
        // we instead just add multiple examples.
        FgExampleMemoryStore store = new FgExampleMemoryStore();
        for (final LogLinearExample desc : exList) {
            for (int i = 0; i < (desc.getWeight() * 100); i++) {
                FgExample ex = getFgExample(desc);
                store.add(ex);
            }
        }
        
        return store;
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
    
    private static List<String> getStateNames(List<LogLinearExample> exList, Alphabet<Object> yAlphabet) {
        StringIterable iter = new StringIterable(yAlphabet.getObjects());
        List<String> list = new ArrayList<String>();
        for (String s : iter) {
            list.add(s);
        }
        return list;
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

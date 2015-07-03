package edu.jhu.pacaya.gm.inf;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.AbstractFgInferencer;
import edu.jhu.pacaya.autodiff.erma.ErmaBp;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.files.Files;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.prim.util.Timer;

/**
 * Loopy belief propagation inference algorithm.
 * 
 * @author mgormley
 *
 */
public class BeliefPropagation extends ErmaBp {
    
    private static final long serialVersionUID = 1L;        
    private static final Logger log = LoggerFactory.getLogger(BeliefPropagation.class);

    public static class BeliefPropagationPrm extends ErmaBpPrm implements FgInferencerFactory {
        private static final long serialVersionUID = 1L;        
    }
    
    public enum BpScheduleType {
        /** Send messages from a root to the leaves and back. */
        TREE_LIKE,
        /** Send messages in a random order. */
        RANDOM,
        /**
         * FOR TESTING ONLY: Schedule with only edges, so that no global factor dynamic programming
         * algorithms are ever called.
         */
        NO_GLOBAL_FACTORS,
    }
    
    public enum BpUpdateOrder {
        /** Send each message in sequence according to the schedule. */ 
        SEQUENTIAL,
        /** Create all messages first. Then send them all at the same time. */
        PARALLEL
    };
    
    public BeliefPropagation(final FactorGraph fg, BeliefPropagationPrm prm) {
        super(fg, prm);
    }
    
}

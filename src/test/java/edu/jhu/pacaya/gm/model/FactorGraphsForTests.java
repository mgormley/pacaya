package edu.jhu.pacaya.gm.model;

import edu.jhu.pacaya.gm.feat.ObsFeExpFamFactor;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureExtractor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.collections.QLists;

// TODO: Move FactorGraphTest.* and CrfTrainerTest.* here.
public class FactorGraphsForTests {

    public static class FgAndVars {
        
        public FactorGraph fg;
        public VarConfig goldConfig;
        
        // Three words.
        public Var w0;
        public Var w1;
        public Var w2;
        
        // Three latent vars.
        public Var z0;
        public Var z1;
        public Var z2;
                
        // Three tags.
        public Var t0;
        public Var t1;
        public Var t2;
        
        // Emission factors. 
        public ExplicitFactor emit0;
        public ExplicitFactor emit1;
        public ExplicitFactor emit2;
        
        // Transition factors.
        public ExplicitFactor tran0;
        public ExplicitFactor tran1;
        
    }

    // TODO: Shouldn't this just call getLinearChainFgAndVars.
    /** Gets a simple linear chain CRF consisting of 3 words and 3 tags. */
    public static FactorGraph getLinearChainGraph() {
        FactorGraph fg = new FactorGraph();
    
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);
    
        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1)); 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2)); 
    
        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2)); 
        
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.4);
        tran0.setValue(2, 0.3);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.4);
        tran1.setValue(2, 1.3);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExplicitFactor)f).convertRealToLog();
        }
        
        return fg;
    }

    public static FgAndVars getLinearChainFgWithVars() {
    
        FactorGraph fg = new FactorGraph();
    
        // Create three words.
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", QLists.getList("man", "dog"));
        Var w1 = new Var(VarType.PREDICTED, 2, "w1", QLists.getList("run", "jump"));
        Var w2 = new Var(VarType.PREDICTED, 2, "w2", QLists.getList("fence", "bucket"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", QLists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", QLists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", QLists.getList("N", "V"));
    
        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0, w0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1, w1)); 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2, w2)); 
    
        // Implied: emit*.fill(0.0);
        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2)); 
    
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.4);
        tran0.setValue(2, 0.3);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.4);
        tran1.setValue(2, 1.3);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExplicitFactor)f).convertRealToLog();
        }
    
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
        fgv.emit0 = emit0;
        fgv.emit1 = emit1;
        fgv.emit2 = emit2;
        fgv.tran0 = tran0;
        fgv.tran1 = tran1;
        
        fgv.goldConfig = new VarConfig();
        fgv.goldConfig.put(fgv.w0, 0);
        fgv.goldConfig.put(fgv.w1, 0);
        fgv.goldConfig.put(fgv.w2, 0);
        fgv.goldConfig.put(fgv.t0, 0);
        fgv.goldConfig.put(fgv.t1, 1);
        fgv.goldConfig.put(fgv.t2, 1);
        
        return fgv;
    }

    public static FgAndVars getLinearChainFgWithVarsLatent() {
    
        FactorGraph fg = new FactorGraph();
    
        // Create three words.
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", QLists.getList("man", "dog"));
        Var w1 = new Var(VarType.PREDICTED, 2, "w1", QLists.getList("run", "jump"));
        Var w2 = new Var(VarType.PREDICTED, 2, "w2", QLists.getList("fence", "bucket"));
    
        // Create latent classes.
        Var z0 = new Var(VarType.LATENT, 2, "z0", QLists.getList("C1", "C2"));
        Var z1 = new Var(VarType.LATENT, 2, "z1", QLists.getList("C1", "C2"));
        Var z2 = new Var(VarType.LATENT, 2, "z2", QLists.getList("C1", "C2"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", QLists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", QLists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", QLists.getList("N", "V"));
    
        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(z0, w0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(z1, w1));
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(z2, w2));
    
        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Latent emission factors. 
        ExplicitFactor emitL0 = new ExplicitFactor(new VarSet(t0, z0));
        ExplicitFactor emitL1 = new ExplicitFactor(new VarSet(t1, z1));
        ExplicitFactor emitL2 = new ExplicitFactor(new VarSet(t2, z2));
        
        emitL0.setValue(0, 1.1);
        emitL0.setValue(1, 1.9);
        emitL0.setValue(2, 1.2);
        emitL0.setValue(3, 1.3);
        emitL1.setValue(0, 1.3);
        emitL1.setValue(1, 1.7);
        emitL1.setValue(2, 1.2);
        emitL1.setValue(3, 1.3);
        emitL2.setValue(0, 1.5);
        emitL2.setValue(1, 1.5);
        emitL2.setValue(2, 1.2);
        emitL2.setValue(3, 1.3);
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1));
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2));
    
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.4);
        tran0.setValue(2, 0.3);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.4);
        tran1.setValue(2, 1.3);
        tran1.setValue(3, 1.5);
        
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(emitL0);
        fg.addFactor(emitL1);
        fg.addFactor(emitL2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
    
        for (Factor f : fg.getFactors()) {
            ((ExplicitFactor)f).convertRealToLog();
        }
        
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.z0 = z0;
        fgv.z1 = z1;
        fgv.z2 = z2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
    
        fgv.goldConfig = new VarConfig();
        fgv.goldConfig.put(fgv.w0, 0);
        fgv.goldConfig.put(fgv.w1, 0);
        fgv.goldConfig.put(fgv.w2, 0);
        fgv.goldConfig.put(fgv.t0, 0);
        fgv.goldConfig.put(fgv.t1, 1);
        fgv.goldConfig.put(fgv.t2, 1);
        fgv.goldConfig.put(fgv.z0, 1);
        fgv.goldConfig.put(fgv.z1, 0);
        fgv.goldConfig.put(fgv.z2, 0);
        
        return fgv;
    }

    /**
     * This method differs from {@link #getLinearChainFgWithVars()}'s version in that it uses a feature extractor.
     */
    public static FgAndVars getLinearChainFgWithVars(ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {
    
        FactorGraph fg = new FactorGraph();
    
        // Create three words.
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", QLists.getList("man", "dog"));
        Var w1 = new Var(VarType.PREDICTED, 2, "w1", QLists.getList("run", "jump"));
        Var w2 = new Var(VarType.PREDICTED, 2, "w2", QLists.getList("fence", "bucket"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", QLists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", QLists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", QLists.getList("N", "V"));
    
        // Emission factors. 
        ObsFeExpFamFactor emit0 = new ObsFeExpFamFactor(new VarSet(t0, w0), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit1 = new ObsFeExpFamFactor(new VarSet(t1, w1), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit2 = new ObsFeExpFamFactor(new VarSet(t2, w2), "emit", ofc, obsFe); 
    
        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ObsFeExpFamFactor tran0 = new ObsFeExpFamFactor(new VarSet(t0, t1), "tran", ofc, obsFe); 
        ObsFeExpFamFactor tran1 = new ObsFeExpFamFactor(new VarSet(t1, t2), "tran", ofc, obsFe); 
        
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.4);
        tran0.setValue(2, 0.3);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.4);
        tran1.setValue(2, 1.3);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExpFamFactor)f).convertRealToLog();
        }
    
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
    
        fgv.goldConfig = new VarConfig();
        fgv.goldConfig.put(fgv.w0, 0);
        fgv.goldConfig.put(fgv.w1, 0);
        fgv.goldConfig.put(fgv.w2, 0);
        fgv.goldConfig.put(fgv.t0, 0);
        fgv.goldConfig.put(fgv.t1, 1);
        fgv.goldConfig.put(fgv.t2, 1);
        
        return fgv;
    }

    /**
     * This method differs from {@link #getLinearChainFgWithVarsLatent()}'s version in that it uses a feature extractor.
     */
    public static FgAndVars getLinearChainFgWithVarsLatent(ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {
    
        FactorGraph fg = new FactorGraph();
    
        // Create three words.
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", QLists.getList("man", "dog"));
        Var w1 = new Var(VarType.PREDICTED, 2, "w1", QLists.getList("run", "jump"));
        Var w2 = new Var(VarType.PREDICTED, 2, "w2", QLists.getList("fence", "bucket"));
    
        // Create latent classes.
        Var z0 = new Var(VarType.LATENT, 2, "z0", QLists.getList("C1", "C2"));
        Var z1 = new Var(VarType.LATENT, 2, "z1", QLists.getList("C1", "C2"));
        Var z2 = new Var(VarType.LATENT, 2, "z2", QLists.getList("C1", "C2"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", QLists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", QLists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", QLists.getList("N", "V"));
    
        // Emission factors. 
        ObsFeExpFamFactor emit0 = new ObsFeExpFamFactor(new VarSet(z0, w0), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit1 = new ObsFeExpFamFactor(new VarSet(z1, w1), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit2 = new ObsFeExpFamFactor(new VarSet(z2, w2), "emit", ofc, obsFe); 
    
        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Latent emission factors. 
        ObsFeExpFamFactor emitL0 = new ObsFeExpFamFactor(new VarSet(t0, z0), "latent-emit", ofc, obsFe); 
        ObsFeExpFamFactor emitL1 = new ObsFeExpFamFactor(new VarSet(t1, z1), "latent-emit", ofc, obsFe); 
        ObsFeExpFamFactor emitL2 = new ObsFeExpFamFactor(new VarSet(t2, z2), "latent-emit", ofc, obsFe); 
    
        emitL0.setValue(0, 1.1);
        emitL0.setValue(1, 1.9);
        emitL1.setValue(0, 1.3);
        emitL1.setValue(1, 1.7);
        emitL2.setValue(0, 1.5);
        emitL2.setValue(1, 1.5);
        
        // Transition factors.
        ObsFeExpFamFactor tran0 = new ObsFeExpFamFactor(new VarSet(t0, t1), "tran", ofc, obsFe); 
        ObsFeExpFamFactor tran1 = new ObsFeExpFamFactor(new VarSet(t1, t2), "tran", ofc, obsFe); 
    
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.4);
        tran0.setValue(2, 0.3);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.4);
        tran1.setValue(2, 1.3);
        tran1.setValue(3, 1.5);
        
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(emitL0);
        fg.addFactor(emitL1);
        fg.addFactor(emitL2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
    
        for (Factor f : fg.getFactors()) {
            ((ExpFamFactor)f).convertRealToLog();
        }
        
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.z0 = z0;
        fgv.z1 = z1;
        fgv.z2 = z2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
    
        fgv.goldConfig = new VarConfig();
        fgv.goldConfig.put(fgv.w0, 0);
        fgv.goldConfig.put(fgv.w1, 0);
        fgv.goldConfig.put(fgv.w2, 0);
        fgv.goldConfig.put(fgv.t0, 0);
        fgv.goldConfig.put(fgv.t1, 1);
        fgv.goldConfig.put(fgv.t2, 1);
        fgv.goldConfig.put(fgv.z0, 1);
        fgv.goldConfig.put(fgv.z1, 0);
        fgv.goldConfig.put(fgv.z2, 0);
        
        return fgv;
    }

    /** Extremely simple factor graph with just one variable. */
    public static FactorGraph getOneVarFg() {
        return getOneVarFgAndVars().fg;
    }
    
    public static FgAndVars getOneVarFgAndVars() {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
    
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
    
        emit0.setValue(0, 1.1);
        emit0.setValue(1, 1.9);
    
        fg.addFactor(emit0);
        
        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.emit0 = emit0;
        fgv.t0 = t0;
        
        fgv.goldConfig = new VarConfig();
        fgv.goldConfig.put(fgv.t0, 0);
        
        return fgv;
    }
    

    public static FgAndVars getOneVarFgAndVars(ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", QLists.getList("N", "V"));
        
        ObsFeExpFamFactor emit0 = new ObsFeExpFamFactor(new VarSet(t0), "emit", ofc, obsFe); 
    
        emit0.setValue(0, 1.1);
        emit0.setValue(1, 1.9);
    
        fg.addFactor(emit0);
        
        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.emit0 = emit0;
        fgv.t0 = t0;

        fgv.goldConfig = new VarConfig();
        fgv.goldConfig.put(fgv.t0, 0);
        
        return fgv;
    }

}

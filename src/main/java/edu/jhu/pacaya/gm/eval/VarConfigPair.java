package edu.jhu.pacaya.gm.eval;

import java.util.List;

import edu.jhu.pacaya.gm.model.VarConfig;

public class VarConfigPair {
    public List<VarConfig> gold;
    public List<VarConfig> pred;
    public VarConfigPair(List<VarConfig> gold, List<VarConfig> pred) {
        this.gold = gold;
        this.pred = pred;
    }        
}
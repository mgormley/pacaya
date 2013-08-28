package edu.jhu.gm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.util.Alphabet;

/**
 * A collection of samples for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public class FgExamples implements Iterable<FgExample> {
        
    private ArrayList<FgExample> examples;
    /**
     * This is a hack to carry around the source sentences. For example, the
     * CoNLL2009 sentences from which these examples were generated.
     */
    private Object sourceSents;
    private FeatureTemplateList fts;

    public FgExamples(FeatureTemplateList fts) {
        this.fts = fts;
        this.examples = new ArrayList<FgExample>();
    }
    
    /** Adds an example. */
    public void add(FgExample example) {
        examples.add(example);
    }
    
    /** Gets the i'th example. */
    public FgExample get(int i) {
        return examples.get(i);
    }
    
    /** Gets the number of examples. */
    public int size() {
        return examples.size();
    }

    @Override
    public Iterator<FgExample> iterator() {
        return examples.iterator();
    }

    public List<VarConfig> getGoldConfigs() {
        List<VarConfig> varConfigList = new ArrayList<VarConfig>();
        for (FgExample ex : examples) {
            varConfigList.add(ex.getGoldConfig());
        }
        return varConfigList;
    }

    public int getNumFactors() {
        int numFactors = 0;
        for (FgExample ex : examples) {
            numFactors += ex.getOriginalFactorGraph().getNumFactors();
        }
        return numFactors;
    }

    public int getNumVars() {
        int numVars = 0;
        for (FgExample ex : examples) {
            numVars += ex.getOriginalFactorGraph().getNumVars();
        }
        return numVars;
    }

    public void setSourceSentences(Object sents) {
        this.sourceSents = sents;
    }
    
    public Object getSourceSentences() {
        return sourceSents;
    }

    public FeatureTemplateList getTemplates() {
        return fts;
    }

    public double getTotMsFgClampTimer() {
        double tot = 0;
        for (int i=0; i<this.size(); i++) {
            tot += this.get(i).fgClampTimer.totMs();
        }
        return tot;
    }

    public double getTotMsFeatCacheTimer() {
        double tot = 0;
        for (int i=0; i<this.size(); i++) {
            tot += this.get(i).featCacheTimer.totMs();
        }
        return tot;
    }

}

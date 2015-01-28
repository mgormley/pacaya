package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.jhu.gm.app.Encoder;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Prm;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.collections.Lists;

public class RelationsEncoder implements Encoder<AnnoSentence, List<String>> {
    
    private RelationsFactorGraphBuilderPrm prm;
    private CorpusStatistics cs;
    private ObsFeatureConjoiner ofc;
    
    public RelationsEncoder(RelationsFactorGraphBuilderPrm prm, CorpusStatistics cs, ObsFeatureConjoiner ofc) {
        this.prm = prm;
        this.cs = cs;
        this.ofc = ofc;
    }
    
    @Override
    public LFgExample encode(AnnoSentence sent, List<String> rels) {
        return getExample(sent, rels, true);
    }

    @Override
    public UFgExample encode(AnnoSentence sent) {
        return getExample(sent, null, false);
    }

    private LFgExample getExample(AnnoSentence sent, List<String> rels, boolean labeledExample) {
        RelationsFactorGraphBuilder rfgb = new RelationsFactorGraphBuilder(prm);
        FactorGraph fg = new FactorGraph();
        ObsFeatureExtractor relFe = new RelObsFe(prm.fePrm, sent, ofc.getTemplates());
        relFe = new ObsFeatureCache(relFe);
        rfgb.build(sent, ofc, fg, cs, relFe);
        
        VarConfig vc = new VarConfig();
        if (rels != null) {
            addRelVarAssignments(sent, rels, rfgb, vc);
        }
        
        if (labeledExample) {
            return new LabeledFgExample(fg, vc);
        } else {
            return new UnlabeledFgExample(fg, vc);
        }
    }

    public static void addRelVarAssignments(AnnoSentence sent, List<String> rels, RelationsFactorGraphBuilder rfgb,
            VarConfig vc) {
        List<Pair<NerMention, NerMention>> nePairs = sent.getNePairs();
        for (RelVar var : rfgb.getRelVars()) {    	
    		NerMention ne1 = var.ment1;
    		NerMention ne2 = var.ment2;
            int k = nePairs.indexOf(new Pair<NerMention,NerMention>(ne1, ne2));
            String relation = rels.get(k);
            assert var != null;
            vc.put(var, relation);
    	}
    }
        
}

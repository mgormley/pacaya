package edu.jhu.nlp.relations;

import java.util.List;

import edu.jhu.data.NerMention;
import edu.jhu.data.NerMentions;
import edu.jhu.data.RelationMention;
import edu.jhu.data.RelationMentions;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.app.Encoder;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;

public class RelationsEncoder implements Encoder<AnnoSentence, RelationMentions> {

    private RelationsFactorGraphBuilderPrm prm;
    private CorpusStatistics cs;
    private ObsFeatureConjoiner ofc;
    
    public RelationsEncoder(RelationsFactorGraphBuilderPrm prm, CorpusStatistics cs, ObsFeatureConjoiner ofc) {
        this.prm = prm;
        this.cs = cs;
        this.ofc = ofc;
    }
    
    @Override
    public LFgExample encode(AnnoSentence sent, RelationMentions rels) {
        return getExample(sent, rels, true);
    }

    @Override
    public UFgExample encode(AnnoSentence sent) {
        return getExample(sent, null, false);
    }

    private LFgExample getExample(AnnoSentence sent, RelationMentions rels, boolean labeledExample) {
        RelationsFactorGraphBuilder rfgb = new RelationsFactorGraphBuilder(prm);
        FactorGraph fg = new FactorGraph();
        rfgb.build(sent, ofc, fg, cs);
        
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

    private static void addRelVarAssignments(AnnoSentence sent, RelationMentions rels, RelationsFactorGraphBuilder rfgb,
            VarConfig vc) {
        // Iterate over all pairs of mentions, such that ne1 comes before ne2.
        // This code assumes that the mentions are already in sorted order.
        NerMentions nes = sent.getNamedEntities();
        for (int i = 0; i < nes.size(); i++) {
            NerMention ne1 = nes.get(i);
            for (int j=i+1; j < nes.size(); j++) {
                NerMention ne2 = nes.get(j);
                String relation = getRelation(rels, ne1, ne2);
                RelVar var = rfgb.getVar(ne1, ne2);
                vc.put(var, relation);
            }
        }
    }

    public static String getRelation(RelationMentions rels, NerMention ne1, NerMention ne2) {
        RelationMention rm = rels.get(ne1, ne2);
        String relation;
        if (rm == null) {
            relation = "NO_RELATION";
        } else {
            String role1 = rm.getArgs().get(0).get1();
            String role2 = rm.getArgs().get(1).get1();
            relation = String.format("%s:%s:%s", rm.getType(), role1, role2);
        }
        return relation;
    }
        
}

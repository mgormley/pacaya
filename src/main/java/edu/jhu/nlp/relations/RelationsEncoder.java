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
import edu.jhu.util.collections.Lists;

public class RelationsEncoder implements Encoder<AnnoSentence, RelationMentions> {

    public static final String NO_RELATION_LABEL = "NO_RELATION";

    public enum DatasetType {
        ACE2004, ACE2005
    }
    
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

    public static void addRelVarAssignments(AnnoSentence sent, RelationMentions rels, RelationsFactorGraphBuilder rfgb,
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
            relation = NO_RELATION_LABEL;
        } else if (isAsymmetric(rm.getType(), rm.getSubType(), DatasetType.ACE2005)) {
            String role1 = rm.getArgs().get(0).get1();
            String role2 = rm.getArgs().get(1).get1();
            relation = String.format("%s:%s:%s", rm.getType(), role1, role2);
        } else {
            relation = String.format("%s:%s:%s", rm.getType(), "Arg-1", "Arg-1");
        }
        return relation;
    }
    
    private static boolean isAsymmetric(String relType, String relSubtype, DatasetType dataType) {
        // Currently, we rely on the DatasetType.ANNOTATION which loses information about the source.
        // Here we assume the use of ACE 2005 or throw an error.
        if (relType.equals("DISC")) {
            // The DISC relation only appears in ACE 2004.
            throw new RuntimeException("ACE 2004 not currently supported");
        } else {
            dataType = DatasetType.ACE2005;
        }
        
        if (dataType == DatasetType.ACE2005) {
            // This is valid whether or not subtypes are used.
            List<String> asymmtricTypes = Lists.getList("ART", "GEN-AFF", "ORG-AFF", "PART-WHOLE");
            return asymmtricTypes.contains(relType);
//        } else if (dataType == DatasetType.ACE2004 && !useRelationSubtype) {
//            // Following prior work, only PER-SOC (and NONE) are the fully symmetric types.
//            List<String> asymmtricTypes = Lists.getList("PHYS", "EMP-ORG", "ART", "OTHER-AFF", "GPE-AFF", "DISC");
//            return asymmtricTypes.contains(rm.getType());
        } else {
            // TODO: Implement case for ACE '04 subtypes.
            throw new RuntimeException("Not yet implemented");
        }
    }
        
}

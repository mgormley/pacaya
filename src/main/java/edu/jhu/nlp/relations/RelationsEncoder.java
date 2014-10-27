package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.List;

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
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;

public class RelationsEncoder implements Encoder<AnnoSentence, List<String>> {

    private static String NO_RELATION_LABEL = null;
    
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
        ObsFeatureExtractor relFe = new RelObsFe(prm, sent, ofc.getTemplates());
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
    
    public static void addNePairsAndRelLabels(AnnoSentence sent) {
        if (sent.getRelations() == null || sent.getNamedEntities() == null) {
            throw new RuntimeException("Missing relations or named entities");
        }
        List<Pair<NerMention,NerMention>> nePairs = new ArrayList<>();
        List<String> relLabels = new ArrayList<>();
        
        NerMentions nes = sent.getNamedEntities();
        RelationMentions rels = sent.getRelations();
        // Add positive instances.
        // 
        // Note: we require gold instances here since, on the ACE '05 data, there are 28 relations
        // which appear in the gold data but wouldn't be added just by iterating over all pairs 
        // of named entities as below. These include relations between an entity and itself, and cases
        // where the original training data contains multiple copies of the same relation.
        for (RelationMention rm : rels) {
            List<Pair<String, NerMention>> argsOrd = rm.getNerOrderedArgs();
            NerMention ne1 = argsOrd.get(0).get2();
            NerMention ne2 = argsOrd.get(1).get2();
            nePairs.add(new Pair<NerMention,NerMention>(ne1, ne2));
            String relation = getRelation(rels, ne1, ne2);
            relLabels.add(relation);
        }
        // Add negative instances. 
        //
        // Iterate over all pairs of mentions, such that ne1 comes before ne2.
        // This code assumes that the mentions are already in sorted order.
        for (int i = 0; i < nes.size(); i++) {
            NerMention ne1 = nes.get(i);
            for (int j = i + 1; j < nes.size(); j++) {
                NerMention ne2 = nes.get(j);
                
                int numMentsBtwn = RelObsFe.getNumBtwn(sent, ne1, ne2);
                if (numMentsBtwn <= RelationsOptions.maxInterveningEntities) {                
                    String relation = getRelation(rels, ne1, ne2);
                    if (getNoRelationLabel().equals(relation)) {
                        nePairs.add(new Pair<NerMention,NerMention>(ne1, ne2));
                        relLabels.add(relation);
                    }
                }
            }
        }
        sent.setNePairs(nePairs);
        sent.setRelLabels(relLabels);
        
        if (RelationsOptions.removeEntityTypes) {
            // Replace entity types with Brown cluster tags.
            for (NerMention ne : nes) {
                ne.setEntityType(sent.getCluster(ne.getHead()));
                ne.setEntitySubType(sent.getCluster(ne.getHead()));
            }
        }
    }
    
    public static String getNoRelationLabel() {
        if (NO_RELATION_LABEL == null) {
            NO_RELATION_LABEL = getRelation("NO_RELATION", "", "Arg-1", "Arg-1");                 
        }
        return NO_RELATION_LABEL;
    }
    
    private static String getRelation(RelationMentions rels, NerMention ne1, NerMention ne2) {
        RelationMention rm = rels.get(ne1, ne2);
        String relation;
        if (rm == null) {
            relation = getNoRelationLabel();
        } else if (isAsymmetric(rm.getType(), rm.getSubType(), DatasetType.ACE2005)) {
            List<Pair<String, NerMention>> argsOrd = rm.getNerOrderedArgs();
            Pair<String, NerMention> arg1 = argsOrd.get(0);
            Pair<String, NerMention> arg2 = argsOrd.get(1);
        	String role1 = arg2.get1();
        	String role2 = arg1.get1();
        	assert arg1.get2().compareTo(arg2.get2()) <= 0;
            relation = getRelation(rm.getType(), rm.getSubType(), role1, role2);
        } else {
            relation = getRelation(rm.getType(), rm.getSubType(), "Arg-1", "Arg-1");
        }
        return relation;
    }

    private static String getRelation(String type, String subtype, String role1, String role2) {
        String relation = type;
        if (RelationsOptions.useRelationSubtype) {
            relation += "-" + subtype;
        }
        if (RelationsOptions.predictArgRoles) {
            return String.format("%s(%s,%s)", relation, role1, role2);
        } else {
            return String.format("%s", relation, role1, role2);
        }
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

    public static List<AnnoSentence> getSingletons(List<AnnoSentence> sents) {
        List<AnnoSentence> singles = new ArrayList<>();
        for (int i=0; i<sents.size(); i++) {
            AnnoSentence sent = sents.get(i);
            for (int k=0; k<sent.getRelLabels().size(); k++) {
                AnnoSentence single = sent.getShallowCopy();
                single.setNePairs(Lists.getList(sent.getNePairs().get(k)));
                single.setRelLabels(Lists.getList(sent.getRelLabels().get(k)));
                // TODO: Remove these.
                //single.removeAt(AT.NER);
                //single.removeAt(AT.RELATIONS);
                singles.add(single);
            }
        }
        return singles;
    }
        
}

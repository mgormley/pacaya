package edu.jhu.nlp.relations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.Trainable;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Prm;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.collections.Sets;

/**
 * Munging of relations and named entity pairs in order to facilitate relation extraction
 * experiments.
 * 
 * @author mgormley
 */
public class RelationMunger implements Serializable {

    public enum DatasetType {
        ACE2004, ACE2005
    }
    
    public static class RelationMungerPrm extends Prm {
        private static final long serialVersionUID = 1L;
        @Opt(description="Whether to predict the roles for the arguments (i.e. direction of the relation).")
        public boolean predictArgRoles = true;
        @Opt(description="Whether to use the relation subtypes.")
        public boolean useRelationSubtype = false;
        @Opt(description="The maximum number of intervening entities to allow when generating negative examples.")
        public int maxInterveningEntities = Integer.MAX_VALUE;
        @Opt(description="Whether to remove entity types from mentions (odd setting, but needed for compat w/PM13).")
        public boolean removeEntityTypes = false;
        @Opt(description="Whether to shorten entity mention spans following Zhou et al. (2005)")
        public boolean shortenEntityMentions = true;      
        @Opt(hasArg = true, description = "Whether ReConcreteReader should create a separate sentence for each relation.")
        public boolean makeRelSingletons = true;  
    }

    private static final long serialVersionUID = 1L;
    private static String NO_RELATION_LABEL = null;
    private final Pattern REL_LABEL_SPLITS = Pattern.compile("^([^-\\(]+)(?:-([^\\(]+))?(?:\\((.+),(.+)\\))?$");  
    private RelationMungerPrm prm;
    
    public RelationMunger(RelationMungerPrm prm) {
        this.prm = prm;
    }
    
    public RelationDataPreproc getDataPreproc() { return new RelationDataPreproc(); }
    public RelationDataPostproc getDataPostproc() { return new RelationDataPostproc(); }

    public static boolean isNoRelationLabel(String relation) {
        return relation.startsWith(NO_RELATION_LABEL);
    }
    
    String getNoRelationLabel() {
        if (NO_RELATION_LABEL == null) {
            NO_RELATION_LABEL = buildRelation("NO_RELATION", "", "Arg-1", "Arg-1");                 
        }
        return NO_RELATION_LABEL;
    }
    
    String buildRelation(String type, String subtype, String role1, String role2) {
        String relation = type;
        if (prm.useRelationSubtype) {
            relation += "-" + subtype;
        }
        if (prm.predictArgRoles) {
            return String.format("%s(%s,%s)", relation, role1, role2);
        } else {
            return String.format("%s", relation);
        }
    }
    
    String[] unbuildRelation(String relLabel) {
        Matcher m = REL_LABEL_SPLITS.matcher(relLabel);
        if (!m.matches()) {
            throw new RuntimeException("Unable to unbuild relation label: " + relLabel);
        }
        String type = m.group(1);
        String subtype = m.group(2);
        String role1 = m.group(3);
        String role2 = m.group(4);
        return new String[] { type, subtype, role1, role2 };
    }
    
    /**
     * Creates {@link AT#NE_PAIRS} from {@link AT#NER}, and {@link AT#REL_LABELS} from {@link AT#RELATIONS}.
     * @author mgormley
     */
    public class RelationDataPreproc implements Annotator {
        
        private static final long serialVersionUID = 1L;
        private final Logger log = LoggerFactory.getLogger(RelationDataPreproc.class);

        @Override
        public Set<AT> getAnnoTypes() {
            return Collections.emptySet();
        }
        
        @Override
        public void annotate(AnnoSentenceCollection aSents) {
            munge(aSents);
        }
        
        public void munge(AnnoSentenceCollection aSents) {
            // TODO: This is a hack to replicate the PM13 setting. Think of a better way to incorporate this.
            for (AnnoSentence aSent : aSents) {     
                if (prm.shortenEntityMentions) {
                    // (maybe) Set the end of the span to be the head token.
                    shortenEntityMentions(aSent);
                }
                // Add the named entity pairs.
                addNePairsAndMaybeRelLabels(aSent);
            }
            if (prm.makeRelSingletons) {
                AnnoSentenceCollection tmpSents = getSingletons(aSents);
                // Deterministically shuffle the positive and negative examples for this communication.
                Collections.shuffle(tmpSents, new Random(1234567890));
                aSents.clear();
                aSents.addAll(tmpSents);
            }
        }
        
        void shortenEntityMentions(AnnoSentence aSent) {
            for (NerMention m : aSent.getNamedEntities()) {
                // Set the end of the span to be the head token.
                m.getSpan().setEnd(m.getHead()+1);
            }
            aSent.getNamedEntities().sort();
        }
        
        void addNePairsAndMaybeRelLabels(AnnoSentence sent) {
            addNePairs(sent);
            if (sent.hasAt(AT.RELATIONS)) {
                addRelLabels(sent);
            }
        }

        protected void addNePairs(AnnoSentence sent) {
            if (sent.getNamedEntities() == null) { throw new RuntimeException("Missing named entities"); }

            NerMentions nes = sent.getNamedEntities();
            RelationMentions rels = sent.getRelations();
            
            if (prm.removeEntityTypes) {
                for (NerMention ne : nes) {
                    ne.setEntityType(null);
                    ne.setEntitySubType(null);
                }
            }

            // Add pairs of named entities.
            List<Pair<NerMention,NerMention>> nePairs = new ArrayList<>();
            
            if (rels != null) {
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
                }
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
                    if (numMentsBtwn <= prm.maxInterveningEntities) {   
                        if (rels != null) {
                            // Only add if negative example, since we already added positive examples.
                            String relation = getRelation(rels, ne1, ne2);
                            if (getNoRelationLabel().equals(relation)) {
                                nePairs.add(new Pair<NerMention,NerMention>(ne1, ne2));
                            }
                        } else {
                            // Add all examples.
                            nePairs.add(new Pair<NerMention,NerMention>(ne1, ne2));
                        }
                    }
                }
            }
            sent.setNePairs(nePairs);
        }

        protected void addRelLabels(AnnoSentence sent) {
            // Add a relation label for each pair.
            if (sent.getRelations() == null) { throw new RuntimeException("Missing relations"); }
            RelationMentions rels = sent.getRelations();
            List<String> relLabels = new ArrayList<>();
            for (Pair<NerMention,NerMention> pair : sent.getNePairs()) {
                String relation = getRelation(rels, pair.get1(), pair.get2());
                relLabels.add(relation);
            }
            sent.setRelLabels(relLabels);
        }
        
        AnnoSentenceCollection getSingletons(AnnoSentenceCollection sents) {
            AnnoSentenceCollection singles = new AnnoSentenceCollection();
            for (int i=0; i<sents.size(); i++) {
                AnnoSentence sent = sents.get(i);
                for (int k=0; k<sent.getRelLabels().size(); k++) {
                    AnnoSentence single = sent.getShallowCopy();
                    single.setNePairs(Lists.getList(sent.getNePairs().get(k)));
                    single.setRelLabels(Lists.getList(sent.getRelLabels().get(k)));
                    singles.add(single);
                }
            }
            return singles;
        }           

        private String getRelation(RelationMentions rels, NerMention ne1, NerMention ne2) {
            RelationMention rm = rels.get(ne1, ne2);
            String relation;
            if (rm == null) {
                relation = getNoRelationLabel();
            } else if (isAsymmetric(rm.getType(), rm.getSubType(), DatasetType.ACE2005)) {
                List<Pair<String, NerMention>> argsOrd = rm.getNerOrderedArgs();
                Pair<String, NerMention> arg1 = argsOrd.get(0);
                Pair<String, NerMention> arg2 = argsOrd.get(1);
                String role1 = arg1.get1();
                String role2 = arg2.get1();
                assert arg1.get2().compareTo(arg2.get2()) <= 0;
                relation = buildRelation(rm.getType(), rm.getSubType(), role1, role2);
            } else {
                relation = buildRelation(rm.getType(), rm.getSubType(), "Arg-1", "Arg-1");
            }
            return relation;
        }
        
        private boolean isAsymmetric(String relType, String relSubtype, DatasetType dataType) {
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
//            } else if (dataType == DatasetType.ACE2004 && !useRelationSubtype) {
//                // Following prior work, only PER-SOC (and NONE) are the fully symmetric types.
//                List<String> asymmtricTypes = Lists.getList("PHYS", "EMP-ORG", "ART", "OTHER-AFF", "GPE-AFF", "DISC");
//                return asymmtricTypes.contains(rm.getType());
            } else {
                // TODO: Implement case for ACE '04 subtypes.
                throw new RuntimeException("Not yet implemented");
            }
        }
        
    }
    
    /**
     * Creates {@link AT#RELATIONS} from {@link AT#NE_PAIRS} and {@link AT#REL_LABELS}.
     * @author mgormley
     */
    public class RelationDataPostproc extends AbstractParallelAnnotator implements Annotator {
        
        private static final long serialVersionUID = 1L;
        private final Logger log = LoggerFactory.getLogger(RelationDataPreproc.class);

        @Override
        public Set<AT> getAnnoTypes() {
            return Sets.getSet(AT.RELATIONS);
        }

        @Override
        public void annotate(AnnoSentence sent) {
            munge(sent);            
        }

        public void munge(AnnoSentence sent) {
            if (prm.makeRelSingletons) {
                throw new IllegalStateException("Unable to reverse the munging when singletons are made.");
            }
            if (sent.hasAt(AT.NE_PAIRS) && sent.hasAt(AT.REL_LABELS)) {
                assert sent.hasAt(AT.NER);
                addRelationsFromRelLabelsAndNePairs(sent);
            }
        }
        
        /**
         * Creates and adds the relations {@link RelationMentions} to the AnnoSentence from the nePairs and relLabels. 
         * 
         * This reverses the work of {@link #addNePairsAndMaybeRelLabels(AnnoSentence)}.
         */
        void addRelationsFromRelLabelsAndNePairs(AnnoSentence sent) {
            if (!sent.hasAt(AT.REL_LABELS)) { throw new IllegalStateException("Missing relation mentions"); }
            if (!sent.hasAt(AT.NE_PAIRS)) { throw new IllegalStateException("Missing NE pairs"); }
            if (!sent.hasAt(AT.NER)) { throw new IllegalStateException("Missing NER mentions"); }
            List<String> relLabels = sent.getRelLabels();
            List<Pair<NerMention,NerMention>> nePairs = sent.getNePairs();
            if (relLabels.size() != nePairs.size()) {
                throw new IllegalStateException(String.format(
                        "Number of relation labels %d is different than the number of named entity pairs %d",
                        relLabels.size(), nePairs.size()));
            }
            RelationMentions relations = new RelationMentions();
            for (int i=0; i<nePairs.size(); i++) {
                String relLabel = relLabels.get(i);
                if (!relLabel.equals(getNoRelationLabel())) {
                    Pair<NerMention, NerMention> nePair = nePairs.get(i);
                    String[] splits = unbuildRelation(relLabel);
                    if (splits.length != 3 && splits.length != 4) {
                        log.error("Invalid splits: " + Arrays.toString(splits));
                        throw new IllegalStateException("Unable to convert relation label to RelationMention: " + relLabel + " " + splits.length);
                    }
                    int s=0;
                    String type = splits[s++];
                    String subType = (splits.length == 4) ? splits[s++] : null;
                    List<Pair<String, NerMention>> args = new ArrayList<>();
                    args.add(new Pair<String, NerMention>(splits[s++], nePair.get1()));
                    args.add(new Pair<String, NerMention>(splits[s++], nePair.get2()));
                    // Set all the fields on the RelationMention except the trigger.
                    relations.add(new RelationMention(type, subType, args , null));
                }
            }
            sent.setRelations(relations);
        }
        
    }

    public RelationMungerPrm getPrm() {
        return prm;
    }   

}

package edu.jhu.nlp.relations;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.relations.RelationMunger.RelationDataPostproc;
import edu.jhu.nlp.relations.RelationMunger.RelationDataPreproc;
import edu.jhu.nlp.relations.RelationMunger.RelationMungerPrm;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;

public class RelationMungerTest {
        
    @Test
    public void testAddNePairsAndRelLabels() throws Exception {
        AnnoSentence sent =  getSentWithRelationsAndNer();
        sent.removeAt(AT.NE_PAIRS);
        sent.removeAt(AT.REL_LABELS);
        
        // Convert RELATIONS and NER to NE_PAIRS and REL_LABELS.
        RelationMungerPrm prm = new RelationMungerPrm();
        prm.useRelationSubtype = true;
        prm.predictArgRoles = true;
        RelationMunger munger = new RelationMunger(prm);
        RelationDataPreproc pre = munger.getDataPreproc(); 
        pre.addNePairsAndRelLabels(sent);
       
        List<Pair<NerMention, NerMention>> nePairs = sent.getNePairs();
        NerMentions ner = sent.getNamedEntities();
        System.out.println(nePairs);
        assertEquals(ner.get(1), nePairs.get(0).get1());
        assertEquals(ner.get(2), nePairs.get(0).get2());
        assertEquals(ner.get(0), nePairs.get(1).get1());
        assertEquals(ner.get(1), nePairs.get(1).get2());
        assertEquals(ner.get(0), nePairs.get(2).get1());
        assertEquals(ner.get(0), nePairs.get(2).get2());
        
        // Check the conversion.
        List<String> relLabels = sent.getRelLabels();
        System.out.println(relLabels);
        assertEquals("ART-SUBART(Arg-1,Arg-2)", relLabels.get(0));
        assertEquals("SEE-SUBSEE(Arg-1,Arg-1)", relLabels.get(1));
        assertEquals("SELF-SUBSELF(Arg-1,Arg-1)", relLabels.get(2));
    }

    @Test
    public void testAddRelationsFromRelLabelsAndNePairs() throws Exception {    
        AnnoSentence sent = getSentWithRelationsAndNer();
        sent.removeAt(AT.RELATIONS);
                
        // Convert NE_PAIRS and REL_LABELS to RELATIONS.
        RelationMungerPrm prm = new RelationMungerPrm();
        prm.useRelationSubtype = true;
        prm.predictArgRoles = true;
        RelationMunger munger = new RelationMunger(prm);
        RelationDataPostproc post = munger.getDataPostproc(); 
        post.addRelationsFromRelLabelsAndNePairs(sent);
        
        // The expected Relations.
        NerMentions ner = sent.getNamedEntities();
        RelationMentions expRelations = new RelationMentions();
        expRelations.add(new RelationMention(
                "ART", // Asymmetric type. 
                "SUBART", 
                Lists.getList(
                        new Pair<>("Arg-2", ner.get(2)), // Swapped order tests nePairs ordering.
                        new Pair<>("Arg-1", ner.get(1))),
                null));
        expRelations.add(new RelationMention(
                "SEE", // Symmetric type. 
                "SUBSEE", 
                Lists.getList(
                        new Pair<>("Arg-1", ner.get(0)), 
                        new Pair<>("Arg-1", ner.get(1))), // Note the symmetry here.
                null));
        expRelations.add(new RelationMention(
                "SELF", // Symmetric type.
                "SUBSELF", 
                Lists.getList(new Pair<>("Arg-1", ner.get(0)), new Pair<>("Arg-1", ner.get(0))),
                null));
        
        RelationMentions actRelations = sent.getRelations();
        assertEquals(expRelations.size(), actRelations.size());
        for (int i=0; i<expRelations.size(); i++) {
            RelationMention erm = expRelations.get(i);
            RelationMention arm = actRelations.get(i);
            assertEquals(erm.getType(), arm.getType());
            assertEquals(erm.getSubType(), arm.getSubType());
            assertEquals(erm.getTrigger(), arm.getTrigger());
            assertEquals(erm.getNerOrderedArgs(), arm.getNerOrderedArgs());
        }
    }

    private static AnnoSentence getSentWithRelationsAndNer() {
        // Create Sentence with RELATIONS and NER.
        AnnoSentence sent = new AnnoSentence();
        int n = 5;
        sent.setWords(Lists.getList("dog","spied", "the", "cat", "from", "MD"));
        
        // Named Entities.
        NerMentions ner = new NerMentions(n, 
                Lists.getList(
                        new NerMention(new Span(0, 1), "MAMMAL", "DOG", "noun", 0, "uuid1"),
                        new NerMention(new Span(2, 6), "MAMMAL", "CAT", "noun", 3, "uuid2"),
                        new NerMention(new Span(5, 6), "LOCATION", "STATE", "noun", 5, "uuid3")));
        sent.setNamedEntities(ner);
        
        // Relations.
        RelationMentions relations = new RelationMentions();
        relations.add(new RelationMention(
                "ART", // Asymmetric type. 
                "SUBART", 
                Lists.getList(
                        new Pair<>("Arg-2", ner.get(2)), // Swapped order tests nePairs ordering.
                        new Pair<>("Arg-1", ner.get(1))),
                null));
        relations.add(new RelationMention(
                "SEE", // Symmetric type. 
                "SUBSEE", 
                Lists.getList(
                        new Pair<>("Arg-1", ner.get(0)), 
                        new Pair<>("Arg-2", ner.get(1))),
                null));
        relations.add(new RelationMention(
                "SELF", // Symmetric type.
                "SUBSELF", 
                Lists.getList(new Pair<>("Arg-1", ner.get(0)), new Pair<>("Arg-1", ner.get(0))),
                null));
        sent.setRelations(relations);

        // Named Entity Pairs.
        List<Pair<NerMention, NerMention>> nePairs = new ArrayList<>();
        nePairs.add(new Pair<>(ner.get(1), ner.get(2)));
        nePairs.add(new Pair<>(ner.get(0), ner.get(1)));
        nePairs.add(new Pair<>(ner.get(0), ner.get(0)));
        sent.setNePairs(nePairs);
        
        // Relation Labels.
        List<String> relLabels = new ArrayList<>();
        relLabels.add("ART-SUBART(Arg-1,Arg-2)");
        relLabels.add("SEE-SUBSEE(Arg-1,Arg-1)");
        relLabels.add("SELF-SUBSELF(Arg-1,Arg-1)");
        sent.setRelLabels(relLabels);
        
        return sent;
    }

    @Test
    public void testBuildUnbuildRelation() throws Exception {
        String type = "type";
        String subtype = "subtype";
        String role1 = "role1";
        String role2 = "role2";
        {
            RelationMungerPrm prm = new RelationMungerPrm();
            prm.useRelationSubtype = true;
            prm.predictArgRoles = true;
            RelationMunger munger = new RelationMunger(prm);
            assertEquals("type-subtype(role1,role2)", munger.buildRelation(type, subtype, role1, role2));
            Assert.assertArrayEquals(new String[]{type, subtype, role1, role2}, munger.unbuildRelation("type-subtype(role1,role2)"));
        }
        {
            RelationMungerPrm prm = new RelationMungerPrm();
            prm.useRelationSubtype = false;
            prm.predictArgRoles = true;
            RelationMunger munger = new RelationMunger(prm);
            assertEquals("type(role1,role2)", munger.buildRelation(type, subtype, role1, role2));
            Assert.assertArrayEquals(new String[]{type, null, role1, role2}, munger.unbuildRelation("type(role1,role2)"));
        }
        {
            RelationMungerPrm prm = new RelationMungerPrm();
            prm.useRelationSubtype = true;
            prm.predictArgRoles = false;
            RelationMunger munger = new RelationMunger(prm);
            assertEquals("type-subtype", munger.buildRelation(type, subtype, role1, role2));
            Assert.assertArrayEquals(new String[]{type, subtype, null, null}, munger.unbuildRelation("type-subtype"));
        }
        {
            RelationMungerPrm prm = new RelationMungerPrm();
            prm.useRelationSubtype = false;
            prm.predictArgRoles = false;
            RelationMunger munger = new RelationMunger(prm);
            assertEquals("type", munger.buildRelation(type, subtype, role1, role2));
            Assert.assertArrayEquals(new String[]{type, null, null, null}, munger.unbuildRelation("type"));
        }
    }
    
    @Test
    public void testGetSingletons() throws Exception {
        AnnoSentence sent = getSentWithRelationsAndNer();        
        RelationMungerPrm prm = new RelationMungerPrm();
        RelationMunger munger = new RelationMunger(prm);
        RelationDataPreproc pre = munger.getDataPreproc();        
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        sents.add(sent);
        sents = pre.getSingletons(sents);
        assertEquals(3, sents.size());
    }

    @Test
    public void testShortenEntityMentions() throws Exception {
        // Create Sentence with RELATIONS and NER.
        AnnoSentence sent = new AnnoSentence();
        int n = 5;
        sent.setWords(Lists.getList("dog","spied", "the", "cat", "from", "MD"));        
        // Named Entities.
        NerMentions ner = new NerMentions(n, 
                Lists.getList(
                        new NerMention(new Span(0, 1), "MAMMAL", "DOG", "noun", 0, "uuid1"),
                        new NerMention(new Span(2, 6), "MAMMAL", "CAT", "noun", 3, "uuid2"),
                        new NerMention(new Span(5, 6), "LOCATION", "STATE", "noun", 5, "uuid3")));
        sent.setNamedEntities(ner);
        
        RelationMungerPrm prm = new RelationMungerPrm();
        RelationMunger munger = new RelationMunger(prm);
        RelationDataPreproc pre = munger.getDataPreproc();        
        pre.shortenEntityMentions(sent);
        assertEquals(4, ner.get(1).getSpan().end());
    }

}

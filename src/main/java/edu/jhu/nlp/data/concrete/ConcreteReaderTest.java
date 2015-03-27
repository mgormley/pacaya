package edu.jhu.nlp.data.concrete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Constituent;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMention;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.MentionArgument;
import edu.jhu.hlt.concrete.Parse;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.SituationMention;
import edu.jhu.hlt.concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.TaggedToken;
import edu.jhu.hlt.concrete.TextSpan;
import edu.jhu.hlt.concrete.Token;
import edu.jhu.hlt.concrete.TokenList;
import edu.jhu.hlt.concrete.TokenRefSequence;
import edu.jhu.hlt.concrete.TokenTagging;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.TokenizationKind;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.hlt.concrete.util.ConcreteUUIDFactory;
import edu.jhu.nlp.data.concrete.ConcreteReader.ConcreteReaderPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.util.collections.Lists;

public class ConcreteReaderTest {

    private static ConcreteUUIDFactory uuidFactory = new ConcreteUUIDFactory();

    @Test
    public void testToolNames() throws Exception {
        // Test that we correctly get the second tool's annotations.
        ConcreteReaderPrm prm = new ConcreteReaderPrm();
        prm.posTool = "ToolTwo";
        prm.lemmaTool = "ToolTwo";
        prm.chunkTool = "ToolTwo";
        prm.nerTool = "ToolTwo";
        prm.depParseTool = "ToolTwo";
        prm.parseTool = "ToolTwo";
        prm.nerTool = "ToolTwo";
        prm.relationTool = "ToolTwo";
        ConcreteReader r = new ConcreteReader(prm);
        AnnoSentenceCollection sents = r.sentsFromComm(createSimpleCommunication());
        assertEquals(1, sents.size());
        AnnoSentence sent = sents.get(0);
        assertEquals("n", sent.getPosTag(0));
        assertEquals("vic", sent.getLemma(0));
        assertEquals("bn", sent.getChunk(0));
        assertEquals(-1, sent.getParent(0));
        assertTrue(sent.getNaryTree().getSymbol().startsWith("symbol"));
        assertEquals("per0", sent.getNamedEntities().get(0).getEntityType());
        assertEquals("near0", sent.getRelations().get(0).getType());
    }

    public static Communication createSimpleCommunication() throws Exception {
        Communication comm = new Communication();
        comm.setId("Gore-y Landing");
        comm.setText("vice pres says jump");
        // 0123456789012345678
        comm.setType("Test");
        comm.setUuid(getUUID());
        
        String toolName = "TestTool";
        comm.setMetadata(getMetadata(toolName));

        // Tokens
        Tokenization tokenization = new Tokenization();
        tokenization.setKind(TokenizationKind.TOKEN_LIST);
        tokenization.setUuid(getUUID());
        List<Token> listOfTokens = new ArrayList<Token>();
        String[] tokens = new String[] { "vice", "pres", "says", "jump" };
        for (int i = 0; i < tokens.length; i++) {
            Token token = new Token();
            token.setText(tokens[i]);
            token.setTokenIndex(i);
            listOfTokens.add(i, token);
        }
        TokenList tokenList = new TokenList();
        tokenList.setTokenList(listOfTokens);
        tokenization.setTokenList(tokenList);
        tokenization.setMetadata(getMetadata(toolName));

        // POS Tags
        addTagging(tokenization, "POS", "ToolOne tags", new String[] { "N", "N", "V", "V" });
        addTagging(tokenization, "POS", "ToolTwo tags", new String[] { "n", "n", "v", "v" });
        
        // Lemmas
        addTagging(tokenization, "LEMMA", "ToolOne tags", new String[] { "VIC", "PRE", "SAY", "JUM" });
        addTagging(tokenization, "LEMMA", "ToolTwo tags", new String[] { "vic", "pre", "say", "jum" });
        
        // Chunks
        addTagging(tokenization, "CHUNK", "ToolOne tags", new String[] { "BN", "IN", "BV", "IV" });
        addTagging(tokenization, "CHUNK", "ToolTwo tags", new String[] { "bn", "in", "bv", "iv" });

        // Dependency Parse
        addDepParse(tokenization, "ToolOne", new int[] { 1, 2, 3, -1 });
        addDepParse(tokenization, "ToolTwo", new int[] { -1, 0, 1, 2 });
                
        // Constituency Parse
        addParse(tokenization, "ToolOne", "SYMBOL");
        addParse(tokenization, "ToolTwo", "symbol");
        
        // Sentence
        Sentence sentence = new Sentence();
        sentence.setTokenization(tokenization);
        sentence.setUuid(getUUID());
        TextSpan sentenceSpan = new TextSpan();
        sentenceSpan.setStart(0);
        sentenceSpan.setEnding(18);
        sentence.setTextSpan(sentenceSpan);

        // Section
        Section section = new Section();
        section.addToSentenceList(sentence);
        section.setKind("SectionKind");
        section.setUuid(getUUID());
        TextSpan sectionSpan = new TextSpan();
        sectionSpan.setStart(0);
        sectionSpan.setEnding(18);
        comm.addToSectionList(section);

        // EntityMentionSet
        addEntityMentionSet(comm, tokenization, "ToolOne", "PER");
        addEntityMentionSet(comm, tokenization, "ToolTwo", "per");
        
        // SituationMentionSet
        addSituationMentionSet(comm, tokenization, "ToolOne", "NEAR", 0);
        addSituationMentionSet(comm, tokenization, "ToolTwo", "near", 1);
        
        return comm;
    }

    private static void addSituationMentionSet(Communication comm, Tokenization tokenization, String toolName, 
            String situationTypePrefix, int emsIdx) {
        EntityMention em1 = comm.getEntityMentionSetList().get(emsIdx).getMentionList().get(0);
        EntityMention em2 = comm.getEntityMentionSetList().get(emsIdx).getMentionList().get(1);
        
        List<SituationMention> smList = new ArrayList<>();
        for (int i=0; i<3; i++) {
            SituationMention sm = new SituationMention();
            sm.setUuid(getUUID());
            sm.setSituationType("STATE");
            sm.setSituationKind(situationTypePrefix+i);
            sm.setText("sm text");
            TokenRefSequence tref = new TokenRefSequence();
            tref.setTokenizationId(tokenization.getUuid());
            tref.setTokenIndexList(Lists.getList(i, i+1));
            sm.setTokens(tref);
            for (EntityMention em : Lists.getList(em1, em2)) {
                MentionArgument ma = new MentionArgument();
                ma.setEntityMentionId(em.getUuid());
                ma.setRole("role");
                sm.addToArgumentList(ma);
            }
            smList.add(sm);
        }
        SituationMentionSet sms = new SituationMentionSet();
        sms.setUuid(getUUID());
        sms.setMetadata(getMetadata(toolName));
        sms.setMentionList(smList);
        comm.addToSituationMentionSetList(sms);
    }
    
    private static void addEntityMentionSet(Communication comm, Tokenization tokenization, String toolName, String entityTypePrefix) {
        List<EntityMention> emList = new ArrayList<>();
        for (int i=0; i<3; i++) {
            EntityMention em = new EntityMention();
            em.setUuid(getUUID());
            em.setEntityType(entityTypePrefix+i);
            em.setPhraseType("ph"+i);
            em.setText("em text");
            TokenRefSequence tref = new TokenRefSequence();
            tref.setTokenizationId(tokenization.getUuid());
            tref.setTokenIndexList(Lists.getList(i, i+1));
            em.setTokens(tref);
            emList.add(em);
        }
        EntityMentionSet ems = new EntityMentionSet();
        ems.setUuid(getUUID());
        ems.setMetadata(getMetadata(toolName));
        ems.setMentionList(emList);
        comm.addToEntityMentionSetList(ems);
    }

    private static void addParse(Tokenization tokenization, String toolName, String tagPrefix) {
        int n = tokenization.getTokenList().getTokenListSize();
        List<Constituent> cList = new ArrayList<>();
        for (int i=0; i<n; i++) {
            Constituent c = new Constituent();
            c.setStart(i);
            c.setEnding(n);
            if (i < n-1) {
                c.setChildList(Lists.getList(i+1));
            } else {
                c.setChildList(new ArrayList<Integer>());
            }
            c.setHeadChildIndex(i);
            c.setTag(tagPrefix+i);
            c.setId(i);
            cList.add(c);
        }        
        Parse p = new Parse();
        p.setUuid(getUUID());
        p.setMetadata(getMetadata(toolName));
        p.setConstituentList(cList);
        tokenization.addToParseList(p);
    }


    private static void addDepParse(Tokenization tokenization, String toolName, int[] parents) {
        List<Dependency> depList = new ArrayList<>();
        int dep = 0;
        for (int gov : parents) {
            Dependency d = new Dependency();
            d.setEdgeType("l"+dep);
            d.setGov(gov);
            d.setDep(dep++);
            depList.add(d);
        }
        DependencyParse dp = new DependencyParse();
        dp.setUuid(getUUID());
        dp.setDependencyList(depList);
        dp.setMetadata(getMetadata(toolName));
        tokenization.addToDependencyParseList(dp);
    }

    private static void addTagging(Tokenization tokenization, String tagType, String toolName, String[] tags) {
        List<TaggedToken> taggedTokenList = new ArrayList<>();
        int i = 0;
        for (String tag : tags) {
            TaggedToken t = new TaggedToken();
            t.setTag(tag);
            t.setTokenIndex(i++);
            taggedTokenList.add(t);
        }
        TokenTagging tt = new TokenTagging();
        tt.setUuid(getUUID());
        tt.setMetadata(getMetadata(toolName));
        tt.setTaggedTokenList(taggedTokenList);
        tt.setTaggingType(tagType);
        tokenization.addToTokenTaggingList(tt);
    }

    private static AnnotationMetadata getMetadata(String toolName) {        
        AnnotationMetadata tokenizationMetadata = new AnnotationMetadata();
        tokenizationMetadata.setTimestamp(System.currentTimeMillis());
        tokenizationMetadata.setTool(toolName);
        return tokenizationMetadata;
    }

    private static UUID getUUID() {
        return uuidFactory.getConcreteUUID();
    }
    
}

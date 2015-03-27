package edu.jhu.nlp.data.concrete;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.TextSpan;
import edu.jhu.hlt.concrete.Token;
import edu.jhu.hlt.concrete.TokenList;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.TokenizationKind;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.hlt.concrete.serialization.CompactCommunicationSerializer;
import edu.jhu.hlt.concrete.uuid.UUIDFactory;
import edu.jhu.nlp.data.concrete.ConcreteReader.ConcreteReaderPrm;
import edu.jhu.nlp.data.concrete.ConcreteWriter.ConcreteWriterPrm;
import edu.jhu.nlp.data.conll.CoNLL09Sentence;
import edu.jhu.nlp.data.conll.SrlGraphTest;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;

public class ConcreteWriterTest {

    String concreteFilename = "/edu/jhu/nlp/data/concrete/agiga_dog-bites-man.concrete";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testWriteConcreteFile() throws Exception {
        // TODO: Create a Communication in-memory instead of reading from disk
        assertNotNull("Test file missing", getClass().getResource(concreteFilename));

        File f = new File(getClass().getResource(concreteFilename).getFile());
        ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());

        AnnoSentenceCollection asc = reader.sentsFromPath(f);

        for (AnnoSentence sent : asc) {
            System.out.println(sent);
        }

        // Create a temporary file.
        final File tempFile = tempFolder.newFile("tempFile.concrete");
//        File tempFile = new File("tempFile.concrete");

        ConcreteWriterPrm cwPrm = new ConcreteWriterPrm();
        cwPrm.addAnnoTypes(Arrays.asList(AT.DEP_TREE, AT.SRL, AT.NER, AT.RELATIONS));
        ConcreteWriter cw = new ConcreteWriter(cwPrm);
        cw.write(asc, tempFile);
    }

    @Test
    public void testCompareCommunications() throws Exception {
        CompactCommunicationSerializer ser = new CompactCommunicationSerializer();
        File concreteFile = new File(getClass().getResource(concreteFilename).getFile());

        ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());
        Communication commOne = ser.fromPathString(concreteFile.getAbsolutePath());
        AnnoSentenceCollection sents = reader.sentsFromComm(commOne);

        ConcreteWriterPrm cwPrm = new ConcreteWriterPrm();
        cwPrm.addAnnoTypes(Arrays.asList(AT.DEP_TREE, AT.SRL, AT.NER, AT.RELATIONS));
        ConcreteWriter cw = new ConcreteWriter(cwPrm);

        Communication commTwo = commOne.deepCopy();
        assertTrue(commOne.equals(commTwo));
        cw.addAnnotations(sents, commTwo);
        assertFalse(commOne.equals(commTwo));
    }

    @Test
    public void testCreateSrlCommunication() throws Exception {
        // Convert Communication to AnnoSentenceCollection
        Communication simpleComm = createSimpleCommunication();
        ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());
        AnnoSentenceCollection sentences = reader.sentsFromComm(simpleComm);

        // Write Communication to disk
        CompactCommunicationSerializer ser = new CompactCommunicationSerializer();
        Files.write(Paths.get("simpleFile.concrete"), ser.toBytes(simpleComm));

        // Add SRL graph to AnnoSentence
        SrlGraphTest sgt = new SrlGraphTest();
        CoNLL09Sentence cos = sgt.getSimpleCoNLL09Sentence();
        sentences.get(0).setSrlGraph(cos.getSrlGraph());

        // Add annotations to Communication
        ConcreteWriterPrm cwPrm = new ConcreteWriterPrm();
        cwPrm.addAnnoTypes(Arrays.asList(AT.DEP_TREE, AT.SRL, AT.NER, AT.RELATIONS));
        ConcreteWriter cw = new ConcreteWriter(cwPrm);
        cw.addAnnotations(sentences, simpleComm);

        // Write annotated Communication to disk
        final File srlFile = tempFolder.newFile("srlFile.concrete");
        cw.write(sentences, new File("srlFile.concrete"));
    }

    public static Communication createSimpleCommunication() throws Exception {
        Communication comm = new Communication();
        comm.setId("Gore-y Landing");
        comm.setText("vice pres says jump");
        // 0123456789012345678
        comm.setType("Test");
        comm.setUuid(getUUID());

        AnnotationMetadata commMetadata = new AnnotationMetadata();
        commMetadata.setTimestamp(System.currentTimeMillis());
        commMetadata.setTool("TestTool");
        comm.setMetadata(commMetadata);

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

        AnnotationMetadata tokenizationMetadata = new AnnotationMetadata();
        tokenizationMetadata.setTimestamp(System.currentTimeMillis());
        tokenizationMetadata.setTool("TestTool");
        tokenization.setMetadata(tokenizationMetadata);

        Sentence sentence = new Sentence();
        sentence.setTokenization(tokenization);
        sentence.setUuid(getUUID());
        TextSpan sentenceSpan = new TextSpan();
        sentenceSpan.setStart(0);
        sentenceSpan.setEnding(18);
        sentence.setTextSpan(sentenceSpan);

        Section section = new Section();
        section.addToSentenceList(sentence);
        section.setKind("SectionKind");
        section.setUuid(getUUID());
        TextSpan sectionSpan = new TextSpan();
        sectionSpan.setStart(0);
        sectionSpan.setEnding(18);

        comm.addToSectionList(section);

        return comm;
    }

    protected static UUID getUUID() {
        return UUIDFactory.newUUID();
    }
}

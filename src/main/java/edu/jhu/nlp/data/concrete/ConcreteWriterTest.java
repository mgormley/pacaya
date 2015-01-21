package edu.jhu.nlp.data.concrete;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.serialization.CompactCommunicationSerializer;
import edu.jhu.nlp.data.concrete.ConcreteReader.ConcreteReaderPrm;
import edu.jhu.nlp.data.concrete.ConcreteWriter;
import edu.jhu.nlp.data.concrete.ConcreteWriter.ConcreteWriterPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;


public class ConcreteWriterTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	@Test
	public void testWriteConcreteFile() throws Exception {
		// TODO: Create a Communication in-memory instead of reading from disk
		String concreteFile = "/agiga_dog-bites-man.concrete";
		
		assertNotNull("Test file missing", getClass().getResource(concreteFile));

		File f = new File(getClass().getResource(concreteFile).getFile());
		ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());

		AnnoSentenceCollection asc = reader.toSentences(f);
		
        for (AnnoSentence sent : asc) {
            System.out.println(sent);
        }
        
        // Create a temporary file.
        final File tempFile = tempFolder.newFile("tempFile.concrete");
//        File tempFile = new File("tempFile.concrete"); 
      
        ConcreteWriterPrm cwPerm = new ConcreteWriterPrm();
        cwPerm.addAnnoTypes(Arrays.asList(AT.DEP_TREE, AT.SRL, AT.NER));
        ConcreteWriter cw = new ConcreteWriter(cwPerm);
        cw.write(asc,  tempFile);
	}
	
	@Test
	public void testCompareCommunications() throws Exception {
		CompactCommunicationSerializer ser = new CompactCommunicationSerializer();
		String concreteFilename = "/agiga_dog-bites-man.concrete";
		File concreteFile = new File(getClass().getResource(concreteFilename).getFile());

        Communication commOne = ser.fromPathString(concreteFile.getAbsolutePath());
        
		ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());
		AnnoSentenceCollection sents = reader.toSentences(commOne);

		List<Communication> comms = (List<Communication>) sents.getSourceSents();
		Communication commTwo = comms.get(0);

        assertTrue(commOne.equals(commTwo));

        ConcreteWriterPrm cwPerm = new ConcreteWriterPrm();
        cwPerm.addAnnoTypes(Arrays.asList(AT.DEP_TREE, AT.SRL, AT.NER));
        ConcreteWriter cw = new ConcreteWriter(cwPerm);
        commTwo = commTwo.deepCopy();
        cw.addAnnotations(sents, commTwo);

        assertFalse(commOne.equals(commTwo));
        
        EntityMentionSet em1 = commOne.getEntityMentionSetList().get(0);
        EntityMentionSet em2 = commTwo.getEntityMentionSetList().get(0);
        assertTrue(em1.equals(em2));
	}
	
}

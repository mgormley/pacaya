/*
 * Copyright 2012-2014 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */
package edu.jhu.nlp.joint;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import concrete.tools.AnnotationException;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.nlp.AnnoPipeline;
import edu.jhu.nlp.data.concrete.ConcreteReader;
import edu.jhu.nlp.data.concrete.ConcreteReader.ConcreteReaderPrm;
import edu.jhu.nlp.data.concrete.ConcreteWriter;
import edu.jhu.nlp.data.concrete.ConcreteWriter.ConcreteWriterPrm;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.util.Prm;
import edu.jhu.util.Prng;
import edu.jhu.util.Threads;
import edu.jhu.util.files.Files;
import edu.jhu.util.report.ReporterManager;

/**
 * Annotator of communication that takes a serialized AnnoPipeline and annotates communications.
 * This is intended for use within a PostStanfordAnnotationTool for Annotated Gigaword v2.0.
 * 
 * @author mgormley
 */
public class CommunicationAnnotator {
    
    public static enum InputType { FILE, RESOURCE };
    
    public static class CommunicationAnnotatorPrm extends Prm {
        public static final long serialVersionUID = 1L;
        /** (REQUIRED) The path to the serialized pipeline. */
        public String pipeIn;
        /** (REQUIRED) Whether the path is a file or resource. */
        public InputType inputType;
        /** The number of threads for parallelizing across sentences. */
        public int threads = 1;
        /** The pseudo random number generator seed. */
        public long seed = Prng.DEFAULT_SEED;
        /** The reporting file. */
        public File reportOut = null;
        /** The concrete reader parameters. */
        public ConcreteReaderPrm crPrm = new ConcreteReaderPrm();
        /** The concrete writer parameters. (Output annotation types will be overwritten.)*/
        public ConcreteWriterPrm cwPrm = new ConcreteWriterPrm();
    }
    
    private static final Logger log = LoggerFactory.getLogger(CommunicationAnnotator.class);
    private CommunicationAnnotatorPrm prm;     // Parameters.    
    private AnnoPipeline anno;    // Cached.    
    
    public CommunicationAnnotator(CommunicationAnnotatorPrm prm) {
        if (prm.pipeIn == null) {
            throw new IllegalArgumentException("pipeIn must not be null");
        }
        if (prm.inputType == null) {
            throw new IllegalArgumentException("inputType must not be null");
        }
        this.prm = prm;
    }
    
    public void init() {
        ReporterManager.init(prm.reportOut, true);
        Prng.seed(prm.seed);
        Threads.initDefaultPool(prm.threads);
        if (prm.inputType == InputType.FILE) { 
            log.info("Reading the annotation pipeline from file: " + prm.pipeIn);
            this.anno = (AnnoPipeline) Files.deserialize(prm.pipeIn);
        } else { // inputType == InputType.RESOURCE
            log.info("Reading the annotation pipeline from resource: " + prm.pipeIn);
            this.anno = (AnnoPipeline) Files.deserializeResource(prm.pipeIn);
        }
    }

    public void annotate(Communication c) throws AnnotationException {
        ConcreteReader cr = new ConcreteReader(prm.crPrm );
        AnnoSentenceCollection sents = cr.sentsFromComm(c);
        anno.annotate(sents);
        // Overwrite the output annotation types on cwPrm.
        prm.cwPrm.addAnnoTypes(anno.getAnnoTypes());
        ConcreteWriter cw = new ConcreteWriter(prm.cwPrm);
        cw.addAnnotations(sents, c);
    }
    
    public void close() {
        Threads.shutdownDefaultPool();
        ReporterManager.close();
    }
}

/*
 * Copyright 2012-2014 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */
package edu.jhu.nlp.joint;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import concrete.tools.AnnotationException;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.nlp.AnnoPipeline;
import edu.jhu.nlp.data.concrete.ConcreteReader;
import edu.jhu.nlp.data.concrete.ConcreteWriter;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
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
public class CommunicationAnnotator { // TODO: implements PostStanfordAnnotationTool {
    private static final Logger log = LoggerFactory.getLogger(CommunicationAnnotator.class);

    // Parameters.
    private File pipeIn;
    private boolean concreteSrlIsSyntax;
    private int threads;
    private long seed;
    private File reportOut;
    
    // Cached.
    private AnnoPipeline anno;    
        
    public CommunicationAnnotator(File pipeIn, boolean concreteSrlIsSyntax, int threads, long seed, File reportOut) {
        if (pipeIn == null) {
            throw new IllegalArgumentException("pipeIn must not be null");
        }
        this.pipeIn = pipeIn;
        this.concreteSrlIsSyntax = concreteSrlIsSyntax;
        this.threads = threads;
        this.seed = seed;
        this.reportOut = reportOut;
    }
    
    // TODO: @Override
    public void init() {
        ReporterManager.init(reportOut, true);
        Prng.seed(seed);
        Threads.initDefaultPool(threads);
        log.info("Reading the annotation pipeline from file: " + pipeIn);
        this.anno = (AnnoPipeline) Files.deserialize(pipeIn);
    }

    // TODO: @Override
    public Communication annotate(Communication c) throws AnnotationException {
        // Return a copy.
        c = new Communication(c);
        ConcreteReader cr = new ConcreteReader();
        AnnoSentenceCollection sents = cr.toSentences(c);
        anno.annotate(sents);
        ConcreteWriter cw = new ConcreteWriter(concreteSrlIsSyntax);
        cw.addAnnotations(sents, c);
        return c;
    }
    
    public void close() {
        Threads.shutdownDefaultPool();
        ReporterManager.close();
    }
}

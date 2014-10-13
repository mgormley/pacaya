package edu.jhu.nlp.features;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.primitives.UnsignedBytes;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.tag.BrownClusterTagger;
import edu.jhu.nlp.tag.BrownClusterTagger.BrownClusterTaggerPrm;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.list.ShortArrayList;
import edu.jhu.prim.map.IntIntHashMap;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.tuple.Triple;
import edu.jhu.prim.tuple.Tuple;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Alphabet;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Timer;
import edu.jhu.util.hash.MurmurHash3;

/**
 * Class for comparing methods of representing features. Allows for testing of
 * OOM errors, speed up extraction/hashing/lookup.
 * 
 * @author mgormley
 */
public class FeatureCreationSpeedTest {

    private final int OPT = 4;

    //@Test
    public void testSpeedOfFeatureCreation() throws UnsupportedEncodingException, FileNotFoundException {
        // Params
        final int numExamples = 50000;
        final int numTemplates = 60;
        final int numRounds = 1;

        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();

        // Run
        System.out.println("Num sents: " + sents.size());
        //testFeatExtract(numTemplates, sents, new PairExtractor());
        //testFeatExtract(numTemplates, sents, new TupleExtractor());
        //testFeatExtract(numTemplates, sents, new StringExtractor());
        
        // "Training" where we fill the alphabet.
        FeatureNames alphabet = new FeatureNames();
        IntIntHashMap alphabet2 = new IntIntHashMap();
        testFeatExtract(numRounds, numTemplates, sents, true, alphabet, alphabet2);
        alphabet.stopGrowth();
        // "Testing" where the alphabet is filled.
        testFeatExtract(numRounds, numTemplates, sents, true, alphabet, alphabet2);
        
//        testFeatExtract(numRounds, numTemplates, sents, 0, true);
//        testFeatExtract(numRounds, numTemplates, sents, 1, true);
//        testFeatExtract(numRounds, numTemplates, sents, 2, true);
    }
    
    private void testFeatExtract(int numRounds, final int numTemplates, AnnoSentenceCollection sents, final boolean lookup, FeatureNames alphabet, IntIntHashMap alphabet2) {
        Timer timer = new Timer();
        Timer lookupTimer = new Timer();
        Timer hashTimer = new Timer();
        Timer extTimer = new Timer();
        
        // These local vars ensure we don't skip some computation. 
        long hashSum = 0;
        int alphSum = 0;
        
        int numToks = 0;
        int numPairs = 0;
        for (int round = 0; round<numRounds; round++) {
            numPairs = 0;
            timer = new Timer();
            timer.start();
            lookupTimer = new Timer();
            hashTimer = new Timer();
            extTimer = new Timer();
            Alphabet<String> wordAlphabet = new Alphabet<>();
            Alphabet<String> tagAlphabet = new Alphabet<>();
            
            try {
                for (int i=0; i<sents.size(); i++) {
                    AnnoSentence sent = sents.get(i);
                    numToks += sent.size();
                    
                    Pair[] preds = null;
                    Pair[] args = null;
                    if (OPT == 0) {
                        extTimer.start();
                        preds = new Pair[sents.size()];
                        args = new Pair[sents.size()];
                        for (int pa=0; pa<sent.size(); pa++) {
                            String pWord = sent.getWord(pa);
                            String aWord = sent.getWord(pa);
                            preds[pa] = new Pair("pred", pWord);
                            args[pa] = new Pair("arg", pWord);
                        }
                        extTimer.stop();
                    }
                    ShortArrayList words = null;
                    ShortArrayList tags = null;
                    if (OPT == 5 || OPT == 6 || OPT == 7) {
                        words = new ShortArrayList(sent.size());
                        for (int c=0; c<sent.size(); c++) {
                            words.add(SafeCast.safeIntToShort(wordAlphabet.lookupIndex(sent.getWord(c))));
                        }
                        tags = new ShortArrayList(sent.size());
                        for (int c=0; c<sent.size(); c++) {
                            tags.add(SafeCast.safeIntToShort(tagAlphabet.lookupIndex(sent.getPosTag(c))));
                        }
                    }
                    
                    FeatureVector fv = new FeatureVector(numTemplates);
                    
                    for (int pred=0; pred<sent.size(); pred++) {
                        for (int arg=0; arg<sent.size(); arg++) {
                            numPairs++;
                            for (int t=0; t<numTemplates; t++) {
                                String pWord = sent.getWord(pred);
                                String aWord = sent.getWord(arg);
                                String pPos = sent.getPosTag(pred);
                                String aPos = sent.getPosTag(arg);
                                
                                int featIdx;
                                if (OPT == 0 || OPT == 1 || OPT ==2) {
                                    extTimer.start();
                                    Object featName = null;
                                    if (OPT == 0) {
                                        throw new RuntimeException();
                                    } else if (OPT == 1) {
                                        featName = new Tuple(t, pWord, aWord, pPos, aPos);
                                    } else if (OPT == 2) {
                                        featName = t + "_" + pWord + "_" + aWord + "_" + pPos + "_" + aPos;
                                    }
                                    extTimer.stop();

                                    hashTimer.start();
                                    hashSum += featName.hashCode();
                                    hashTimer.stop();
                                    
                                    lookupTimer.start();
                                    featIdx = alphabet.lookupIndex(featName);
                                    lookupTimer.stop();   
                                } else {
                                    final boolean murmur = true;
                                    
                                    extTimer.start();
                                    byte[] barr = null;
                                    int hash = 0;
                                    if (OPT == 3) {
                                        String data = t + "_" + pWord + "_" + aWord + "_" + pPos + "_" + aPos;
                                        if (murmur) {
                                            hash = MurmurHash3.murmurhash3_x86_32(data, 0, data.length(), 123456789);
                                        } else {
                                            hash = data.hashCode();
                                        }
                                    } else if (OPT == 4) {
                                        StringBuilder sb = new StringBuilder(3+4+pWord.length()+aWord.length()+pPos.length()+aPos.length());
                                        sb.append(t);
                                        sb.append("_");
                                        sb.append(pWord);
                                        sb.append("_");
                                        sb.append(aWord);
                                        sb.append("_");
                                        sb.append(pPos);
                                        sb.append("_");
                                        sb.append(aPos);
                                        String data = sb.toString();
                                        if (murmur) {
                                            hash = MurmurHash3.murmurhash3_x86_32(data, 0, data.length(), 123456789);
                                        } else {
                                            hash = data.hashCode();
                                        }
                                    } else if (OPT == 5) {
                                        ByteBuffer bb = ByteBuffer.allocate(4+4+4+4+4);
                                        bb.putInt((byte)t);
                                        bb.putInt(words.get(pred));
                                        bb.putInt(words.get(arg));
                                        bb.putInt(tags.get(pred));
                                        bb.putInt(tags.get(arg));
                                        barr = bb.array();
                                        if (murmur) {
                                            hash = MurmurHash3.murmurhash3_x86_32(barr, 0, barr.length, 123456789);
                                        } else {
                                            hash = Arrays.hashCode(barr);
                                        }
                                    } else if (OPT == 6) {
                                        // Direct hashing
                                        // From Arrays.hashCode(long[])
                                        //int result = 1;
                                        //for (long element : a) {
                                        //    int elementHash = (int)(element ^ (element >>> 32));
                                        //    result = 31 * result + elementHash;
                                        //}
                                        hash = 1;
                                        hash = 31 * hash + t;
                                        hash = 31 * hash + words.get(pred);
                                        hash = 31 * hash + words.get(arg);
                                        hash = 31 * hash + tags.get(pred);
                                        hash = 31 * hash + tags.get(arg); 
                                    } else if (OPT == 7) {
                                        long element = CreateFKey_WWPP((byte)t, (byte)0, 
                                                words.get(pred), words.get(arg), 
                                                (byte) tags.get(pred), (byte) tags.get(arg));                                        
                                        hash = (int)(element ^ (element >>> 32));
                                    }
                                    extTimer.stop();
                                    
                                    hashTimer.start();
                                    //hash = hash & 0x3ffff; // 2^18 - 1 = 262144
                                    hash = FastMath.mod(hash, 200000);//10000000);
                                    hashSum += hash;
                                    hashTimer.stop();

                                    lookupTimer.start();
                                    final int alpha = 0;
                                    if (alpha == 0) {
                                        featIdx = hash;
                                    } else if (alpha == 1) {
                                        featIdx = alphabet.lookupIndex(hash);
                                    } else {
                                        featIdx = alphabet2.getWithDefault(hash, -1);
                                        if (alphabet.isGrowing() && featIdx == -1) {
                                            featIdx = alphabet2.size();
                                            alphabet2.put(hash, featIdx);
                                        }
                                    }
                                    lookupTimer.stop();
                                }
                                
                                fv.add(featIdx, 1);
                                alphSum += featIdx;
                            }
                        }
                    }
                    if (i % 1000 == 0) {
                        System.out.println("i="+i+" alphabet.size() = " + alphabet.size()+" alphabet2.size() = " + alphabet2.size());
                    }
                }
                timer.stop();
                System.out.println("Num features: " + alphabet.size());
            } catch (OutOfMemoryError e) {
                System.out.println("Num features at OOM: " + alphabet.size());
                break;
            }
        }
        System.out.println("Num pairs: " + numPairs);
        System.out.println("Hash sum: " + hashSum + " Alph sum: " + alphSum);
        System.out.println("Time to extract: " + extTimer.totMs());
        System.out.println("Time to hash: " + hashTimer.totMs());
        System.out.println("Time to lookup: " + lookupTimer.totMs());
        System.out.println("Time total            : " + timer.totMs());
        System.out.println("Toks/sec: " + ((double) numToks/ timer.totSec()));
    }
        
    static final long BYTE_MASK = 0xffL;
    static final long SHORT_MASK = 0xffffL;
    static final long INT_MASK = 0xffffffffL;

    private static long CreateFKey_WWPP(byte type, byte flags, short w1, short w2, 
            byte p1, byte p2) {
        long fkey = (type & BYTE_MASK) | ((flags & BYTE_MASK) << 8);
        fkey |= ((w1 & SHORT_MASK) << 16) | ((w2 & SHORT_MASK) << 32)
                | ((p1 & BYTE_MASK) << 48) | ((p2 & BYTE_MASK) << 56);
        return fkey;
    }
    
    //@Test
    public void testSpeedOfFeatureCreation2() throws IOException {
        // Params
        final int numExamples = 10;
        final int numRounds = 1;

        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
        
        // Add Brown clusters
        BrownClusterTagger bct = new BrownClusterTagger(new BrownClusterTaggerPrm());
        bct.read(new File("./data/bc_out_1000/full.txt_en_1000/bc/paths"));
        bct.annotate(sents);
        
        // Run
        System.out.println("Num sents: " + sents.size());
        
        //List<FeatTemplate> tpls = TemplateSets.getBjorkelundArgUnigramFeatureTemplates();
        //List<FeatTemplate> tpls = TemplateSets.getFromResource(TemplateSets.kooHybridDepFeatsResource);
        List<FeatTemplate> tpls = TemplateSets.getCoarseUnigramSet1();
        System.out.println("Num tpls: " + tpls.size());

        testFeatExtract2(numRounds, tpls, sents, "en", 3, true);
        System.exit(0);
        
        for (int t=0; t<tpls.size(); t++) {
            System.out.println(tpls.get(t));
            testFeatExtract2(numRounds, tpls.subList(t, t+1), sents, "en", 3, true);
        }
    }
    
    private void testFeatExtract2(int numRounds, List<FeatTemplate> tpls, AnnoSentenceCollection sents, String language, final int opt, final boolean lookup) {
        Timer timer = new Timer();
        Timer lookupTimer = new Timer();
        Timer hashTimer = new Timer();
        Timer extTimer = new Timer();
        long hashSum = 0;
        
        CorpusStatisticsPrm prm = new CorpusStatisticsPrm();
        prm.language = language;
        CorpusStatistics cs = new CorpusStatistics(prm);

        int numPairs = 0;
        for (int round = 0; round<numRounds; round++) {
            numPairs = 0;
            timer = new Timer();
            timer.start();
            lookupTimer = new Timer();
            hashTimer = new Timer();
            extTimer = new Timer();
            
            FeatureNames alphabet = new FeatureNames();
            try {
                for (int i=0; i<sents.size(); i++) {
                    AnnoSentence sent = sents.get(i);
                    TemplateFeatureExtractor ext = new TemplateFeatureExtractor(sent, cs);
                    
                    for (int pred=0; pred<sent.size(); pred++) {
                        for (int arg=0; arg<sent.size(); arg++) {
                            extTimer.start();
                            List<String> feats = new ArrayList<String>();
                            ext.addFeatures(tpls, LocalObservations.newPidxCidx(pred, arg), feats);
                            extTimer.stop();
                            
                            numPairs++;
                            for (int t=0; t<feats.size(); t++) {                                

                                extTimer.start();
                                Object featName = feats.get(t);
                                extTimer.stop();
                                
                                hashTimer.start();
                                if (opt == 3) {
                                    String data = (String) featName;
                                    featName = FastMath.mod(MurmurHash3.murmurhash3_x86_32(data, 0, data.length(), 123456789), 200000);
                                }
                                hashSum += featName.hashCode();
                                hashTimer.stop();
                                
                                if (lookup) {
                                    lookupTimer.start();
                                    alphabet.lookupIndex(featName);
                                    lookupTimer.stop();
                                }
                            }
                        }
                    }
                    if (i % 1000 == 0) {
                        System.out.println("alphabet.size() = " + alphabet.size());
                    }
                }
                timer.stop();
                System.out.println("Num features: " + alphabet.size());
            } catch (OutOfMemoryError e) {
                System.out.println("Num features at OOM: " + alphabet.size());
                break;
            }
        }
        System.out.println("Num pairs: " + numPairs);
        System.out.println("Hash sum: " + hashSum);
        System.out.println("Time to extract: " + extTimer.totMs());
        System.out.println("Time to hash: " + hashTimer.totMs());
        System.out.println("Time to lookup: " + lookupTimer.totMs());
        System.out.println("Time total            : " + timer.totMs());
        System.out.println("Seconds per sentence: " + timer.totSec() / sents.size());
        System.out.println("Sentences per second: " + sents.size() / timer.totSec());
    }
    

    // ---------- Unused Legacy Classes --------------
    private interface Extractor {
        Object extract(int t, String pWord, String aWord);
    }
    private static class PairExtractor implements Extractor {
        public Object extract(int t, String pWord, String aWord) {
            return new Pair(t, new Pair(new Pair("pred", pWord), new Pair("arg", aWord)));
        }
    }
    private static class TupleExtractor implements Extractor {
        public Object extract(int t, String pWord, String aWord) {
            return new Tuple(t,"pred", pWord, "arg", aWord);
        }
    }
    private static class StringExtractor implements Extractor {
        public Object extract(int t, String pWord, String aWord) {
            return t + "_pred_" + pWord + "_arg_" + aWord;
        }
    }
    // -----------------------------------------------------
    
    public static void main(String[] args) throws IOException {
        new FeatureCreationSpeedTest().testSpeedOfFeatureCreation();
    }

}

package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.DepTree.Dir;
import edu.jhu.nlp.data.LabeledSpan;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.features.FeaturizedSentence;
import edu.jhu.nlp.features.FeaturizedTokenPair;
import edu.jhu.nlp.features.LocalObservations;
import edu.jhu.nlp.features.TemplateFeatureExtractor;
import edu.jhu.nlp.features.TemplateLanguage.EdgeProperty;
import edu.jhu.nlp.features.TemplateLanguage.TokProperty;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;
import edu.jhu.nlp.relations.RelationsOptions.EntityTypeRepl;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.set.IntHashSet;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Lambda.FnObjDoubleToVoid;
import edu.jhu.util.FeatureNames;

/**
 * Feature extraction for relations.
 * @author mgormley
 */
public class RelObsFe implements ObsFeatureExtractor {

    private static final Logger log = Logger.getLogger(RelObsFe.class);

    private RelationsFactorGraphBuilderPrm prm;
    private AnnoSentence sent;
    private FactorTemplateList fts;

    public RelObsFe(RelationsFactorGraphBuilderPrm prm, AnnoSentence sent, FactorTemplateList fts) {
        this.prm = prm;
        this.sent = sent;
        this.fts = fts;
    }
    
    @Override
    public void init(UFgExample ex, FactorTemplateList fts) {
        return;
    }

    @Override
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        final FeatureNames alphabet = fts.getTemplate(factor).getAlphabet();
        ObjFeatVec<String> obsFeats = calcObsFeatureVectorStrs(factor);
        final FeatureVector fv = new FeatureVector();
        obsFeats.iterate(new FnObjDoubleToVoid<String>() {            
            @Override
            public void call(String fname, double val) {
                int fidx = alphabet.lookupIndex(fname);
                if (fidx != -1) {
                    fv.add(fidx, val);
                }
            }
        });
        return fv;
    }
    
    public ObjFeatVec<String> calcObsFeatureVectorStrs(ObsFeExpFamFactor factor) {
        RelVar rv = (RelVar) factor.getVars().get(0);
        LocalObservations local = LocalObservations.newNe1Ne2(rv.ment1, rv.ment2);
        ObjFeatVec<String> fv = new ObjFeatVec<String>();
        
        // The bias features are used to ensure that at least one feature fires for each variable configuration.
        fv.add("BIAS_FEATURE", 1.0);
        
        // Set entity types to be Brown cluster tags if missing.
        NerMention ne1 = local.getNe1();
        if (ne1.getEntityType() == null) {
            if (RelationsOptions.entityTypeRepl == EntityTypeRepl.BROWN) {
                ne1.setEntityType(sent.getCluster(ne1.getHead()));
            } else {
                ne1.setEntityType("NOTYPE");
            }
        }
        NerMention ne2 = local.getNe2();
        if (ne2.getEntityType() == null) {
            if (RelationsOptions.entityTypeRepl == EntityTypeRepl.BROWN) {
                ne2.setEntityType(sent.getCluster(ne2.getHead()));
            } else {
                ne2.setEntityType("NOTYPE");
            }
        }
        
        if (RelationsOptions.useZhou05Features) {
            addZhou05Features(local, fv);
        }
        
        if (RelationsOptions.useEmbeddingFeatures) {
            addEmbeddingFeatures(local, fv);
        }
        
        // TODO: Add template features. 
        // fe.addFeatures(prm.templates, local, obsFeats);         
        // TemplateFeatureExtractor fe = new TemplateFeatureExtractor(sent, null);

        return fv;
    }
    
    /** Add the features from Sun et al. (2011) and Zhou et al. (2005). */
    private void addZhou05Features(LocalObservations local, ObjFeatVec<String> features) {
        NerMention m1 = local.getNe1();
        NerMention m2 = local.getNe2();
        Span m1span = m1.getSpan();
        Span m2span = m2.getSpan();
        
        FeaturizedSentence fsent = new FeaturizedSentence(sent, null);
        
        // =============================
        // 4.1 Words
        // =============================
        // • WM1: bag-of-words in M1
        // • HM1: head word of M1
        // • WM2: bag-of-words in M2
        // • HM2: head word of M2
        // • HM12: combination of HM1 and HM2        
        StringBuilder wm1 = new StringBuilder("m1WordSet:");
        for (String word : sortUniq(sent.getWords(m1span))) {
            addBinFeat(features, "m1HasWord:" + word);
            wm1.append("_");
            wm1.append(word);
        }
        addBinFeat(features, wm1.toString());        
        String hm1 = "m1HeadWord:" + sent.getWord(m1.getHead());
        addBinFeat(features, hm1); 
        
        StringBuilder wm2 = new StringBuilder("m2WordSet:");
        for (String word : sortUniq(sent.getWords(m2span))) {
            addBinFeat(features, "m2HasWord:" + word);
            wm2.append("_");
            wm2.append(word);
        }
        addBinFeat(features, wm2.toString());        
        String hm2 = "m2HeadWord:" + sent.getWord(m2.getHead());
        addBinFeat(features, hm2); 
        
        String hm12 = combo(hm1, hm2);
        addBinFeat(features, hm12);
        
        // • WBNULL: when no word in between
        // • WBFL: the only word in between when only
        // one word in between
        // • WBF: first word in between when at least two
        // words in between
        // • WBL: last word in between when at least two
        // words in between
        // • WBO: other words in between except first and
        // last words when at least three words in between 
        Span btwn = Span.getSpanBtwn(m1span, m2span);
        if (btwn.size() == 0) {
            addBinFeat(features, "wbNull");
        } else if (btwn.size() == 1) {
            addBinFeat(features, "wbfl:" + sent.getWord(btwn.start()));
        } else if (btwn.size() >= 2) {
            addBinFeat(features, "wbf:" + sent.getWord(btwn.start()));
            addBinFeat(features, "wbl:" + sent.getWord(btwn.end()-1));
            for (int i=btwn.start()+1; i<btwn.end(); i++) {
                addBinFeat(features, "wbo:" + sent.getWord(i));
            }
        }
        
        // • BM1F: first word before M1
        // • BM1L: second word before M1
        // • AM2F: first word after M2
        // • AM2L: second word after M2
        addBinFeat(features, "bm1f:" + fsent.getFeatTok(m1span.start() - 1).getForm());
        addBinFeat(features, "bm1l:" + fsent.getFeatTok(m1span.start() - 2).getForm());
        addBinFeat(features, "am2f:" + fsent.getFeatTok(m1span.end() + 0).getForm());
        addBinFeat(features, "am2l:" + fsent.getFeatTok(m1span.end() + 1).getForm());
        

        // =============================
        // 4.2 Entity Type
        // =============================
        // • ET12: combination of mention entity types
        String et12 = "et12:" + m1.getEntityType() + "_" + m2.getEntityType();
        addBinFeat(features, et12);
        
        // =============================
        // 4.3 Mention Level 
        // =============================
        // • ML12: combination of mention levels
        String ml12 = "ml12:" + m1.getPhraseType() + "_" + m2.getPhraseType();
        addBinFeat(features, ml12);
        
        // =============================
        // 4.4 Overlap
        // =============================
        // This category of features includes:
        // • #MB: number of other mentions in between
        // • #WB: number of words in between
        // • M1>M2 or M1<M2: flag indicating whether
        // M2/M1is included in M1/M2.
        //
        // 1) ET12+M1>M2; 
        // 2) ET12+M1<M2; 
        // 3) HM12+M1>M2; 
        // 4) HM12+M1<M2.
        int numMentsBtwn = getNumBtwn(sent, m1, m2);
        addBinFeat(features, "#MB:"+numMentsBtwn);
        int numWordsBtwn = btwn.size();
        addBinFeat(features, "#WB:"+numWordsBtwn);
        String m1ConM2 = "M1>M2:"+m1span.contains(m2span);
        String m2ConM1 = "M1<M2:"+m2span.contains(m1span);
        addBinFeat(features, m1ConM2);
        addBinFeat(features, m2ConM1);
        
        addBinFeat(features, combo(et12, m1ConM2));
        addBinFeat(features, combo(et12, m2ConM1));
        addBinFeat(features, combo(hm12, m1ConM2));
        addBinFeat(features, combo(hm12, m2ConM1));
        
        // =============================
        // 4.5 Base Phrase Chunking
        // =============================
        // • CPHBNULL when no phrase in between
        // • CPHBFL: the only phrase head when only one
        // phrase in between
        // • CPHBF: first phrase head in between when at
        // least two phrases in between
        // • CPHBL: last phrase head in between when at
        // least two phrase heads in between
        // • CPHBO: other phrase heads in between except
        // first and last phrase heads when at least three
        // phrases in between
        Pair<List<LabeledSpan>, IntArrayList> chunkPair = getSpansFromBIO(sent.getChunks(), true);
        List<LabeledSpan> chunks = chunkPair.get1();
        IntArrayList tokIdxToChunkIdx = chunkPair.get2();
        int c1 = tokIdxToChunkIdx.get(m1.getHead());
        int c2 = tokIdxToChunkIdx.get(m2.getHead());
        int[] chunkHeads = getHeadsOfSpans(chunks, sent.getParents());
        assert c2 >= c1;
        int numChunksBtwn = Math.max(c2 - c1 - 1, 0);
        if (numChunksBtwn == 0) {
            addBinFeat(features, "CPHBNULL");
        } else if (numChunksBtwn == 1) {
            addBinFeat(features, "CPHBFL:"+sent.getWord(chunkHeads[c1+1]));
        } else {
            addBinFeat(features, "CPHBF:"+sent.getWord(chunkHeads[c1+1]));
            addBinFeat(features, "CPHBL:"+sent.getWord(chunkHeads[c2-1]));
            for (int b=c1+2; b<=c2-2; b++) {
                addBinFeat(features, "CPHBO:"+sent.getWord(chunkHeads[b]));
            }
        }
        
        // • CPHBM1F: first phrase head before M1
        // • CPHBM1L: second phrase head before M1
        // • CPHAM2F: first phrase head after M2
        // • CPHAM2L: second phrase head after M2
        String chunkHead;
        chunkHead = (c1-1 < 0) ? "BOS" : sent.getWord(chunkHeads[c1-1]);
        addBinFeat(features, "CPHBM1F:"+chunkHead);
        chunkHead = (c1-2 < 0) ? "BOS" : sent.getWord(chunkHeads[c1-2]);
        addBinFeat(features, "CPHBM1L:"+chunkHead);
        chunkHead = (c2+1 >= chunkHeads.length) ? "EOS" : sent.getWord(chunkHeads[c2+1]);
        addBinFeat(features, "CPHAM2F:"+chunkHead);
        chunkHead = (c2+2 >= chunkHeads.length) ? "EOS" : sent.getWord(chunkHeads[c2+2]);
        addBinFeat(features, "CPHAM2L:"+chunkHead);
        
        // • CPP: path of phrase labels connecting the two
        // mentions in the chunking
        StringBuilder chunkPath = new StringBuilder();
        for (int b=c1+1; b<=c2-1; b++) {
            chunkPath.append(chunks.get(b).getLabel());
            chunkPath.append("_");
        }
        addBinFeat(features, "CPP:"+chunkPath);
        
        // • CPPH: path of phrase labels connecting the two
        // mentions in the chunking augmented with head words,
        // if at most two phrases in between
        if (numChunksBtwn <= 2) {
            StringBuilder chunkHeadPath = new StringBuilder();
            for (int b=c1+1; b<=c2-1; b++) {
                chunkHeadPath.append(chunks.get(b).getLabel());
                chunkHeadPath.append(":");
                chunkHeadPath.append(sent.getWord(chunkHeads[b]));
                chunkHeadPath.append("_");
            }
            addBinFeat(features, "CPPH:"+chunkHeadPath);
        }
        
        // =============================
        // 4.6 Dependency Tree
        // =============================
        // • ET1DW1: combination of the entity type and
        // the dependent word for M1
        // • H1DW1: combination of the head word and the
        // dependent word for M1
        // • ET2DW2: combination of the entity type and
        // the dependent word for M2
        // • H2DW2: combination of the head word and the
        // dependent word for M2
        // • ET12SameNP: combination of ET12 and
        // whether M1 and M2 included in the same NP
        // • ET12SamePP: combination of ET12 and
        // whether M1 and M2 exist in the same PP
        // • ET12SameVP: combination of ET12 and
        // whether M1 and M2 included in the same VP

        String depWord1 = getDepWord(fsent, sent, m1);
        String dw1 = "dw1:"+depWord1;
        addBinFeat(features, combo("et1:"+m1.getEntityType(), dw1));
        addBinFeat(features, combo(hm1, dw1));
        
        String depWord2 = getDepWord(fsent, sent, m2);
        String dw2 = "dw2:"+depWord2;
        addBinFeat(features, combo("et2:"+m2.getEntityType(), dw2));
        addBinFeat(features, combo(hm2, dw2));
        
        String sameNp = "sameNp:"+inSamePhrase(sent, m1.getHead(), m2.getHead(), "NP");
        String samePp = "samePp:"+inSamePhrase(sent, m1.getHead(), m2.getHead(), "PP");
        String sameVp = "sameVp:"+inSamePhrase(sent, m1.getHead(), m2.getHead(), "VP");
        addBinFeat(features, combo(et12, sameNp));
        addBinFeat(features, combo(et12, samePp));
        addBinFeat(features, combo(et12, sameVp));
        
        // =============================
        // 4.7 Parse Tree
        // =============================
        // • PTP: path of phrase labels (removing dupli-
        // cates) connecting M1 and M2 in the parse tree
        // • PTPH: path of phrase labels (removing dupli- cates) 
        // connecting M1 and M2 in the parse tree augmented 
        // with the head word of the top phrase in the path.
        
        String ptp = "ptp:"+ StringUtils.join(getPathSymbols(m1.getHead(), m2.getHead()), ":");
        addBinFeat(features, ptp);
        int lca = getLca(sent.getParents(), m1.getHead(), m2.getHead());
        addBinFeat(features, combo(ptp, "lcaHead:" + fsent.getFeatTok(lca).getForm()));

        // =============================
        // 4.8 Semantic Resources
        // =============================
        // >>> Country Name List
        // • ET1Country: the entity type of M1 when M2 is a country name
        // • CountryET2: the entity type of M2 when M1 is a country name
        //
        // >>> Personal Relative Trigger Word List
        // • ET1SC2: combination of the entity type of M1 and the semantic
        // class of M2 when M2 triggers a personal social subtype.
        // • SC1ET2: combination of the entity type of M2 and the semantic
        // class of M1 when the first mention triggers a personal social
        // subtype.
        
        // TODO: Finish other features.
        

        // ----------- Below are the extra features from Sun et al. (2011). -------------
        // Bigram of the words between the two mentions: This was extracted by both Zhao and
        // Grishman (2005) and Jiang and Zhai (2007).
        //Span btwn = Span.getSpanBtwn(m1span, m2span);
        for (int i=btwn.start(); i<=btwn.end(); i++) {
            addBinFeat(features, "bigrwb:" + fsent.getFeatTok(i-1).getForm() + "_" + sent.getWord(i));
        }
        
        // Patterns: There are three types of patterns: 
        
        // 1) the sequence of the tokens between the two mentions as used in Boschee et al. (2005);
        StringBuilder seq = new StringBuilder();
        for (int i=btwn.start(); i<btwn.end(); i++) {
            seq.append(sent.getWord(i));
            seq.append("_");
        }
        String seqwb = "seqwb:" + seq.toString();
        addBinFeat(features, seqwb);        
        addBinFeat(features, combo(et12, seqwb));
        
        // 2) the sequence of the heads of the constituents between the two mentions as used by
        // Grishman et al. (2005);
        // (basically following the highest cut through the parse tree between the two mentions)

        // Since the above isn't clear, we instead just use the heads of the chunks between the mentions.        
        StringBuilder chunkHeadPath = new StringBuilder();
        for (int b=c1+1; b<=c2-1; b++) {
            chunkHeadPath.append(sent.getWord(chunkHeads[b]));
            chunkHeadPath.append("_");
        }
        String chnkhb = "chnkhb:"+chunkHeadPath;
        addBinFeat(features, chnkhb);
        addBinFeat(features, combo(et12, chnkhb));

        // 3) the shortest dependency path between the two mentions in a dependency tree as adopted
        // by Bunescu and Mooney (2005a).    
        TemplateFeatureExtractor tfe = new TemplateFeatureExtractor(fsent, null);        
        List<Pair<Integer, Dir>> path = fsent.getFeatTokPair(m1.getHead(), m2.getHead()).getDependencyPath();
        if (path != null) {
            List<String> posPath = tfe.getTokPropsForPath(TokProperty.POS, EdgeProperty.DIR, path);
            List<String> wordPath = tfe.getTokPropsForPath(TokProperty.WORD, EdgeProperty.DIR, path);
            List<String> relPath = tfe.getTokPropsForPath(null, EdgeProperty.EDGEREL, path);
            relPath.addAll(tfe.getTokPropsForPath(null, EdgeProperty.DIR, path));
            
            addBinFeat(features, "posdppath:" + StringUtils.join(posPath, "_"));
            addBinFeat(features, "reldppath:" + StringUtils.join(relPath, "_"));
            addBinFeat(features, "worddppath:" + StringUtils.join(wordPath, "_"));
            if (path.size() >= 3) {
                addBinFeat(features, combo(et12, "posdppath:" + StringUtils.join(posPath.subList(1, posPath.size()-1), "_")));
                addBinFeat(features, combo(et12, "reldppath:" + StringUtils.join(relPath.subList(1, relPath.size()-1), "_")));
                addBinFeat(features, combo(et12, "worddppath:" + StringUtils.join(wordPath.subList(1, wordPath.size()-1), "_")));
            }
        }
        
        // Title list: This is tailored for the EMP-ORG type of relations as the head of one of the
        // mentions is usually a title. The features are decoded in a way similar to that of Sun
        // (2009).
        
        // TODO: Finish above title feature.
    }

    // TODO: Move this somewhere else.
    private static int[] getHeadsOfSpans(List<? extends Span> spans, int[] parents) {
        int[] heads = new int[spans.size()];
        for (int i=0; i<spans.size(); i++) {
            heads[i] = getHeadOfSpan(spans.get(i), parents);
        }
        return heads;
    }

    private static int getHeadOfSpan(Span span, int[] parents) {
        int prev= span.start();
        int head = span.start();
        while (head != -1 && span.start() <= head && head < span.end()) {
            prev = head;
            head = parents[head];
        }
        return prev;
    }

    // TODO: Move this somewhere else.
    public static Pair<List<LabeledSpan>,IntArrayList> getSpansFromBIO(List<String> tags, boolean includeOutside) {
        List<LabeledSpan> chunks = new ArrayList<>();
        IntArrayList tokIdxToSpanIdx = new IntArrayList(tags.size());
        int curChunk = -1;
        for (int i=0; i<tags.size(); i++) {
            String label = tags.get(i);
            if (label.startsWith("B-")) {
                // Create a new span with the label.
                LabeledSpan span = new LabeledSpan(i, i+1, label.substring(2));
                curChunk = chunks.size();
                chunks.add(span);
            } else if (label.startsWith("I-")) {
                assert curChunk != -1 : "I- was not preceeded by B- in chunks: " + tags;
                // Update the end of the previous span.
                chunks.get(curChunk).setEnd(i+1);
            } else if (label.equals("O")) {
                if (includeOutside) {
                    // Create a new span with the "Outside" label.
                    LabeledSpan span = new LabeledSpan(i, i+1, label);
                    curChunk = chunks.size();
                    chunks.add(span);
                } else {
                    curChunk = -1;
                }
            } else {
                throw new RuntimeException("Unexpected tag: " + label);
            }
            tokIdxToSpanIdx.add(curChunk);            
        }        
        return new Pair<List<LabeledSpan>,IntArrayList>(chunks, tokIdxToSpanIdx);
    }
    
    public static int getNumBtwn(AnnoSentence sent, NerMention m1, NerMention m2) {
        Span btwn = Span.getSpanBtwn(m1.getSpan(), m2.getSpan());
        int numMentsBtwn = 0;
        for (NerMention m : sent.getNamedEntities()) {
            if (m != m1 && m != m2 && btwn.contains(m.getSpan().start())) {
                numMentsBtwn++;
            }
        }
        return numMentsBtwn;
    }

    private int getLca(int[] parents, int a, int b) {
        IntArrayList a2r = getPathToRoot(parents, a);
        IntArrayList b2r = getPathToRoot(parents, b);
        IntHashSet a2rSet = new IntHashSet(a2r);
        int lca = -1;
        for (int i=0; i<b2r.size(); i++) {
            if (a2rSet.contains(b2r.get(i))) {
                lca = b2r.get(i);
            }
        }
        return lca;
    }

    private IntArrayList getPathToRoot(int[] parents, int a) {
        IntArrayList a2r = new IntArrayList();
        int i=a;
        while (parents[i] >= 0) {
            a2r.add(i);
            i = parents[i];
        }
        return a2r;
    }

    private List<String> getPathSymbols(int m1, int m2) {
        ArrayList<String> path = new ArrayList<>();
        NaryTree tree = sent.getNaryTree();
        List<NaryTree> m1ToRoot = getPathToRoot(tree.getLeafAt(m1));
        List<NaryTree> m2ToRoot = getPathToRoot(tree.getLeafAt(m2));
        NaryTree lca = getLca(m1ToRoot, m2ToRoot);
        
        for (NaryTree node : m1ToRoot) {
            path.add(node.getSymbol());
            if (node == lca) {
                break;
            }
        }
        
        boolean recording = false;
        Collections.reverse(m2ToRoot);
        for (NaryTree node : m2ToRoot) {
            if (recording) {
                path.add(node.getSymbol());
            }
            if (node == lca) {
                recording = true;
            }
        }
        
        return path;
    }

    /** 
     * Returns the parent of the head of the mention.
     * TODO: Maybe email Zhou et al. (2005) to ask what they meant by "dependent word".
     */
    private static String getDepWord(FeaturizedSentence fsent, AnnoSentence sent, NerMention m1) {
        int[] parents = sent.getParents();
        return fsent.getFeatTok(parents[m1.getHead()]).getForm();
    }

    private static boolean inSamePhrase(AnnoSentence sent, int m1, int m2, String phrase) {
        NaryTree tree = sent.getNaryTree();
        List<NaryTree> m1ToRoot = getPathToRoot(tree.getLeafAt(m1));
        List<NaryTree> m2ToRoot = getPathToRoot(tree.getLeafAt(m2));
        List<NaryTree> lcaToRoot = getLcaToRoot(m1ToRoot, m2ToRoot);
        
        return inSamePhrase(phrase, lcaToRoot);
    }

    private static boolean inSamePhrase(String phrase, List<NaryTree> lcaToRoot) {
        for (NaryTree node : lcaToRoot) {
            if (node.getSymbol().startsWith(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static List<NaryTree> getLcaToRoot(List<NaryTree> m1ToRoot, List<NaryTree> m2ToRoot) {
        NaryTree lca = getLca(m1ToRoot, m2ToRoot);
        assert lca != null;
        List<NaryTree> lcaToRoot = getPathToRoot(lca);
        return lcaToRoot;
    }

    private static NaryTree getLca(List<NaryTree> m1ToRoot, List<NaryTree> m2ToRoot) {
        NaryTree lca = null;
        Set<NaryTree> m1ToRootSet = new HashSet<>(m1ToRoot);
        for (NaryTree node : m2ToRoot) {
            if (m1ToRootSet.contains(node)) {
                lca = node;
            }
        }
        return lca;
    }
    
    private static List<NaryTree> getPathToRoot(NaryTree node) {
        List<NaryTree> path = new ArrayList<>();
        while (node != null) {
            path.add(node);
            node = node.getParent();
        }
        return path;
    }

    private String combo(String hm1, String hm2) {
        return hm1 + "_" + hm2;
    }

    private static Collection<String> sortUniq(List<String> words) {
        ArrayList<String> uniq = new ArrayList<>(new HashSet<>(words));
        Collections.sort(uniq);
        return words;
    }

    private void addEmbeddingFeatures(LocalObservations local, ObjFeatVec<String> fv) {
        //  - Per word, we have various features, such as whether a word is in between
        //    the entities or on the dependency path between them.
        //    - For each of the above features, if the feature fires, set the
        //      values to the embedding for that word.
        //    - For each of the above features, if the feature fires, set the
        //      values to the embedding for the word's super sense tag.
        NerMention m1 = local.getNe1();
        NerMention m2 = local.getNe2();
        Span m1span = m1.getSpan();
        Span m2span = m2.getSpan();
        
        FeaturizedSentence fsent = new FeaturizedSentence(sent, null);
        
        String ne1 = m1.getEntityType();
        String ne2 = m2.getEntityType();
        String ne1ne2 = ne1 + ne2;
                
        switch (RelationsOptions.embFeatType) {
        case FULL:            
            //     - chunk_head
            //     - chunk_head+ne1
            //     - chunk_head+ne2
            //     - chunk_head+ne1+ne2
            Pair<List<LabeledSpan>, IntArrayList> chunkPair = getSpansFromBIO(sent.getChunks(), true);
            List<LabeledSpan> chunks = chunkPair.get1();
            IntArrayList tokIdxToChunkIdx = chunkPair.get2();
            int c1 = tokIdxToChunkIdx.get(m1.getHead());
            int c2 = tokIdxToChunkIdx.get(m2.getHead());
            int[] chunkHeads = getHeadsOfSpans(chunks, sent.getParents());
            for (int b=c1+1; b<=c2-1; b++) {
                int i = chunkHeads[b];
                addEmbFeat("chunk_head", i, fv);
                addEmbFeat("chunk_head-t1"+ne1, i, fv);
                addEmbFeat("chunk_head-t2"+ne2, i, fv);
                addEmbFeat("chunk_head-t1t2"+ne1ne2, i, fv);
            }
            
        case FULL_NO_CHUNKS:
            //     - in_between: is the word in between entities
            //     - in_between+ne1 if in_between = T: ne1 is the entity type
            //     - in_between+ne2 if in_between = T
            //     - in_between+ne1+ne2 if in_between = T
            Span btwn = Span.getSpanBtwn(m1span, m2span);
            for (int i=btwn.start(); i<btwn.end(); i++) {
                addEmbFeat("in_between", i, fv);
                addEmbFeat("in_between-t1"+ne1, i, fv);
                addEmbFeat("in_between-t2"+ne2, i, fv);
                addEmbFeat("in_between-t1t2"+ne1ne2, i, fv);
            }
    
            //     - on_path
            //     - on_path+ne1 if on_path = T
            //     - on_path+ne2 if on_path = T
            //     - on_path+ne1+ne2 if on_path = T
            FeaturizedTokenPair ftp = fsent.getFeatTokPair(m1.getHead(), m2.getHead());        
            List<Pair<Integer, Dir>> depPath = ftp.getDependencyPath();
            if (depPath != null) {
                for (Pair<Integer,DepTree.Dir> pair : depPath) {
                    int i = pair.get1();
                    addEmbFeat("on_path", i, fv);
                    addEmbFeat("on_path-t1"+ne1, i, fv);
                    addEmbFeat("on_path-t2"+ne2, i, fv);
                    addEmbFeat("on_path-t1t2"+ne1ne2, i, fv);
                }
            } else {
                log.warn("No dependency path between mention heads");
            }
            
            //     - -1_ne1: immediately to the left of the ne1 head
            //     - +1_ne1: immediately to the right of the ne1 head
            //     - -2_ne1: two to the left of the ne1 head
            //     - +2_ne1: two to the right of the ne1 head
            addEmbFeat("-1_ne1", m1.getHead()-1, fv);
            addEmbFeat("+1_ne1", m1.getHead()+1, fv);
            addEmbFeat("-2_ne1", m1.getHead()-2, fv);
            addEmbFeat("+2_ne1", m1.getHead()+2, fv);
            
            //     - -1_ne2: immediately to the left of the ne1 head
            //     - +1_ne2: immediately to the right of the ne1 head
            //     - -2_ne2: two to the left of the ne1 head
            //     - +2_ne2: two to the right of the ne1 head
            addEmbFeat("-1_ne2", m2.getHead()-1, fv);
            addEmbFeat("+1_ne2", m2.getHead()+1, fv);
            addEmbFeat("-2_ne2", m2.getHead()-2, fv);
            addEmbFeat("+2_ne2", m2.getHead()+2, fv);
                    
        case HEAD_TYPE:
            //     - ne1_head+ne1
            //     - ne1_head+ne2
            //     - ne1_head+ne1+ne2
            addEmbFeat("ne1_head-t1"+ne1,    m1.getHead(), fv);
            addEmbFeat("ne1_head-t2"+ne2,    m1.getHead(), fv);
            addEmbFeat("ne1_head-t1t2"+ne1ne2, m1.getHead(), fv);
            
            //     - ne2_head+ne1
            //     - ne2_head+ne2
            //     - ne2_head+ne1+ne2
            addEmbFeat("ne2_head-t1"+ne1,    m2.getHead(), fv);
            addEmbFeat("ne2_head-t2"+ne2,    m2.getHead(), fv);
            addEmbFeat("ne2_head-t1t2"+ne1ne2, m2.getHead(), fv);
            
        case HEAD_ONLY:
            //     - ne1_head: true if is the head of the first entity
            addEmbFeat("ne1_head",        m1.getHead(), fv);
            //     - ne2_head: true if is the head of the second entity
            addEmbFeat("ne2_head",        m2.getHead(), fv);
        }
    }

    private void addBinFeat(ObjFeatVec<String> fv, String fname) {
        fv.add(fname, 1.0);
    }
    
    private void addEmbFeat(String fname, int i, ObjFeatVec<String> fv) {
        if (i < 0 || sent.size() <= i) {
            return;
        }
        double[] embed = sent.getEmbed(i);
        if (embed != null) {
            for (int d=0; d<embed.length; d++) {
                fv.add(fname+"_"+d, embed[d]);
            }
        }
    }
    
}
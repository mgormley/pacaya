package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.features.FeaturizedSentence;
import edu.jhu.nlp.features.LocalObservations;
import edu.jhu.nlp.features.TemplateFeatureExtractor;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.set.IntHashSet;
import edu.jhu.util.FeatureNames;

/**
 * Feature extraction for relations.
 * @author mgormley
 */
public class RelObsFe implements ObsFeatureExtractor {
    
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
        TemplateFeatureExtractor fe = new TemplateFeatureExtractor(sent, null);
        List<String> obsFeats = new ArrayList<>();
        RelVar rv = (RelVar) factor.getVars().get(0);
        LocalObservations local = LocalObservations.newNe1Ne2(rv.ment1, rv.ment2);
        // TODO: fe.addFeatures(prm.templates, local, obsFeats);

        addFeatures(local, obsFeats);
        
        FeatureNames alphabet = fts.getTemplate(factor).getAlphabet();
        
        // The bias features are used to ensure that at least one feature fires for each variable configuration.
        ArrayList<String> biasFeats = new ArrayList<String>(1);
        biasFeats.add("BIAS_FEATURE");
        // Add the bias features.
        FeatureVector fv = new FeatureVector(1 + obsFeats.size());
        FeatureUtils.addFeatures(biasFeats, alphabet, fv, true, prm.featureHashMod);
        
        // Add the other features.
        FeatureUtils.addFeatures(obsFeats, alphabet, fv, false, prm.featureHashMod);
        
        return fv;
    }

    private void addFeatures(LocalObservations local, List<String> features) {
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
            features.add("m1HasWord:" + word);
            wm1.append("_");
            wm1.append(word);
        }
        features.add(wm1.toString());        
        String hm1 = "m1HeadWord:" + sent.getWord(m1.getHead());
        features.add(hm1); 
        
        StringBuilder wm2 = new StringBuilder("m2WordSet:");
        for (String word : sortUniq(sent.getWords(m2span))) {
            features.add("m2HasWord:" + word);
            wm2.append("_");
            wm2.append(word);
        }
        features.add(wm2.toString());        
        String hm2 = "m2HeadWord:" + sent.getWord(m2.getHead());
        features.add(hm2); 
        
        String hm12 = combo(hm1, hm2);
        features.add(hm12);
        
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
            features.add("wbNull");
        } else if (btwn.size() == 1) {
            features.add("wbfl:" + sent.getWord(btwn.start()));
        } else if (btwn.size() >= 2) {
            features.add("wbf:" + sent.getWord(btwn.start()));
            features.add("wbl:" + sent.getWord(btwn.end()-1));
            for (int i=btwn.start()+1; i<btwn.end(); i++) {
                features.add("wbo:" + sent.getWord(i));
            }
        }
        
        // • BM1F: first word before M1
        // • BM1L: second word before M1
        // • AM2F: first word after M2
        // • AM2L: second word after M2
        features.add("bm1f:" + fsent.getFeatTok(m1span.start() - 1).getForm());
        features.add("bm1l:" + fsent.getFeatTok(m1span.start() - 2).getForm());
        features.add("am2f:" + fsent.getFeatTok(m1span.end() + 0).getForm());
        features.add("am2l:" + fsent.getFeatTok(m1span.end() + 1).getForm());
        

        // =============================
        // 4.2 Entity Type
        // =============================
        // • ET12: combination of mention entity types
        String et12 = "et12:" + m1.getEntityType() + "_" + m2.getEntityType();
        features.add(et12);
        
        // =============================
        // 4.3 Mention Level 
        // =============================
        // • ML12: combination of mention levels
        String ml12 = "ml12:" + m1.getPhraseType() + "_" + m2.getPhraseType();
        features.add(ml12);
        

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
        int numMentsBtwn = 0;
        for (NerMention m : sent.getNamedEntities()) {
            if (m != m1 && m != m2 && btwn.contains(m.getSpan().start())) {
                numMentsBtwn++;
            }
        }
        features.add("#MB:"+numMentsBtwn);
        int numWordsBtwn = btwn.size();
        features.add("#WB:"+numWordsBtwn);
        String m1ConM2 = "M1>M2:"+m1span.contains(m2span);
        String m2ConM1 = "M1<M2:"+m2span.contains(m1span);
        features.add(m1ConM2);
        features.add(m2ConM1);
        
        features.add(combo(et12, m1ConM2));
        features.add(combo(et12, m2ConM1));
        features.add(combo(hm12, m1ConM2));
        features.add(combo(hm12, m2ConM1));
        
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
        // 
        // • CPHBM1F: first phrase head before M1
        // • CPHBM1L: second phrase head before M1
        // • CPHAM2F: first phrase head after M2
        // • CPHAM2F: second phrase head after M2
        // • CPP: path of phrase labels connecting the two
        // mentions in the chunking
        //
        // • CPPH: path of phrase labels connecting the two
        // mentions in the chunking augmented with head words,
        // if at most two phrases in between
        
        // TODO: Finish Base Phrase Chunking features.
        
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
        features.add(combo("et1:"+m1.getEntityType(), dw1));
        features.add(combo(hm1, dw1));
        
        String depWord2 = getDepWord(fsent, sent, m2);
        String dw2 = "dw2:"+depWord2;
        features.add(combo("et2:"+m2.getEntityType(), dw2));
        features.add(combo(hm2, dw2));
        
        String sameNp = "sameNp:"+inSamePhrase(sent, m1.getHead(), m2.getHead(), "NP");
        String samePp = "samePp:"+inSamePhrase(sent, m1.getHead(), m2.getHead(), "PP");
        String sameVp = "sameVp:"+inSamePhrase(sent, m1.getHead(), m2.getHead(), "VP");
        features.add(combo(et12, sameNp));
        features.add(combo(et12, samePp));
        features.add(combo(et12, sameVp));
        
        // =============================
        // 4.7 Parse Tree
        // =============================
        // • PTP: path of phrase labels (removing dupli-
        // cates) connecting M1 and M2 in the parse tree
        // • PTPH: path of phrase labels (removing dupli- cates) 
        // connecting M1 and M2 in the parse tree augmented 
        // with the head word of the top phrase in the path.
        
        String ptp = "ptp:"+ StringUtils.join(getPathSymbols(m1.getHead(), m2.getHead()), ":");
        features.add(ptp);
        int lca = getLca(sent.getParents(), m1.getHead(), m2.getHead());
        features.add(combo(ptp, "lcaHead:" + fsent.getFeatTok(lca).getForm()));

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


    private boolean usePosTagFeatures = true;
    private boolean useLemmaFeatures = false;
    
    /**
     * Adds a set of features based on a span of tokens.
     */
    private void addExtentFeatures(List<String> features, Span extent, String prefix) {
        features.add(String.format("%sAllWords:%s", prefix, sent.getWordsStr(extent)));
        if (useLemmaFeatures) {
            features.add(String.format("%sAllLemmas:%s", prefix, sent.getLemmasStr(extent)));
        }
        if (usePosTagFeatures) {
            features.add(String.format("%sAllPosTags:%s", prefix, sent.getPosTagsStr(extent)));
            features.add(String.format("%sAllWordPosTags:%s", prefix, sent.getWordPosTagsStr(extent)));
        }

        for (String word : sortUniq(sent.getWords(extent))) {
            features.add(String.format("%sHasWord:%s", prefix, word));
        }
        for (String lemma : sortUniq(sent.getLemmas(extent))) {
            features.add(String.format("%sHasLemma:%s", prefix, lemma));
        }
        if (usePosTagFeatures) {
            for (String tag : sortUniq(sent.getPosTags(extent))) {
                features.add(String.format("%sHasPosTag:%s", prefix, tag));
            }
            for (String wt : sortUniq(sent.getWordPosTags(extent))) {
                features.add(String.format("%sHasWordPosTag:%s", prefix, wt));
            }
        }
    }

    private static Collection<String> sortUniq(List<String> words) {
        ArrayList<String> uniq = new ArrayList<>(new HashSet<>(words));
        Collections.sort(uniq);
        return words;
    }
    
}
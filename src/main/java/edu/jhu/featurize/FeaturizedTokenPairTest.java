package edu.jhu.featurize;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceTest;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;

public class FeaturizedTokenPairTest {

    @Test
    public void testZhaoPathFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        AnnoSentence simpleSent = sent.toAnnoSentence(csPrm.useGoldSyntax);

        FeaturizedToken zhaoPred = new FeaturizedToken(1, simpleSent);
        FeaturizedToken zhaoArg = new FeaturizedToken(0, simpleSent);
        FeaturizedTokenPair zhaoLink = new FeaturizedTokenPair(1, 0, zhaoPred, zhaoArg, simpleSent);
        List<Pair<Integer, Dir>> desiredDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        desiredDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        desiredDpPathShare.add(new Pair<Integer, Dir>(-1, Dir.NONE));
        List<Pair<Integer, Dir>> observedDpPathShare = zhaoLink.getDpPathShare();
        System.out.println(observedDpPathShare);
        assertEquals(desiredDpPathShare, observedDpPathShare);
    }

    @Test
    public void testZhaoObjectPathSentence1() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        AnnoSentence simpleSent = sent.toAnnoSentence(csPrm.useGoldSyntax);

        // Example indices.
        FeaturizedToken zhaoPred = new FeaturizedToken(3, simpleSent);
        FeaturizedToken zhaoArg = new FeaturizedToken(4, simpleSent);
        FeaturizedTokenPair zhaoLink = new FeaturizedTokenPair(3, 4, zhaoPred, zhaoArg, simpleSent);

        // Path between two indices.
        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(1, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(4, Dir.NONE));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath, seenPath);

        // Shared path to root for two indices.
        List<Pair<Integer, Dir>> dpPathShare = zhaoLink.getDpPathShare();
        ArrayList<Pair<Integer, Dir>> expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        expectedDpPathShare.add(new Pair<Integer, Dir>(-1, Dir.NONE));
        assertEquals(dpPathShare, expectedDpPathShare);

        // New example indices.
        zhaoPred = new FeaturizedToken(0, simpleSent);
        zhaoArg = new FeaturizedToken(4, simpleSent);
        zhaoLink = new FeaturizedTokenPair(0, 4, zhaoPred, zhaoArg, simpleSent);

        // Path between two indices.
        expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(0, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(1, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(4, Dir.NONE));
        seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath, seenPath);

        // Shared path to root for two indices.
        dpPathShare = zhaoLink.getDpPathShare();
        expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        expectedDpPathShare.add(new Pair<Integer, Dir>(-1, Dir.NONE));
        assertEquals(dpPathShare, expectedDpPathShare);

        // Line path (consecutive indices between two).
        ArrayList<Integer> linePath = zhaoLink.getLinePath();
        ArrayList<Integer> expectedLinePath = new ArrayList<Integer>();
        expectedLinePath.add(0);
        expectedLinePath.add(1);
        expectedLinePath.add(2);
        expectedLinePath.add(3);
        expectedLinePath.add(4);
        assertEquals(expectedLinePath, linePath);
    }

    @Test
    public void testZhaoObjectPathSentence2() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        AnnoSentence simpleSent = sent.toAnnoSentence(csPrm.useGoldSyntax);

        // Example indices.
        FeaturizedToken zhaoPred = new FeaturizedToken(3, simpleSent);
        FeaturizedToken zhaoArg = new FeaturizedToken(4, simpleSent);
        FeaturizedTokenPair zhaoLink = new FeaturizedTokenPair(3, 4, zhaoPred, zhaoArg, simpleSent);

        // Path between two indices.
        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(4, Dir.NONE));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath, seenPath);

        // Shared path to root for two indices.
        List<Pair<Integer, Dir>> dpPathShare = zhaoLink.getDpPathShare();
        ArrayList<Pair<Integer, Dir>> expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(5, Dir.UP));
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        expectedDpPathShare.add(new Pair<Integer, Dir>(-1, Dir.NONE));
        assertEquals(dpPathShare, expectedDpPathShare);

        // New example indices.
        zhaoPred = new FeaturizedToken(0, simpleSent);
        zhaoArg = new FeaturizedToken(4, simpleSent);
        zhaoLink = new FeaturizedTokenPair(0, 4, zhaoPred, zhaoArg, simpleSent);

        // Path between two indices.
        expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(0, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(1, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(4, Dir.NONE));
        seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath, seenPath);

        // Shared path to root for two indices.
        dpPathShare = zhaoLink.getDpPathShare();
        expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        expectedDpPathShare.add(new Pair<Integer, Dir>(-1, Dir.NONE));
        assertEquals(dpPathShare, expectedDpPathShare);

        // Line path (consecutive indices between two).
        ArrayList<Integer> linePath = zhaoLink.getLinePath();
        ArrayList<Integer> expectedLinePath = new ArrayList<Integer>();
        expectedLinePath.add(0);
        expectedLinePath.add(1);
        expectedLinePath.add(2);
        expectedLinePath.add(3);
        expectedLinePath.add(4);
        assertEquals(expectedLinePath, linePath);
    }

    @Test
    public void testZhaoObjectPathSentence2PredictedSyntax() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = false;
        AnnoSentence simpleSent = sent.toAnnoSentence(csPrm.useGoldSyntax);

        FeaturizedToken zhaoPred = new FeaturizedToken(3, simpleSent);
        FeaturizedToken zhaoArg = new FeaturizedToken(4, simpleSent);
        FeaturizedTokenPair zhaoLink = new FeaturizedTokenPair(3, 4, zhaoPred, zhaoArg, simpleSent);

        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(4, Dir.NONE));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath, seenPath);

        zhaoPred = new FeaturizedToken(0, simpleSent);
        zhaoArg = new FeaturizedToken(4, simpleSent);
        zhaoLink = new FeaturizedTokenPair(0, 4, zhaoPred, zhaoArg, simpleSent);

        expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(0, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(6, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(4, Dir.NONE));
        seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath, seenPath);
    }

    public static CoNLL09Sentence getSpanishConll09Sentence1() {
        return SentFeatureExtractorTest.getSpanishConll09Sentence1();
    }

    public static CoNLL09Sentence getSpanishConll09Sentence2() {
        return SentFeatureExtractorTest.getSpanishConll09Sentence2();
    }

    public static CoNLL09Sentence getDogConll09Sentence() {
        return AnnoSentenceTest.getDogConll09Sentence();
    }

}

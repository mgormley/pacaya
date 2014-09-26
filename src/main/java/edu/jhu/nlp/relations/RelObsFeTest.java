package edu.jhu.nlp.relations;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.data.LabeledSpan;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;

public class RelObsFeTest {

    @Test
    public void testGetSpansFromBIONoOutside() {
        List<String> tags = Lists.getList("B-NP", "I-NP", "I-NP", "O", "B-NP", "B-VP", "I-VP", "O", "O", "B-PP");
        Pair<List<LabeledSpan>, IntArrayList> pair = RelObsFe.getSpansFromBIO(tags, false);
        List<LabeledSpan> spans = pair.get1();
        IntArrayList tokIdxToSpanIdx = pair.get2();
        
        assertEquals(new LabeledSpan(0, 3, "NP"), spans.get(0));
        assertEquals(new LabeledSpan(4, 5, "NP"), spans.get(1));
        assertEquals(new LabeledSpan(5, 7, "VP"), spans.get(2));
        assertEquals(new LabeledSpan(9, 10, "PP"), spans.get(3));
        
        assertArrayEquals(new int[]{0, 0, 0, -1, 1, 2, 2, -1, -1, 3}, tokIdxToSpanIdx.toNativeArray());
    }
    

    @Test
    public void testGetSpansFromBIOWithOutside() {
        List<String> tags = Lists.getList("B-NP", "I-NP", "I-NP", "O", "B-NP", "B-VP", "I-VP", "O", "O", "B-PP");
        Pair<List<LabeledSpan>, IntArrayList> pair = RelObsFe.getSpansFromBIO(tags, true);
        List<LabeledSpan> spans = pair.get1();
        IntArrayList tokIdxToSpanIdx = pair.get2();
        
        assertEquals(new LabeledSpan(0, 3, "NP"), spans.get(0));
        assertEquals(new LabeledSpan(3, 4, "O"), spans.get(1));
        assertEquals(new LabeledSpan(4, 5, "NP"), spans.get(2));
        assertEquals(new LabeledSpan(5, 7, "VP"), spans.get(3));
        assertEquals(new LabeledSpan(7, 8, "O"), spans.get(4));
        assertEquals(new LabeledSpan(8, 9, "O"), spans.get(5));
        assertEquals(new LabeledSpan(9, 10, "PP"), spans.get(6));
        
        assertArrayEquals(new int[]{0, 0, 0, 1, 2, 3, 3, 4, 5, 6}, tokIdxToSpanIdx.toNativeArray());
    }

}

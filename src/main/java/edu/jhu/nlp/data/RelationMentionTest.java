package edu.jhu.nlp.data;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;


public class RelationMentionTest {

    @Test
    public void testGetNerOrderedArgs() throws Exception {
        NerMention ne1 = new NerMention(new Span(0, 1), "MAMMAL", "DOG", "noun", 0, "uuid1");
        NerMention ne2 = new NerMention(new Span(2, 4), "MAMMAL", "CAT", "noun", 3, "uuid2");
        List<Pair<String, NerMention>> args = Lists.getList(new Pair<>("arg-2", ne2), new Pair<>("arg-1", ne1));
        RelationMention rm = new RelationMention("type", "subType", 
                args, 
                null);
        List<Pair<String, NerMention>> oargs = rm.getNerOrderedArgs();
        assertEquals(args.get(1), oargs.get(0));
        assertEquals(args.get(0), oargs.get(1));
    }

        
    
}

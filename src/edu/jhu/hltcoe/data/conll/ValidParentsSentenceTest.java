package edu.jhu.hltcoe.data.conll;

import org.junit.Test;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.JUnitUtils;

public class ValidParentsSentenceTest {

    @Test
    public void testGetValidParents() {
        //        tokens.add(getTok("_ vice A1 _"));
        //        tokens.add(getTok("Y pres A0 A1 "));
        //        tokens.add(getTok("Y says _ _"));
        //        tokens.add(getTok("_ jump _ A2"));    
        CoNLL09Sentence sent = SrlGraphTest.getSimpleCoNLL09Sentence();
        ValidParentsSentence sentence = new ValidParentsSentence(sent, new Alphabet<Label>());
        boolean[][] validParents = sentence.getValidParents();
                
        JUnitUtils.assertArrayEquals(new boolean[]{ false, false, true, false }, sentence.getValidRoot());
        
        JUnitUtils.assertArrayEquals(new boolean[]{ false, true, false, false }, validParents[0]);
        JUnitUtils.assertArrayEquals(new boolean[]{ false, true, true, false }, validParents[1]);
        JUnitUtils.assertArrayEquals(new boolean[]{ true, true, true, true }, validParents[2]);
        JUnitUtils.assertArrayEquals(new boolean[]{ false, false, true, false }, validParents[3]);
    }

}

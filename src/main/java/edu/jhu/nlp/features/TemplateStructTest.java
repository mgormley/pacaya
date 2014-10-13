package edu.jhu.nlp.features;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.nlp.features.TemplateLanguage.Position;
import edu.jhu.nlp.features.TemplateLanguage.PositionList;
import edu.jhu.nlp.features.TemplateLanguage.PositionModifier;

public class TemplateStructTest {

    @Test
    public void testReadNoNesting() {
        TemplateStruct s0 = TemplateStruct.readTree("p");
        assertEquals("p", s0.name);
        assertEquals("p", s0.full);
        assertEquals(Position.PARENT, s0.type);
        assertEquals(0, s0.deps.size());
    }

    @Test
    public void testReadOneLevel() {
        TemplateStruct s1 = TemplateStruct.readTree("head(p)");
        assertEquals("head", s1.name);
        assertEquals("head(p)", s1.full);
        assertEquals(PositionModifier.HEAD, s1.type);
        assertEquals(1, s1.deps.size());
        
        TemplateStruct s0 = s1.deps.get(0);
        assertEquals("p", s0.name);
        assertEquals("p", s0.full);
        assertEquals(Position.PARENT, s0.type);
        assertEquals(0, s0.deps.size());
    }

    @Test
    public void testReadOneLevelTwoArgs() {
        TemplateStruct s1 = TemplateStruct.readTree("foo(p,c)");
        assertEquals("foo", s1.name);
        assertEquals("foo(p,c)", s1.full);
        assertEquals(2, s1.deps.size());
        
        TemplateStruct s00 = s1.deps.get(0);
        assertEquals("p", s00.name);
        assertEquals("p", s00.full);
        assertEquals(0, s00.deps.size());
        
        TemplateStruct s01 = s1.deps.get(1);
        assertEquals("c", s01.name);
        assertEquals("c", s01.full);
        assertEquals(0, s01.deps.size());
    }
    
    @Test
    public void testReadOneFullDescription() {
        TemplateStruct s1 = TemplateStruct.readTree("line(p,c)");
        assertEquals("line", s1.name);
        assertEquals("line(p,c)", s1.full);
        assertEquals(PositionList.LINE_P_C, s1.type);
        assertEquals(0, s1.deps.size());
    }
    
    @Test
    public void testReadTwoLevel() {
        TemplateStruct s2 = TemplateStruct.readTree("word(head(p))");
        assertEquals("word", s2.name);
        assertEquals("word(head(p))", s2.full);
        assertEquals(1, s2.deps.size());
        
        TemplateStruct s1 = s2.deps.get(0);
        assertEquals("head", s1.name);
        assertEquals("head(p)", s1.full);
        assertEquals(1, s1.deps.size());
        
        TemplateStruct s0 = s1.deps.get(0);
        assertEquals("p", s0.name);
        assertEquals("p", s0.full);
        assertEquals(0, s0.deps.size());
    }
    
    @Test
    public void testReadTwoLevelTwoArgs() {
        TemplateStruct s1 = TemplateStruct.readTree("line(head(p),1(c))");
        assertEquals("line", s1.name);
        assertEquals("line(head(p),1(c))", s1.full);
        assertEquals(2, s1.deps.size());
        
        TemplateStruct s00 = s1.deps.get(0);
        assertEquals("head", s00.name);
        assertEquals("head(p)", s00.full);
        assertEquals(1, s00.deps.size());
        
        TemplateStruct s01 = s1.deps.get(1);
        assertEquals("1", s01.name);
        assertEquals("1(c)", s01.full);
        assertEquals(1, s01.deps.size());
        
        TemplateStruct s000 = s00.deps.get(0);
        assertEquals("p", s000.name);
        assertEquals("p", s000.full);
        assertEquals(0, s000.deps.size());
        
        TemplateStruct s010 = s01.deps.get(0);
        assertEquals("c", s010.name);
        assertEquals("c", s010.full);
        assertEquals(0, s010.deps.size());
    }
    

    @Test
    public void testReadTwoLevelTwoArgsWithWhitespace() {
        TemplateStruct s1 = TemplateStruct.readTree("line ( head ( p ) , 1( c ) )");
        assertEquals("line", s1.name);
        assertEquals("line(head(p),1(c))", s1.full);
        assertEquals(2, s1.deps.size());
        
        TemplateStruct s00 = s1.deps.get(0);
        assertEquals("head", s00.name);
        assertEquals("head(p)", s00.full);
        assertEquals(1, s00.deps.size());
        
        TemplateStruct s01 = s1.deps.get(1);
        assertEquals("1", s01.name);
        assertEquals("1(c)", s01.full);
        assertEquals(1, s01.deps.size());
        
        TemplateStruct s000 = s00.deps.get(0);
        assertEquals("p", s000.name);
        assertEquals("p", s000.full);
        assertEquals(0, s000.deps.size());
        
        TemplateStruct s010 = s01.deps.get(0);
        assertEquals("c", s010.name);
        assertEquals("c", s010.full);
        assertEquals(0, s010.deps.size());
    }
    
    @Test
    public void testSomeDepth() {
        TemplateStruct.readTree("line(head(foo(p,d),p),1(bar(c),baz(d),bag(e)))");
    }

    @Test(expected=RuntimeException.class)
    public void testMissingParen() {
        TemplateStruct.readTree("line(p");
    }

    @Test(expected=RuntimeException.class)
    public void testMissingParen2() {
        TemplateStruct.readTree("word(line(p)");
    }
    
    @Test(expected=RuntimeException.class)
    public void testExtraOpenParen() {
        TemplateStruct.readTree("word((line(p))");
    }
    
    @Test(expected=RuntimeException.class)
    public void testExtraCloseParen() {
        TemplateStruct.readTree("word(line(p)))");
    }
    
}

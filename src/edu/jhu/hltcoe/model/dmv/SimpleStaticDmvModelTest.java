package edu.jhu.hltcoe.model.dmv;

import org.junit.Test;

public class SimpleStaticDmvModelTest {

    @Test
    public void testTwoPosTagDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getTwoPosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        System.out.println(generator.getTreebank(10));
        System.out.println(dmvModel);
    }
    
    @Test
    public void testThreePosTagDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, System.currentTimeMillis());
        System.out.println(generator.getTreebank(10));
        System.out.println(dmvModel);
    }

}

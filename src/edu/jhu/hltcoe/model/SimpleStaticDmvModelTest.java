package edu.jhu.hltcoe.model;

import org.junit.Test;

public class SimpleStaticDmvModelTest {

    @Test
    public void testSimplestDmvModel() {
        DmvModel dmvModel = SimpleStaticDmvModel.getSimplestInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel);
        System.out.println(generator.getTreebank(10));
        System.out.println(dmvModel);
    }

}

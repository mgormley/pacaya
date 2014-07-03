package edu.jhu.autodiff;

import static org.junit.Assert.*;

import org.junit.Test;

public class SumTest {

    @Test
    public void testGradient() {
        checkGradient(new Sum());
    }

    private void checkGradient(Module module) {
        
        //module.forward(input);
    }

}

package edu.jhu.pacaya.sch.util;

import static edu.jhu.pacaya.sch.util.TestUtils.checkThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test; 

public class TestUtilsTest {

    private void throwUnsupported() {
        throw new UnsupportedOperationException();
    }

    private void throwRuntime() { 
        throw new RuntimeException();
    }

    private void throwIndexOutOfBounds() { 
        throw new IndexOutOfBoundsException();
    }

    @Test
    public void test() {
        new TestUtils(); // HACK: just to keep coverage happy

        assertFalse(TestUtils.checkThrows(() -> {}, UnsupportedOperationException.class));
        assertFalse(checkThrows(() -> {}, Throwable.class));
        assertTrue(checkThrows(() -> throwUnsupported(), UnsupportedOperationException.class));
        assertTrue(checkThrows(() -> throwIndexOutOfBounds(), IndexOutOfBoundsException.class));
        assertTrue(checkThrows(() -> throwUnsupported(), RuntimeException.class));
        assertTrue(checkThrows(() -> throwRuntime(), RuntimeException.class));
        assertFalse(checkThrows(() -> throwRuntime(), UnsupportedOperationException.class));
        assertFalse(checkThrows(() -> throwUnsupported(), IndexOutOfBoundsException.class));
        assertFalse(checkThrows(() -> throwIndexOutOfBounds(), UnsupportedOperationException.class));
    }


}

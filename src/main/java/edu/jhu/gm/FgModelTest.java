package edu.jhu.gm;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

import edu.jhu.util.Files;
import edu.jhu.util.Utilities;

public class FgModelTest {

    @Test
    public void testIsSerializable() throws IOException {
        try {
            // Just test that no exception is thrown.
            FgModel model = new FgModel(Utilities.getList(new Feature("asdf")));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(model);
            out.close();
        } catch(java.io.NotSerializableException e) {
            e.printStackTrace();
            fail("FgModel is not serializable: " + e.getMessage());
        }
    }

}

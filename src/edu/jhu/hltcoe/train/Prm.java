package edu.jhu.hltcoe.train;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class Prm implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public static <T extends Prm> T clonePrm(T prm) {
        try {
            ByteArrayOutputStream strOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(strOut);
            out.writeObject(prm);
            out.close();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(strOut.toByteArray()));
            T prm2 = (T) in.readObject();
            in.close();
            return prm2;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
}

package edu.jhu.pacaya.util.cache;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.random.Prng;

public class CachedFastDiskStoreTest {

    int trials = 1;
    int numObjects = 10000;
    double totalSizeMb = 50;
    int objSizeBytes = (int) totalSizeMb * 1024 * 1024 / numObjects;
    double totalInMemoryMb = 1;
    int maxObjsInMemory = (int) totalInMemoryMb * 1024 * 1024 / objSizeBytes;
    
    @Before
    public void setUp() {
        System.out.println("numObjects: " + numObjects);        
        System.out.println("objSizeBytes: " + objSizeBytes);        
        System.out.println("maxObjsInMemory: " + maxObjsInMemory);        
    }

    public static boolean deleteRecursive(File path) throws FileNotFoundException{
        if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }
    
    @Test
    public void testCachedFastDiskStore() throws Exception {
        try {
        deleteRecursive(new File("./tmp/fds"));
        }catch(Exception e) {
            System.err.println(e.getMessage());
        }        
        
        System.out.println("Creating FastDiskStore");
        CachedFastDiskStore<String,byte[]> jcs = new CachedFastDiskStore<String, byte[]>(new File("./tmp/fds/cache1.binary"), true, maxObjsInMemory);
        
        Timer jcsTimer = new Timer();
        for (int t=0; t<trials; t++) {
            System.out.println("trial: " + t);
            for (int i=0; i<numObjects; i++) {
                String key = "key:" + i;
                byte[] obj = new byte[objSizeBytes];
                for (int k=0; k<obj.length; k++) {
                    obj[k] = (byte) i;
                }
                
                //System.out.println("key: " + key);

                jcsTimer.start();
                jcs.put(key, obj);
                jcsTimer.stop();
                
                //Thread.sleep(10);
            }
                        
            for (int i=0; i<numObjects; i++) {
                int j = Prng.nextInt(numObjects);
                String key = "key:" + j;
                
                jcsTimer.start();
                byte[] jcsObj = (byte[]) jcs.get(key);
                jcsTimer.stop();

                assertEquals(objSizeBytes, jcsObj.length);
                for (int k=0; k<jcsObj.length; k++) {
                    assertEquals((byte)j, jcsObj[k]);
                }
            }
        }
        
        System.out.println("FastDiskStore (total ms): " + jcsTimer.totMs());
    }
    
}

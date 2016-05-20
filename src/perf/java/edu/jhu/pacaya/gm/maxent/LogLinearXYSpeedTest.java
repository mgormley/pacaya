package edu.jhu.pacaya.gm.maxent;

import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.hash.MurmurHash3;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.math.FastMath;

public class LogLinearXYSpeedTest {

    /**
     * Output: (old)
     * Build time (ms): 6322.0
     * Train time (ms): 12804.0
     * Accuracy: 1.0
     * Decode time (ms): 299.0
     * 
     * Output: (6/19/15)
     * Build time (ms): 11814.0
     * Train time (ms): 11678.0
     * Accuracy: 1.0
     * Decode time (ms): 49.0
     * 
     * Output: (6/24/15)
     * Build time (ms): 7033.0
     * Train time (ms): 7315.0
     * Accuracy: 1.0
     * Decode time (ms): 44.0
     */
    //Too slow to be a unit test. @Test
    public void testLogLinearTrainDecodeSpeed() {
        int numXs = 100;
        int numYs = 100;
        int numTrials = 1;
        int numFeatTpls = 100;
        int numFeats = 100000;
        
        for (int t=0; t<numTrials; t++) {
            Timer tBuild = new Timer();
            tBuild.start();
            LogLinearXYData data = new LogLinearXYData(numYs);    
            for (int x=0; x<numXs; x++) {
                FeatureVector[] fvs = new FeatureVector[numYs];
                for (int y=0; y<numYs; y++) {
                    fvs[y] = new FeatureVector();
                    for (int f=0; f<numFeatTpls; f++) {
                        String featName = String.format("x=%d_y=%d_feat=%d", x, y, f); 
                        int feat = MurmurHash3.murmurhash3_x86_32(featName);
                        feat = FastMath.mod(feat, numFeats);
                        feat = data.getFeatAlphabet().lookupIndex(""+featName);
                        fvs[y].add(feat, 1.0);          
                    }
                }   
                // Only add examples for which x == y.
                data.addEx(1.0, "x="+x, "y="+(x%numYs), fvs);
            }
            tBuild.stop();
            System.out.println("Build time (ms): " + tBuild.totMs());
            
            Timer tTrain = new Timer();
            tTrain.start();
            LogLinearXY maxent = new LogLinearXY(new LogLinearXYPrm());
            FgModel model = maxent.train(data);
            // System.out.println("Model: \n"+ model);
            tTrain.stop();
            System.out.println("Train time (ms): " + tTrain.totMs());

            Timer tDecode = new Timer();
            tDecode.start();
            int numCorrect = 0;
            for (int i=0; i<data.size(); i++) {
                Pair<String, VarTensor> pair = maxent.decode(model, data.get(i));
                int y = data.get(i).getY();
                String yStr = data.getYAlphabet().lookupObject(y).toString();
                if (pair.get1().equals(yStr)) {
                    numCorrect++;
                }
                //System.out.println("Correct Y: " + yStr);
                //System.out.println("Pred Y   : " + pair.get1());
                //System.out.println("Dist     : " + pair.get2());
            }
            System.out.println("Accuracy: " + (double) numCorrect / data.size());
            tDecode.stop();
            System.out.println("Decode time (ms): " + tDecode.totMs());
        }
    }
    
    public static void main(String[] args) {
        (new LogLinearXYSpeedTest()).testLogLinearTrainDecodeSpeed();
    }
        
}

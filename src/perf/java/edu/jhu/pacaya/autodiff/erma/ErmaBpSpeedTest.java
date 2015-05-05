package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.autodiff.erma.ErmaBp;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.random.Prng;

/**
 * Results from 09/09/2014:
 * 
 * ErmaBp:
 * r=1 t= 9000 avg(ms)=     0.322 tot(ms)=     2.901 tok/sec= 31023.785
 * 
 * BeliefPropagation:
 * r=1 t= 9000 avg(ms)=     0.784 tot(ms)=     7.059 tok/sec= 29324.267
 * 
 * @author mgormley
 */
public class ErmaBpSpeedTest {

    // Number of tokens. 23 is avg sent length in sec 22 of PTB.
    private static final int n = 23;
    
    // @Test
    public void run() {
        Prng.seed(1234567134);
        for (int round=0; round<2; round++) {
            Timer timer = new Timer();
            for (int t=0; t<10000; t++) {
                timer.start();
                FactorGraph fg = getFg();
                runBp(fg, t);
                long elapsed = timer.elapsedSinceLastStart();
                timer.stop();
                double sentsPerSec = ((double) t) / timer.totSec();
                double toksPerSec = sentsPerSec * n;
                if (t%1000==0) {
                    System.out.println(String.format("r=%d t=%5d avg(ms)=%10.3f tot(ms)=%10.3f tok/sec=%10.3f", round, t,
                        timer.avgMs(), timer.totSec(), toksPerSec));
                }
            }
        }
    }
        
    public static void runBp(FactorGraph fg, int t) {
        ErmaBpPrm prm = new ErmaBpPrm();
        //prm.s = Algebras.LOG_SIGN_ALGEBRA;
        prm.s = RealAlgebra.REAL_ALGEBRA;
        prm.maxIterations = 1;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.schedule = BpScheduleType.TREE_LIKE;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();
    }

    public static FactorGraph getFg() {
        FactorGraph fg = new FactorGraph();
        Var[] vs = new Var[n];
        for (int i=0; i<n; i++) {
            vs[i] = new Var(VarType.PREDICTED, 10, "t"+i, null);
            // Add a unary factor.
            ExplicitFactor f1 = new ExplicitFactor(new VarSet(vs[i]));
            randomInit(f1);
            fg.addFactor( f1 );
            // Add a transition factor.
            if (i > 0) {
                ExplicitFactor f2 = new ExplicitFactor(new VarSet(vs[i], vs[i-1]));
                randomInit(f2);
                fg.addFactor( f2 );
            }
        }
        return fg;
    }

    private static void randomInit(ExplicitFactor f1) {
        for (int c=0; c<f1.size(); c++) {
            f1.setValue(c, Prng.nextDouble());
        }
    }

    private static double exp(double x) {
        x = 1d + x / 256d;
        x *= x;
        x *= x;
        x *= x;
        x *= x;
        x *= x;
        x *= x;
        x *= x;
        x *= x;
        return x;
    }

    public static void main(String[] args) {
        (new ErmaBpSpeedTest()).run();
    }
    
}

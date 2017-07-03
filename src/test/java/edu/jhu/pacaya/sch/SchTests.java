package edu.jhu.pacaya.sch;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.jhu.pacaya.sch.graph.IntDiGraphTest;
import edu.jhu.pacaya.sch.graph.WeightedIntDiGraphTest;
import edu.jhu.pacaya.sch.tasks.SumPathTest;
import edu.jhu.pacaya.sch.util.DefaultDictTest;
import edu.jhu.pacaya.sch.util.IndexedTest;
import edu.jhu.pacaya.sch.util.OrderedSetTest;
import edu.jhu.pacaya.sch.util.ScheduleUtilsTest;
import edu.jhu.pacaya.sch.util.TestUtilsTest;
import edu.jhu.pacaya.sch.util.dist.TruncatedNormalTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TruncatedNormalTest.class,
    OrderedSetTest.class,
    ScheduleUtilsTest.class,
    SumPathTest.class,
    TestUtilsTest.class,
    IntDiGraphTest.class,
    WeightedIntDiGraphTest.class,
    DefaultDictTest.class,
    IndexedTest.class })
public class SchTests { }

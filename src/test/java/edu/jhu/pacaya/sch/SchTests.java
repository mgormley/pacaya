package edu.jhu.pacaya.sch;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.jhu.pacaya.sch.graph.IntDiGraphTest;
import edu.jhu.pacaya.sch.util.DefaultDictTest;
import edu.jhu.pacaya.sch.util.IndexedTest;
import edu.jhu.pacaya.sch.util.OrderedSetTest;
import edu.jhu.pacaya.sch.util.ScheduleUtilsTest;
import edu.jhu.pacaya.sch.util.TestUtilsTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    OrderedSetTest.class,
    ScheduleUtilsTest.class,
    TestUtilsTest.class,
    IntDiGraphTest.class,
    DefaultDictTest.class,
    IndexedTest.class })
public class SchTests { }

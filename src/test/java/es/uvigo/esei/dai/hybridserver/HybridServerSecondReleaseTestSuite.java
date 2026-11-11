package es.uvigo.esei.dai.hybridserver;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
  Step1TestSuite.class,
  Step3TestSuite.class,
  Step4TestSuite.class
})
public class HybridServerSecondReleaseTestSuite {
}

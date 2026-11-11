package es.uvigo.esei.dai.hybridserver;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import es.uvigo.esei.dai.hybridserver.step4.HTMLClientRequestsWithDatabaseTest;
import es.uvigo.esei.dai.hybridserver.step4.XMLClientRequestsWithDatabaseTest;
import es.uvigo.esei.dai.hybridserver.step4.XSDClientRequestsWithDatabaseTest;
import es.uvigo.esei.dai.hybridserver.step4.XSLTClientRequestsWithDatabaseTest;

@Suite
@SelectClasses({
  HTMLClientRequestsWithDatabaseTest.class,
  XMLClientRequestsWithDatabaseTest.class,
  XSDClientRequestsWithDatabaseTest.class,
  XSLTClientRequestsWithDatabaseTest.class
})
public class Step4TestSuite {
}

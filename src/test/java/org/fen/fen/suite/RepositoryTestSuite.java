package org.fen.fen.suite;

import org.junit.platform.suite.api.IncludePackages;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Repository Test Suite")
@SelectPackages("org.fen.fen.repository")
@IncludePackages("org.fen.fen.repository")
public class RepositoryTestSuite {
}

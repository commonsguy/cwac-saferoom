package com.commonsware.cwac.saferoom.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  SafeRoomCompatTestSuite.class,
  RekeyTest.class,
  DecryptTest.class
})
public class SafeRoomSuite {
}

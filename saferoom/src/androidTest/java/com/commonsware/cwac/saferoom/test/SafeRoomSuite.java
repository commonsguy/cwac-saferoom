package com.commonsware.cwac.saferoom.test;

import com.commonsware.cwac.saferoom.test.room.simple.DaoTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  SafeRoomCompatTestSuite.class,
  RekeyTest.class,
  EncryptTest.class,
  DecryptTest.class,
  WALTest.class,
  DaoTests.class,
  MigratingImportTest.class
})
public class SafeRoomSuite {
}

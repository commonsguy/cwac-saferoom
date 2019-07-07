package com.commonsware.cwac.saferoom.test.room.simple;

import android.support.test.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DaoTruncateTests extends DaoTests {
  @Before
  public void setUp() {
    db=StuffDatabase.create(InstrumentationRegistry.getTargetContext(), false, true);
    store=db.stuffStore();
  }

  @Test
  public void confirmWalOff() {
    assertFalse(db.getOpenHelper().getWritableDatabase().isWriteAheadLoggingEnabled());
  }
}

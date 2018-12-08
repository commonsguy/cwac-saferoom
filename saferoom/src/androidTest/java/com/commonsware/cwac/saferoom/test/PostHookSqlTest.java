package com.commonsware.cwac.saferoom.test;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PostHookSqlTest {
  private static final String DB_NAME="db";
  private static final String PASSPHRASE="6co4bqk6xloskxwap6kzi9tp434iqdh89xgpi2g95mk38q9772y1fezxzjsgdibszw0ho2x4i7ykjwlvr9z389zhgiblniwra74ajlx9b3l1737kvxr8bxk5hgej5vz9";

  @Before
  public void setUp() throws Exception {
    copy(InstrumentationRegistry.getTargetContext().getAssets().open("note.db"),
      getDbFile());
  }

  @After
  public void tearDown() {
    File db=getDbFile();

    if (db.exists()) {
      db.delete();
    }

    File journal=new File(db.getParentFile(), DB_NAME+"-journal");

    if (journal.exists()) {
      journal.delete();
    }
  }

  @Test(expected = net.sqlcipher.database.SQLiteException.class)
  public void defaultBehavior() throws IOException {
    assertTrue(getDbFile().exists());

    SafeHelperFactory factory=
      SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE));
    SupportSQLiteOpenHelper helper=
      factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
        new Callback(1));
    SupportSQLiteDatabase db=helper.getReadableDatabase();

    db.close();
  }

  @Test
  public void migrate() throws IOException {
    assertTrue(getDbFile().exists());

    SafeHelperFactory factory=
      SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE),
        SafeHelperFactory.POST_KEY_SQL_MIGRATE);
    SupportSQLiteOpenHelper helper=
      factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
        new Callback(1));
    SupportSQLiteDatabase db=helper.getReadableDatabase();

    assertOriginalContent(db);
    db.close();

    // with migrate, the change should be permanent

    factory=SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE));
    helper=factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
        new Callback(1));
    db=helper.getReadableDatabase();

    assertOriginalContent(db);
    db.close();
  }

  @Test
  public void v3() throws IOException {
    assertTrue(getDbFile().exists());

    SafeHelperFactory factory=
      SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE),
        SafeHelperFactory.POST_KEY_SQL_V3);
    SupportSQLiteOpenHelper helper=
      factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
        new Callback(1));
    SupportSQLiteDatabase db=helper.getReadableDatabase();

    assertOriginalContent(db);
    db.close();

    // with v3, the change should be temporary

    factory=SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE));
    helper=factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
      new Callback(1));

    boolean didWeGoBoom = false;

    try {
      db = helper.getReadableDatabase();
    }
    catch (net.sqlcipher.database.SQLiteException ex) {
      didWeGoBoom = true;
    }

    assertTrue(didWeGoBoom);
  }

  private void assertOriginalContent(SupportSQLiteDatabase db) {
    Cursor c=db.query("SELECT _id, content FROM note;");

    assertNotNull(c);
    assertEquals(1, c.getCount());
    Assert.assertTrue(c.moveToFirst());
    assertEquals(1, c.getInt(0));
    assertEquals("this is a test", c.getString(1));
    c.close();
  }

  private File getDbFile() {
    return InstrumentationRegistry.getTargetContext().getDatabasePath(DB_NAME);
  }

  static private void copy(InputStream in, File dst) throws IOException {
    FileOutputStream out=new FileOutputStream(dst);
    byte[] buf=new byte[1024];
    int len;

    while ((len=in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }

    in.close();
    out.close();
  }

  private static final class Callback extends SupportSQLiteOpenHelper.Callback {
    public Callback(int version) {
      super(version);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                          int newVersion) {

    }
  }
}

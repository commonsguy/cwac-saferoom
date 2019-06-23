package com.commonsware.cwac.saferoom.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PreHookSqlTest {
  private static final String DB_NAME="db";
  private static final String PASSPHRASE="cufflink powerboat mundane vagrancy ragweed waving";
  private static final String PREKEY_SQL="PRAGMA cipher_default_kdf_iter = 4000";

  @After
  public void tearDown() {
    Context ctxt=InstrumentationRegistry.getTargetContext();
    File db=ctxt.getDatabasePath(DB_NAME);

    for (File f : db.getParentFile().listFiles()) {
      f.delete();
    }
  }

  @Test
  public void testPreKeySql() throws IOException {
    SafeHelperFactory.Options options = SafeHelperFactory.Options.builder().setPreKeySql(PREKEY_SQL).build();
    SafeHelperFactory factory=
      SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE), options);
    SupportSQLiteOpenHelper helper=
      factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
        new Callback(1));
    SupportSQLiteDatabase db=helper.getWritableDatabase();

    assertEquals(1, db.getVersion());

    db.close();
  }

  private static final class Callback extends SupportSQLiteOpenHelper.Callback {
    public Callback(int version) {
      super(version);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {
      db.execSQL("CREATE TABLE foo (bar, goo);");
      db.execSQL("INSERT INTO foo (bar, goo) VALUES (?, ?)",
        new Object[] {1, "two"});
    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                          int newVersion) {

    }
  }
}

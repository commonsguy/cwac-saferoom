package com.commonsware.cwac.saferoom.test;

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RekeyTest {
  private static final String DB_NAME="db";
  private static final String PASSPHRASE="Call me Ishmael. Some years ago—never mind how long precisely—having little or no money in my purse, and nothing particular to interest me on shore, I thought I would sail about a little and see the watery part of the world. It is a way I have of driving off the spleen and regulating the circulation. Whenever I find myself growing grim about the mouth; whenever it is a damp, drizzly November in my soul; whenever I find myself involuntarily pausing before coffin warehouses, and bringing up the rear of every funeral I meet; and especially whenever my hypos get such an upper hand of me, that it requires a strong moral principle to prevent me from deliberately stepping into the street, and methodically knocking people’s hats off—then, I account it high time to get to sea as soon as I can. This is my substitute for pistol and ball. With a philosophical flourish Cato throws himself upon his sword; I quietly take to the ship. There is nothing surprising in this. If they but knew it, almost all men in their degree, some time or other, cherish very nearly the same feelings towards the ocean with me.";

  @After
  public void tearDown() {
    Context ctxt=InstrumentationRegistry.getTargetContext();
    File db=ctxt.getDatabasePath(DB_NAME);

    if (db.exists()) {
      db.delete();
    }

    File journal=new File(db.getParentFile(), DB_NAME+"-journal");

    if (journal.exists()) {
      journal.delete();
    }
  }

  @Test
  public void rekey() throws IOException {
    SafeHelperFactory factory=
      SafeHelperFactory.fromUser(new SpannableStringBuilder("sekrit"));
    SupportSQLiteOpenHelper helper=
      factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME, 1,
        new Callback(1));
    SupportSQLiteDatabase db=helper.getWritableDatabase();

    assertOriginalContent(db);
    SafeHelperFactory.rekey(db, new SpannableStringBuilder(PASSPHRASE));
    assertOriginalContent(db);
    db.execSQL("UPDATE foo SET bar=?, goo=?", new Object[] {3, "four"});
    assertUpdatedContent(db);
    db.close();

    factory=SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE));
    helper=factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME, 1,
      new Callback(1));
    db=helper.getWritableDatabase();
    assertUpdatedContent(db);
  }

  private void assertOriginalContent(SupportSQLiteDatabase db) {
    Cursor c=db.query("SELECT bar, goo FROM foo;");

    assertNotNull(c);
    assertEquals(1, c.getCount());
    assertTrue(c.moveToFirst());
    assertEquals(1, c.getInt(0));
    assertEquals("two", c.getString(1));
    c.close();
  }

  private void assertUpdatedContent(SupportSQLiteDatabase db) {
    Cursor c=db.query("SELECT bar, goo FROM foo;");

    assertNotNull(c);
    assertEquals(1, c.getCount());
    assertTrue(c.moveToFirst());
    assertEquals(3, c.getInt(0));
    assertEquals("four", c.getString(1));
    c.close();
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

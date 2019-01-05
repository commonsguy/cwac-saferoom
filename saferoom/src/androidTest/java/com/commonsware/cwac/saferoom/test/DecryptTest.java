package com.commonsware.cwac.saferoom.test;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SQLCipherUtils;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DecryptTest {
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
  public void successfulDekey() throws Exception {
    final Context ctxt=InstrumentationRegistry.getTargetContext();

    dekey((Callable<Void>) () -> {
      SQLCipherUtils.decrypt(ctxt, ctxt.getDatabasePath(DB_NAME), PASSPHRASE.toCharArray());

      return null;
    });
  }

  @Test(expected = FileNotFoundException.class)
  public void fileNotFound() throws Exception {
    final Context ctxt=InstrumentationRegistry.getTargetContext();

    dekey((Callable<Void>) () -> {
      SQLCipherUtils.decrypt(ctxt, new File("/oh/you/must/be/kidding"), PASSPHRASE.toCharArray());

      return null;
    });
  }

  private void dekey(Callable<?> decrypter) throws Exception {
    SafeHelperFactory factory=
      SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE));
    SupportSQLiteOpenHelper helper=
      factory.create(InstrumentationRegistry.getTargetContext(), DB_NAME,
        new Callback(1));
    SupportSQLiteDatabase db=helper.getWritableDatabase();

    assertOriginalContent(db);
    db.close();

    final Context ctxt=InstrumentationRegistry.getTargetContext();

    decrypter.call();

    SQLiteDatabase plainDb=
      SQLiteDatabase.openDatabase(ctxt.getDatabasePath(DB_NAME).getAbsolutePath(),
        null, SQLiteDatabase.OPEN_READWRITE);

    assertOriginalContent(plainDb);
    plainDb.close();
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

  private void assertOriginalContent(SQLiteDatabase db) {
    Cursor c=db.rawQuery("SELECT bar, goo FROM foo;", null);

    assertNotNull(c);
    assertEquals(1, c.getCount());
    assertTrue(c.moveToFirst());
    assertEquals(1, c.getInt(0));
    assertEquals("two", c.getString(1));
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

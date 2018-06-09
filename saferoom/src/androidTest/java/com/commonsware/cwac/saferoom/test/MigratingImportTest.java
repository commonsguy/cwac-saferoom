package com.commonsware.cwac.saferoom.test;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MigratingImportTest {
  private static final String DB_NAME="test.db";
  private static final String PASSPHRASE="Call me Ishmael. Some years ago—never mind how long precisely—having little or no money in my purse, and nothing particular to interest me on shore, I thought I would sail about a little and see the watery part of the world. It is a way I have of driving off the spleen and regulating the circulation. Whenever I find myself growing grim about the mouth; whenever it is a damp, drizzly November in my soul; whenever I find myself involuntarily pausing before coffin warehouses, and bringing up the rear of every funeral I meet; and especially whenever my hypos get such an upper hand of me, that it requires a strong moral principle to prevent me from deliberately stepping into the street, and methodically knocking people’s hats off—then, I account it high time to get to sea as soon as I can. This is my substitute for pistol and ball. With a philosophical flourish Cato throws himself upon his sword; I quietly take to the ship. There is nothing surprising in this. If they but knew it, almost all men in their degree, some time or other, cherish very nearly the same feelings towards the ocean with me.";

  @Before
  public void setUp() {
    InstrumentationRegistry.getTargetContext().deleteDatabase(DB_NAME);
  }

  @Test
  public void safe() {
    final Context ctxt=InstrumentationRegistry.getTargetContext();

    SQLiteDatabase.loadLibs(ctxt);
    SQLiteOpenHelper helper = new SafeNonRoomHelper(ctxt);

    helper.getWritableDatabase(PASSPHRASE);
    helper.close();

    ImportingSafeDatabase room = ImportingSafeDatabase.gimme(ctxt);
    SupportSQLiteDatabase db=room.getOpenHelper().getWritableDatabase();

    try {
      assertTrue(db.isWriteAheadLoggingEnabled());
    }
    finally {
      room.close();
    }
  }

  @Test
  public void notQuiteAsSafeButStillNice() {
    final Context ctxt=InstrumentationRegistry.getTargetContext();

    android.database.sqlite.SQLiteOpenHelper helper = new LessSafeNonRoomHelper(ctxt);

    helper.getWritableDatabase();
    helper.close();

    ImportingLessSafeDatabase room = ImportingLessSafeDatabase.gimme(ctxt);
    SupportSQLiteDatabase db=room.getOpenHelper().getWritableDatabase();

    try {
      assertTrue(db.isWriteAheadLoggingEnabled());
    }
    finally {
      room.close();
    }
  }

  private static class SafeNonRoomHelper extends SQLiteOpenHelper {
    SafeNonRoomHelper(@NonNull Context context) {
      super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      // should not need any tables to reproduce the problem
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      throw new IllegalStateException("Wait, wut?");
    }
  }

  private static class LessSafeNonRoomHelper extends android.database.sqlite.SQLiteOpenHelper {
    LessSafeNonRoomHelper(@NonNull Context context) {
      super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(android.database.sqlite.SQLiteDatabase db) {
      // should not need any tables to reproduce the problem
    }

    @Override
    public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int oldVersion, int newVersion) {
      throw new IllegalStateException("Wait, wut?");
    }
  }

  @Entity
  static class SillyEntity {
    @PrimaryKey(autoGenerate = true)
    long id;
  }

  @Database(entities = {SillyEntity.class}, version = 2)
  static abstract class ImportingSafeDatabase extends RoomDatabase {
    static ImportingSafeDatabase gimme(Context ctxt) {
      return Room.databaseBuilder(ctxt, ImportingSafeDatabase.class, DB_NAME)
        .openHelperFactory(new SafeHelperFactory(PASSPHRASE.toCharArray()))
        .addMigrations(MIGRATION_1_2)
        .build();
    }
  }

  @Database(entities = {SillyEntity.class}, version = 2)
  static abstract class ImportingLessSafeDatabase extends RoomDatabase {
    static ImportingLessSafeDatabase gimme(Context ctxt) {
      return Room.databaseBuilder(ctxt, ImportingLessSafeDatabase.class, DB_NAME)
        .addMigrations(MIGRATION_1_2)
        .build();
    }
  }

  private static Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `SillyEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)");
    }
  };
}

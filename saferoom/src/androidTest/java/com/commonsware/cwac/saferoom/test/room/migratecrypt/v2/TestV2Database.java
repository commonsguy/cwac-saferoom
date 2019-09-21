package com.commonsware.cwac.saferoom.test.room.migratecrypt.v2;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

@Database(entities = {TestEntity.class}, version = 2)
abstract public class TestV2Database extends RoomDatabase {
  public abstract TestDao testStore();

  public static Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
      db.execSQL("ALTER TABLE `TestEntity` ADD `value` INTEGER");
    }
  };
}

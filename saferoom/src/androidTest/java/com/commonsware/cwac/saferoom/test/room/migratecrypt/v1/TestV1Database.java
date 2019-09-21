package com.commonsware.cwac.saferoom.test.room.migratecrypt.v1;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {TestEntity.class}, version = 1)
abstract public class TestV1Database extends RoomDatabase {
  public abstract TestDao testStore();
}

package com.commonsware.cwac.saferoom.test.room.migratecrypt.v2;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface TestDao {
  @Query("SELECT * FROM TestEntity")
  List<TestEntity> loadAll();

  @Insert
  void insert(TestEntity entity);
}

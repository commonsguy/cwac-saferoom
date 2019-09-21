package com.commonsware.cwac.saferoom.test.room.migratecrypt.v2;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TestDao {
  @Query("SELECT * FROM TestEntity")
  List<TestEntity> loadAll();

  @Insert
  void insert(TestEntity entity);
}

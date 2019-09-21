package com.commonsware.cwac.saferoom.test.room.migratecrypt.v2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TestEntity {
  @PrimaryKey
  @NonNull
  public final String id;

  @Nullable
  public final Integer value;

  public TestEntity(String id, Integer value) {
    this.id = id;
    this.value = value;
  }
}

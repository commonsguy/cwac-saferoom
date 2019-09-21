package com.commonsware.cwac.saferoom.test.room.migratecrypt.v2;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

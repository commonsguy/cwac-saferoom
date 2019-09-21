package com.commonsware.cwac.saferoom.test.room.migratecrypt.v1;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TestEntity {
  @PrimaryKey
  @NonNull
  public final String id;

  public TestEntity(String id) {
    this.id = id;
  }
}

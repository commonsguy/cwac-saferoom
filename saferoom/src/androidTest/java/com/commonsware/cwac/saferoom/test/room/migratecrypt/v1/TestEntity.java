package com.commonsware.cwac.saferoom.test.room.migratecrypt.v1;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class TestEntity {
  @PrimaryKey
  @NonNull
  public final String id;

  public TestEntity(String id) {
    this.id = id;
  }
}

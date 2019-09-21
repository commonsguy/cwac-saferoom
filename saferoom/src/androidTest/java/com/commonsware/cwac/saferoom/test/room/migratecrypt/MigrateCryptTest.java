package com.commonsware.cwac.saferoom.test.room.migratecrypt;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;

import com.commonsware.cwac.saferoom.SQLCipherUtils;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import com.commonsware.cwac.saferoom.test.room.migratecrypt.v1.TestEntity;
import com.commonsware.cwac.saferoom.test.room.migratecrypt.v1.TestV1Database;
import com.commonsware.cwac.saferoom.test.room.migratecrypt.v2.TestV2Database;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MigrateCryptTest {
  private static final String DB_NAME = "test.db";
  private static final String PASSPHRASE="Call me Ishmael. Some years ago—never mind how long precisely—having little or no money in my purse, and nothing particular to interest me on shore, I thought I would sail about a little and see the watery part of the world. It is a way I have of driving off the spleen and regulating the circulation. Whenever I find myself growing grim about the mouth; whenever it is a damp, drizzly November in my soul; whenever I find myself involuntarily pausing before coffin warehouses, and bringing up the rear of every funeral I meet; and especially whenever my hypos get such an upper hand of me, that it requires a strong moral principle to prevent me from deliberately stepping into the street, and methodically knocking people’s hats off—then, I account it high time to get to sea as soon as I can. This is my substitute for pistol and ball. With a philosophical flourish Cato throws himself upon his sword; I quietly take to the ship. There is nothing surprising in this. If they but knew it, almost all men in their degree, some time or other, cherish very nearly the same feelings towards the ocean with me.";

  private Context ctxt;

  @Before
  public void setUp() {
    ctxt = InstrumentationRegistry.getTargetContext();
  }

  @After
  public void tearDown() {
    ctxt.deleteDatabase(DB_NAME);
  }

  @Test
  public void encryptAndMigrate() throws IOException {
    assertEquals(SQLCipherUtils.State.DOES_NOT_EXIST, SQLCipherUtils.getDatabaseState(ctxt, DB_NAME));

    TestV1Database v1 = Room.databaseBuilder(ctxt, TestV1Database.class, DB_NAME).build();
    String keyOne = UUID.randomUUID().toString();
    TestEntity entity = new TestEntity(keyOne);

    v1.testStore().insert(entity);

    List<TestEntity> entitiesOne = v1.testStore().loadAll();

    assertEquals(1, entitiesOne.size());
    assertEquals(keyOne, entitiesOne.get(0).id);

    v1.close();

    assertEquals(SQLCipherUtils.State.UNENCRYPTED, SQLCipherUtils.getDatabaseState(ctxt, DB_NAME));
    SQLCipherUtils.encrypt(ctxt, ctxt.getDatabasePath(DB_NAME), PASSPHRASE.toCharArray());
    assertEquals(SQLCipherUtils.State.ENCRYPTED, SQLCipherUtils.getDatabaseState(ctxt, DB_NAME));

    SafeHelperFactory factory=
        SafeHelperFactory.fromUser(new SpannableStringBuilder(PASSPHRASE));
    TestV2Database v2 = Room.databaseBuilder(ctxt, TestV2Database.class, DB_NAME)
        .addMigrations(TestV2Database.MIGRATION_1_2)
        .openHelperFactory(factory)
        .build();

    List<com.commonsware.cwac.saferoom.test.room.migratecrypt.v2.TestEntity> entitiesTwo =
        v2.testStore().loadAll();

    assertEquals(1, entitiesTwo.size());
    assertEquals(keyOne, entitiesTwo.get(0).id);
  }
}

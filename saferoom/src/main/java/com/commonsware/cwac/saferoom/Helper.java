/*
 * Copyright (C) 2016 The Android Open Source Project
 * Modifications Copyright (c) 2017 CommonsWare, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.saferoom;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import java.io.IOException;

/**
 * SupportSQLiteOpenHelper implementation that works with SQLCipher for Android
 */
class Helper implements SupportSQLiteOpenHelper {
  private final OpenHelper delegate;
  private final char[] passphrase;
  private final String name;

  Helper(Context context, String name, int version,
         SupportSQLiteOpenHelper.Callback callback, char[] passphrase) {
    SQLiteDatabase.loadLibs(context);
    delegate=createDelegate(context, name, version, callback);
    this.passphrase=passphrase;
    this.name=name;
  }

  private OpenHelper createDelegate(Context context, String name,
                                    int version, final Callback callback) {
    return(new OpenHelper(context, name, version) {
      /**
       * {@inheritDoc}
       */
      @Override
      public void onCreate(SQLiteDatabase db) {
        callback.onCreate(getWrappedDb(db));
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        callback.onUpgrade(getWrappedDb(db), oldVersion, newVersion);
      }

/* MLM -- these methods do not exist in SQLCipher for Android
      @Override
      public void onConfigure(SQLiteDatabase db) {
        callback.onConfigure(getWrappedDb(db));
      }

      @Override
      public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        callback.onDowngrade(getWrappedDb(db), oldVersion, newVersion);
      }
*/

      /**
       * {@inheritDoc}
       */
      @Override
      public void onOpen(SQLiteDatabase db) {
        callback.onOpen(getWrappedDb(db));
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  synchronized public String getDatabaseName() {
    return name;
    // TODO not supported in SQLCipher for Android
//    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    return delegate.getDatabaseName();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  synchronized public void setWriteAheadLoggingEnabled(boolean enabled) {
    // throw new UnsupportedOperationException("I kinna do it, cap'n!");
    delegate.setWriteAheadLoggingEnabled(enabled);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: this implementation zeros out the passphrase after opening the
   * database
   */
  @Override
  synchronized public SupportSQLiteDatabase getWritableDatabase() {
    SupportSQLiteDatabase result=
      delegate.getWritableSupportDatabase(passphrase);

    for (int i=0;i<passphrase.length;i++) {
      passphrase[i]=(char)0;
    }

    return(result);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: this implementation delegates to getWritableDatabase(), to ensure
   * that we only need the passphrase once
   */
  @Override
  public SupportSQLiteDatabase getReadableDatabase() {
    //return delegate.getReadableSupportDatabase();
    return(getWritableDatabase());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  synchronized public void close() {
    delegate.close();
  }

  abstract static class OpenHelper extends SQLiteOpenHelper {
    private volatile Database wrappedDb;
    private volatile Boolean walEnabled;

    OpenHelper(Context context, String name, int version) {
      super(context, name, null, version, null);
    }

    synchronized SupportSQLiteDatabase getWritableSupportDatabase(char[] passphrase) {
      SQLiteDatabase db=super.getWritableDatabase(passphrase);
      SupportSQLiteDatabase result=getWrappedDb(db);

      if (walEnabled!=null) {
        setupWAL(wrappedDb);
      }

      return result;
    }

    synchronized Database getWrappedDb(SQLiteDatabase db) {
      if (wrappedDb==null) {
        wrappedDb = new Database(db);

        if (walEnabled != null && !db.inTransaction()) {
          setupWAL(wrappedDb);
        }
      }

      return(wrappedDb);
    }

    private void setupWAL(Database db) {
      if (!db.isReadOnly()) {
        if (walEnabled) {
          db.enableWriteAheadLogging();
        }
        else {
          db.disableWriteAheadLogging();
        }

        walEnabled=null;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
      super.close();
      wrappedDb.close();
      wrappedDb=null;
    }

    void setWriteAheadLoggingEnabled(boolean writeAheadLoggingEnabled) {
      walEnabled=writeAheadLoggingEnabled;

      if (wrappedDb!=null) {
        setupWAL(wrappedDb);
      }
    }
  }
}

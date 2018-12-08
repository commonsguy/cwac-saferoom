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
import net.sqlcipher.DatabaseErrorHandler;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * SupportSQLiteOpenHelper implementation that works with SQLCipher for Android
 */
class Helper implements SupportSQLiteOpenHelper {
  private final OpenHelper delegate;
  private final char[] passphrase;

  Helper(Context context, String name, Callback callback, char[] passphrase) {
    SQLiteDatabase.loadLibs(context);
    delegate=createDelegate(context, name, callback);
    this.passphrase=passphrase;
  }

  private OpenHelper createDelegate(Context context, String name,
                                    final Callback callback) {
    final Database[] dbRef = new Database[1];

    return(new OpenHelper(context, name, dbRef, callback));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  synchronized public String getDatabaseName() {
    return delegate.getDatabaseName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  synchronized public void setWriteAheadLoggingEnabled(boolean enabled) {
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
    return(getWritableDatabase());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  synchronized public void close() {
    delegate.close();
  }

  static class OpenHelper extends SQLiteOpenHelper {
    private final Database[] dbRef;
    private volatile Callback callback;
    private volatile boolean migrated;

    OpenHelper(Context context, String name, Database[] dbRef, Callback callback) {
      super(context, name, null, callback.version, null, new DatabaseErrorHandler() {
        @Override
        public void onCorruption(SQLiteDatabase dbObj) {
          Database db = dbRef[0];

          if (db != null) {
            callback.onCorruption(db);
          }
        }
      });

      this.dbRef = dbRef;
      this.callback=callback;
    }

    synchronized SupportSQLiteDatabase getWritableSupportDatabase(char[] passphrase) {
      migrated = false;

      SQLiteDatabase db=super.getWritableDatabase(passphrase);

      if (migrated) {
        close();
        return getWritableSupportDatabase(passphrase);
      }

      return getWrappedDb(db);
    }

    synchronized Database getWrappedDb(SQLiteDatabase db) {
      Database wrappedDb = dbRef[0];

      if (wrappedDb == null) {
        wrappedDb = new Database(db);
        dbRef[0] = wrappedDb;
      }

      return(dbRef[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
      callback.onCreate(getWrappedDb(sqLiteDatabase));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
      migrated = true;
      callback.onUpgrade(getWrappedDb(sqLiteDatabase), oldVersion, newVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
      callback.onConfigure(getWrappedDb(db));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      migrated = true;
      callback.onDowngrade(getWrappedDb(db), oldVersion, newVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
      if (!migrated) {
        // from Google: "if we've migrated, we'll re-open the db so we  should not call the callback."
        callback.onOpen(getWrappedDb(db));
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
      super.close();
      dbRef[0] = null;
    }
  }
}

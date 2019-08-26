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

import android.content.Context;
import android.os.Build;
import net.sqlcipher.DatabaseErrorHandler;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;
import androidx.annotation.RequiresApi;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/**
 * SupportSQLiteOpenHelper implementation that works with SQLCipher for Android
 */
class Helper implements SupportSQLiteOpenHelper {
  private final OpenHelper delegate;
  private final byte[] passphrase;
  private final boolean clearPassphrase;

  Helper(Context context, String name, Callback callback, byte[] passphrase,
         SafeHelperFactory.Options options) {
    SQLiteDatabase.loadLibs(context);
    clearPassphrase=options.clearPassphrase;
    delegate=createDelegate(context, name, callback, options);
    this.passphrase=passphrase;
  }

  private OpenHelper createDelegate(Context context, String name,
                                    final Callback callback, SafeHelperFactory.Options options) {
    final Database[] dbRef = new Database[1];

    return(new OpenHelper(context, name, dbRef, callback, options));
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
   * NOTE: by default, this implementation zeros out the passphrase after opening the
   * database
   */
  @Override
  synchronized public SupportSQLiteDatabase getWritableDatabase() {
    SupportSQLiteDatabase result;

    try {
      result = delegate.getWritableSupportDatabase(passphrase);
    }
    catch (SQLiteException e) {
      if (passphrase != null) {
        boolean isCleared = true;

        for (byte b : passphrase) {
          isCleared = isCleared && (b == (byte) 0);
        }

        if (isCleared) {
          throw new IllegalStateException("The passphrase appears to be cleared. This happens by" +
              "default the first time you use the factory to open a database, so we can remove the" +
              "cleartext passphrase from memory. If you close the database yourself, please use a" +
              "fresh SafeHelperFactory to reopen it. If something else (e.g., Room) closed the" +
              "database, and you cannot control that, use SafeHelperFactory.Options to opt out of" +
              "the automatic password clearing step. See the project README for more information.");
        }
      }

      throw e;
    }

    if (clearPassphrase && passphrase != null) {
      for (int i = 0; i < passphrase.length; i++) {
        passphrase[i] = (byte) 0;
      }
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

    OpenHelper(Context context, String name, Database[] dbRef, Callback callback,
               SafeHelperFactory.Options options) {
      super(context, name, null, callback.version, new SQLiteDatabaseHook() {
        @Override
        public void preKey(SQLiteDatabase database) {
          if (options!=null && options.preKeySql!=null) {
            database.rawExecSQL(options.preKeySql);
          }
        }

        @Override
        public void postKey(SQLiteDatabase database) {
          if (options!=null && options.postKeySql!=null) {
            database.rawExecSQL(options.postKeySql);
          }
        }
      }, new DatabaseErrorHandler() {
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

    synchronized SupportSQLiteDatabase getWritableSupportDatabase(byte[] passphrase) {
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

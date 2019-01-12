/***
 Copyright (c) 2017 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain	a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _The Busy Coder's Guide to Android Development_
 https://commonsware.com/Android
 */

package com.commonsware.cwac.saferoom;

import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteStatement;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Pair;
import net.sqlcipher.database.SQLiteCursor;
import net.sqlcipher.database.SQLiteCursorDriver;
import net.sqlcipher.database.SQLiteQuery;
import java.util.List;
import java.util.Locale;

/**
 * A SupportSQLiteDatabase implementation that delegates to a SQLCipher
 * for Android implementation of SQLiteDatabase
 */
class Database implements SupportSQLiteDatabase {
  private static final String[] CONFLICT_VALUES = new String[]
    {"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "};

  private final net.sqlcipher.database.SQLiteDatabase safeDb;

  Database(net.sqlcipher.database.SQLiteDatabase safeDb) {
    this.safeDb=safeDb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SupportSQLiteStatement compileStatement(String sql) {
    return(new Statement(safeDb.compileStatement(sql)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void beginTransaction() {
    safeDb.beginTransaction();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void beginTransactionNonExclusive() {
    safeDb.beginTransactionNonExclusive();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void beginTransactionWithListener(SQLiteTransactionListener listener) {
    safeDb.beginTransactionWithListener(
      new net.sqlcipher.database.SQLiteTransactionListener() {
        @Override
        public void onBegin() {
          listener.onBegin();
        }

        @Override
        public void onCommit() {
          listener.onCommit();
        }

        @Override
        public void onRollback() {
          listener.onRollback();
        }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener listener) {
    safeDb.beginTransactionWithListenerNonExclusive(
      new net.sqlcipher.database.SQLiteTransactionListener() {
        @Override
        public void onBegin() {
          listener.onBegin();
        }

        @Override
        public void onCommit() {
          listener.onCommit();
        }

        @Override
        public void onRollback() {
          listener.onRollback();
        }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void endTransaction() {
    safeDb.endTransaction();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTransactionSuccessful() {
    safeDb.setTransactionSuccessful();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean inTransaction() {
    if (safeDb.isOpen()) {
      return(safeDb.inTransaction());
    }

    throw new IllegalStateException("You should not be doing this on a closed database");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDbLockedByCurrentThread() {
    if (safeDb.isOpen()) {
      return(safeDb.isDbLockedByCurrentThread());
    }

    throw new IllegalStateException("You should not be doing this on a closed database");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean yieldIfContendedSafely() {
    if (safeDb.isOpen()) {
      return(safeDb.yieldIfContendedSafely());
    }

    throw new IllegalStateException("You should not be doing this on a closed database");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
    if (safeDb.isOpen()) {
      return(safeDb.yieldIfContendedSafely(sleepAfterYieldDelay));
    }

    throw new IllegalStateException("You should not be doing this on a closed database");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getVersion() {
    return(safeDb.getVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setVersion(int version) {
    safeDb.setVersion(version);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getMaximumSize() {
    return(safeDb.getMaximumSize());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long setMaximumSize(long numBytes) {
    return(safeDb.setMaximumSize(numBytes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPageSize() {
    return(safeDb.getPageSize());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPageSize(long numBytes) {
    safeDb.setPageSize(numBytes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cursor query(String sql) {
    return(query(new SimpleSQLiteQuery(sql)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cursor query(String sql, Object[] selectionArgs) {
    return(query(new SimpleSQLiteQuery(sql, selectionArgs)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cursor query(final SupportSQLiteQuery supportQuery) {
    return(query(supportQuery, null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cursor query(final SupportSQLiteQuery supportQuery,
                      CancellationSignal signal) {
    BindingsRecorder hack=new BindingsRecorder();

    supportQuery.bindTo(hack);

    return(safeDb.rawQueryWithFactory(
      new net.sqlcipher.database.SQLiteDatabase.CursorFactory() {
        @Override
        public net.sqlcipher.Cursor newCursor(
          net.sqlcipher.database.SQLiteDatabase db,
          SQLiteCursorDriver masterQuery, String editTable,
          SQLiteQuery query) {
          supportQuery.bindTo(new Program(query));
          return new SQLiteCursor(db, masterQuery, editTable, query);
        }
      }, supportQuery.getSql(), hack.getBindings(), null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long insert(String table, int conflictAlgorithm,
                     ContentValues values) {
    return(safeDb.insertWithOnConflict(table, null, values, conflictAlgorithm));
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("ThrowFromFinallyBlock")
  @Override
  public int delete(String table, String whereClause, Object[] whereArgs) {
    String query = "DELETE FROM " + table
      + (TextUtils.isEmpty(whereClause) ? "" : " WHERE " + whereClause);
    SupportSQLiteStatement statement = compileStatement(query);

    try {
      SimpleSQLiteQuery.bind(statement, whereArgs);
      return statement.executeUpdateDelete();
    }
    finally {
      try {
        statement.close();
      }
      catch (Exception e) {
        throw new RuntimeException("Exception attempting to close statement", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("ThrowFromFinallyBlock")
  @Override
  public int update(String table, int conflictAlgorithm, ContentValues values,
                    String whereClause, Object[] whereArgs) {
    // taken from SQLiteDatabase class.
    if (values == null || values.size() == 0) {
      throw new IllegalArgumentException("Empty values");
    }
    StringBuilder sql = new StringBuilder(120);
    sql.append("UPDATE ");
    sql.append(CONFLICT_VALUES[conflictAlgorithm]);
    sql.append(table);
    sql.append(" SET ");

    // move all bind args to one array
    int setValuesSize = values.size();
    int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
    Object[] bindArgs = new Object[bindArgsSize];
    int i = 0;
    for (String colName : values.keySet()) {
      sql.append((i > 0) ? "," : "");
      sql.append(colName);
      bindArgs[i++] = values.get(colName);
      sql.append("=?");
    }
    if (whereArgs != null) {
      for (i = setValuesSize; i < bindArgsSize; i++) {
        bindArgs[i] = whereArgs[i - setValuesSize];
      }
    }
    if (!TextUtils.isEmpty(whereClause)) {
      sql.append(" WHERE ");
      sql.append(whereClause);
    }
    SupportSQLiteStatement statement = compileStatement(sql.toString());

    try {
      SimpleSQLiteQuery.bind(statement, bindArgs);
      return statement.executeUpdateDelete();
    }
    finally {
      try {
        statement.close();
      }
      catch (Exception e) {
        throw new RuntimeException("Exception attempting to close statement", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execSQL(String sql) throws SQLException {
    safeDb.execSQL(sql);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execSQL(String sql, Object[] bindArgs) throws SQLException {
    safeDb.execSQL(sql, bindArgs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadOnly() {
    return(safeDb.isReadOnly());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOpen() {
    return(safeDb.isOpen());
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public boolean needUpgrade(int newVersion) {
    return(safeDb.needUpgrade(newVersion));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPath() {
    return(safeDb.getPath());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLocale(Locale locale) {
    safeDb.setLocale(locale);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaxSqlCacheSize(int cacheSize) {
    safeDb.setMaxSqlCacheSize(cacheSize);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setForeignKeyConstraintsEnabled(boolean enable) {
    safeDb.setForeignKeyConstraintsEnabled(enable);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean enableWriteAheadLogging() {
    return safeDb.enableWriteAheadLogging();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disableWriteAheadLogging() {
    safeDb.disableWriteAheadLogging();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isWriteAheadLoggingEnabled() {
    return safeDb.isWriteAheadLoggingEnabled();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Pair<String, String>> getAttachedDbs() {
    return(safeDb.getAttachedDbs());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDatabaseIntegrityOk() {
    return(safeDb.isDatabaseIntegrityOk());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    safeDb.close();
  }

  /**
   * Changes the passphrase associated with this database. The
   * char[] is *not* cleared by this method -- please zero it
   * out if you are done with it.
   *
   * @param passphrase the new passphrase to use
   */
  public void rekey(char[] passphrase) {
    safeDb.changePassword(passphrase);
  }

  /**
   * Changes the passphrase associated with this database. The supplied
   * Editable is cleared as part of this operation.
   *
   * @param editor source of passphrase, presumably from a user
   */
  public void rekey(Editable editor) {
    char[] passphrase=new char[editor.length()];

    editor.getChars(0, editor.length(), passphrase, 0);

    try {
      rekey(passphrase);
    }
    finally {
      editor.clear();
    }
  }
}

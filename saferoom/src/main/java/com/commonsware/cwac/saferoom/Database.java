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
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void beginTransactionNonExclusive() {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void beginTransactionWithListener(
    SQLiteTransactionListener transactionListener) {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void beginTransactionWithListenerNonExclusive(
    SQLiteTransactionListener transactionListener) {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
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
    return(safeDb.inTransaction());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDbLockedByCurrentThread() {
    return(safeDb.isDbLockedByCurrentThread());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean yieldIfContendedSafely() {
    return(safeDb.yieldIfContendedSafely());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
    return(safeDb.yieldIfContendedSafely(sleepAfterYieldDelay));
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
      + (isEmpty(whereClause) ? "" : " WHERE " + whereClause);
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

//    return(safeDb.delete(table, whereClause, stringify(whereArgs)));
  }

  /**
   * {@inheritDoc}
   */
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
    if (!isEmpty(whereClause)) {
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
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void setForeignKeyConstraintsEnabled(boolean enable) {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    safeDb.setForeignKeyConstraintsEnabled(enable);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public boolean enableWriteAheadLogging() {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    return(safeDb.enableWriteAheadLogging());
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void disableWriteAheadLogging() {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    safeDb.disableWriteAheadLogging();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public boolean isWriteAheadLoggingEnabled() {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    return(safeDb.isWriteAheadLoggingEnabled());
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public List<Pair<String, String>> getAttachedDbs() {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    return(safeDb.getAttachedDbs());
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public boolean isDatabaseIntegrityOk() {
    // TODO not supported in SQLCipher for Android
    throw new UnsupportedOperationException("I kinna do it, cap'n!");
//    return(safeDb.isDatabaseIntegrityOk());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    safeDb.close();
  }

  private static boolean isEmpty(String input) {
    return input == null || input.length() == 0;
  }
}

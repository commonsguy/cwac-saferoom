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

import android.arch.persistence.db.SupportSQLiteStatement;
import net.sqlcipher.database.SQLiteStatement;

/**
 * SupportSQLiteStatement implementation that wraps SQLCipher for Android's
 * SQLiteStatement
 */
class Statement implements SupportSQLiteStatement {
  private final SQLiteStatement safeStatement;

  Statement(SQLiteStatement safeStatement) {
    this.safeStatement=safeStatement;
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void bindNull(int index) {
    safeStatement.bindNull(index);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void bindLong(int index, long value) {
    safeStatement.bindLong(index, value);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void bindDouble(int index, double value) {
    safeStatement.bindDouble(index, value);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void bindString(int index, String value) {
    safeStatement.bindString(index, value);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void bindBlob(int index, byte[] value) {
    safeStatement.bindBlob(index, value);
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void clearBindings() {
    safeStatement.clearBindings();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void execute() {
    safeStatement.execute();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public int executeUpdateDelete() {
    return safeStatement.executeUpdateDelete();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public long executeInsert() {
    return safeStatement.executeInsert();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public long simpleQueryForLong() {
    return safeStatement.simpleQueryForLong();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public String simpleQueryForString() {
    return safeStatement.simpleQueryForString();
  }

  /**
   * {@inheritDoc}
   *
   * NOTE: Not presently supported, will throw an UnsupportedOperationException
   */
  @Override
  public void close() throws Exception {
    safeStatement.close();
  }
}

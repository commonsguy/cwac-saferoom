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

import net.sqlcipher.database.SQLiteProgram;
import androidx.sqlite.db.SupportSQLiteProgram;

/**
 * SupportSQLiteProgram implementation that wraps SQLCipher for Android's
 * implementation
 */
class Program implements SupportSQLiteProgram {
  private final SQLiteProgram delegate;

  Program(SQLiteProgram delegate) {
    this.delegate=delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void bindNull(int index) {
    delegate.bindNull(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void bindLong(int index, long value) {
    delegate.bindLong(index, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void bindDouble(int index, double value) {
    delegate.bindDouble(index, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void bindString(int index, String value) {
    delegate.bindString(index, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void bindBlob(int index, byte[] value) {
    delegate.bindBlob(index, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearBindings() {
    delegate.clearBindings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    delegate.close();
  }
}

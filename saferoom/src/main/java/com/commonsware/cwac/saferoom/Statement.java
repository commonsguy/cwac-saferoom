/***
 Copyright (c) 2017-2018 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain	a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.
 */

package com.commonsware.cwac.saferoom;

import android.arch.persistence.db.SupportSQLiteStatement;
import net.sqlcipher.database.SQLiteStatement;

/**
 * SupportSQLiteStatement implementation that wraps SQLCipher for Android's
 * SQLiteStatement
 */
class Statement extends Program implements SupportSQLiteStatement {
  private final SQLiteStatement safeStatement;

  Statement(SQLiteStatement safeStatement) {
    super(safeStatement);
    this.safeStatement=safeStatement;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() {
    safeStatement.execute();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int executeUpdateDelete() {
    return safeStatement.executeUpdateDelete();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long executeInsert() {
    return safeStatement.executeInsert();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long simpleQueryForLong() {
    return safeStatement.simpleQueryForLong();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String simpleQueryForString() {
    return safeStatement.simpleQueryForString();
  }
}

/*
 * Copyright (c) 2012-2017 CommonsWare, LLC
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
import android.text.Editable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.sqlcipher.database.SQLiteDatabase;

public class SQLCipherUtils {
  /**
   * The detected state of the database, based on whether we can open it
   * without a passphrase.
   */
  public enum State {
    DOES_NOT_EXIST, UNENCRYPTED, ENCRYPTED
  }

  /**
   * Determine whether or not this database appears to be encrypted, based
   * on whether we can open it without a passphrase.
   *
   * @param ctxt a Context
   * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
   *               etc.
   * @return the detected state of the database
   */
  public static State getDatabaseState(Context ctxt, String dbName) {
    SQLiteDatabase.loadLibs(ctxt);

    return(getDatabaseState(ctxt.getDatabasePath(dbName)));
  }

  /**
   * Determine whether or not this database appears to be encrypted, based
   * on whether we can open it without a passphrase.
   *
   * @param dbPath a File pointing to the database
   * @return the detected state of the database
   */
  public static State getDatabaseState(File dbPath) {
    if (dbPath.exists()) {
      SQLiteDatabase db=null;

      try {
        db=
          SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(), "",
            null, SQLiteDatabase.OPEN_READONLY);

        db.getVersion();

        return(State.UNENCRYPTED);
      }
      catch (Exception e) {
        return(State.ENCRYPTED);
      }
      finally {
        if (db != null) {
          db.close();
        }
      }
    }

    return(State.DOES_NOT_EXIST);
  }

  /**
   * Replaces this database with a version encrypted with the supplied
   * passphrase, deleting the original. Do not call this while the database
   * is open, which includes during any Room migrations.
   *
   * The passphrase is untouched in this call. If you are going to turn around
   * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
   * passphrase. If not, please set all bytes of the passphrase to 0 or something
   * to clear out the passphrase.
   *
   * @param ctxt a Context
   * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
   *               etc.
   * @param editor the passphrase, such as obtained by calling getText() on an
   *               EditText
   * @throws IOException
   */
  public static void encrypt(Context ctxt, String dbName, Editable editor)
    throws IOException {
    char[] passphrase=new char[editor.length()];

    editor.getChars(0, editor.length(), passphrase, 0);
    encrypt(ctxt, dbName, passphrase);
  }

  /**
   * Replaces this database with a version encrypted with the supplied
   * passphrase, deleting the original. Do not call this while the database
   * is open, which includes during any Room migrations.
   *
   * The passphrase is untouched in this call. If you are going to turn around
   * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
   * passphrase. If not, please set all bytes of the passphrase to 0 or something
   * to clear out the passphrase.
   *
   * @param ctxt a Context
   * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
   *               etc.
   * @param passphrase the passphrase from the user
   * @throws IOException
   */
  public static void encrypt(Context ctxt, String dbName, char[] passphrase)
    throws IOException {
    encrypt(ctxt, ctxt.getDatabasePath(dbName), passphrase);
  }

  /**
   * Replaces this database with a version encrypted with the supplied
   * passphrase, deleting the original. Do not call this while the database
   * is open, which includes during any Room migrations.
   *
   * The passphrase is untouched in this call. If you are going to turn around
   * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
   * passphrase. If not, please set all bytes of the passphrase to 0 or something
   * to clear out the passphrase.
   *
   * @param ctxt a Context
   * @param originalFile a File pointing to the database
   * @param passphrase the passphrase from the user
   * @throws IOException
   */
  public static void encrypt(Context ctxt, File originalFile, char[] passphrase)
    throws IOException {
    SQLiteDatabase.loadLibs(ctxt);

    if (originalFile.exists()) {
      File newFile=File.createTempFile("sqlcipherutils", "tmp",
          ctxt.getCacheDir());
      SQLiteDatabase db=
        SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(),
          "", null, SQLiteDatabase.OPEN_READWRITE);
      int version=db.getVersion();

      db.close();

      db=SQLiteDatabase.openDatabase(newFile.getAbsolutePath(), passphrase,
        null, SQLiteDatabase.OPEN_READWRITE);

      db.rawExecSQL("ATTACH DATABASE '"+originalFile.getAbsolutePath()+
        "' AS plaintext KEY ''");
      db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')");
      db.rawExecSQL("DETACH DATABASE plaintext");
      db.setVersion(version);
      db.close();

      originalFile.delete();
      newFile.renameTo(originalFile);
    }
    else {
      throw new FileNotFoundException(originalFile.getAbsolutePath()+" not found");
    }
  }

  /**
   * Replaces this database with a decrypted version, deleting the original
   * encrypted database. Do not call this while the database is open, which
   * includes during any Room migrations.
   *
   * The passphrase is untouched in this call. Please set all bytes of the
   * passphrase to 0 or something to clear out the passphrase if you are done
   * with it.
   *
   * @param ctxt a Context
   * @param originalFile a File pointing to the encrypted database
   * @param passphrase the passphrase from the user for the encrypted database
   * @throws IOException
   */
  public static void decrypt(Context ctxt, File originalFile, char[] passphrase)
    throws IOException {
    SQLiteDatabase.loadLibs(ctxt);

    if (originalFile.exists()) {
      File newFile=
        File.createTempFile("sqlcipherutils", "tmp",
          ctxt.getCacheDir());
      SQLiteDatabase db=
        SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(),
          passphrase, null, SQLiteDatabase.OPEN_READWRITE);

      db.rawExecSQL("ATTACH DATABASE '"+newFile.getAbsolutePath()+
        "' AS plaintext KEY ''");
      db.rawExecSQL("SELECT sqlcipher_export('plaintext')");
      db.rawExecSQL("DETACH DATABASE plaintext");

      int version=db.getVersion();

      db.close();

      db=SQLiteDatabase.openDatabase(newFile.getAbsolutePath(), "",
        null, SQLiteDatabase.OPEN_READWRITE);
      db.setVersion(version);
      db.close();

      originalFile.delete();
      newFile.renameTo(originalFile);
    }
    else {
      throw new FileNotFoundException(originalFile.getAbsolutePath()+" not found");
    }
  }
}

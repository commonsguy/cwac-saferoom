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

import android.content.Context;
import android.text.Editable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import net.sqlcipher.database.SQLiteDatabase;

/**
 * SupportSQLiteOpenHelper.Factory implementation, for use with Room
 * and similar libraries, that supports SQLCipher for Android.
 */
public class SafeHelperFactory implements SupportSQLiteOpenHelper.Factory {
  public static final String POST_KEY_SQL_MIGRATE = "PRAGMA cipher_migrate;";
  public static final String POST_KEY_SQL_V3 = "PRAGMA cipher_compatibility = 3;";

  final private byte[] passphrase;
  final private Options options;

  /**
   * Creates a SafeHelperFactory from an Editable, such as what you get by
   * calling getText() on an EditText.
   *
   * The Editable will be cleared as part of this call.
   *
   * @param editor the user's supplied passphrase
   * @return a SafeHelperFactory
   */
  public static SafeHelperFactory fromUser(Editable editor) {
    return fromUser(editor, (String)null);
  }

  /**
   * Creates a SafeHelperFactory from an Editable, such as what you get by
   * calling getText() on an EditText.
   *
   * The Editable will be cleared as part of this call.
   *
   * @param editor the user's supplied passphrase
   * @param postKeySql optional SQL to be executed after database has been
   *                    "keyed" but before any other database access is performed
   * @return a SafeHelperFactory
   */
  public static SafeHelperFactory fromUser(Editable editor, String postKeySql) {
    return fromUser(editor, Options.builder().setPostKeySql(postKeySql).build());
  }

  /**
   * Creates a SafeHelperFactory from an Editable, such as what you get by
   * calling getText() on an EditText.
   *
   * The Editable will be cleared as part of this call.
   *
   * @param editor the user's supplied passphrase
   * @param options options for pre-key, post-key SQL
   * @return a SafeHelperFactory
   */
  public static SafeHelperFactory fromUser(Editable editor, Options options) {
    char[] passphrase=new char[editor.length()];
    SafeHelperFactory result;

    editor.getChars(0, editor.length(), passphrase, 0);

    try {
      result=new SafeHelperFactory(passphrase, options);
    }
    finally {
      editor.clear();
    }

    return(result);
  }

  /**
   * Changes the passphrase associated with this database. The
   * char[] is *not* cleared by this method -- please zero it
   * out if you are done with it.
   *
   * This will not encrypt an unencrypted database. Please use the
   * encrypt() method for that.
   *
   * @param db the database to rekey
   * @param passphrase the new passphrase to use
   */
  public static void rekey(SupportSQLiteDatabase db, char[] passphrase) {
    if (db instanceof Database) {
      ((Database)db).rekey(passphrase);
    }
    else {
      throw new IllegalArgumentException("Database is not from CWAC-SafeRoom");
    }
  }

  /**
   * Changes the passphrase associated with this database. The supplied
   * Editable is cleared as part of this operation.
   *
   * This will not encrypt an unencrypted database. Please use the
   * encrypt() method for that.
   *
   * @param db the database to rekey
   * @param editor source of passphrase, presumably from a user
   */
  public static void rekey(SupportSQLiteDatabase db, Editable editor) {
    if (db instanceof Database) {
      ((Database)db).rekey(editor);
    }
    else {
      throw new IllegalArgumentException("Database is not from CWAC-SafeRoom");
    }
  }

  /**
   * Standard constructor.
   *
   * Note that the passphrase supplied here will be filled in with zeros after
   * the database is opened. Ideally, you should not create additional copies
   * of this passphrase, particularly as String objects.
   *
   * If you are using an EditText to collect the passphrase from the user,
   * call getText() on the EditText, and pass that Editable to the
   * SafeHelperFactory.fromUser() factory method.
   *
   * @param passphrase user-supplied passphrase to use for the database
   */
  public SafeHelperFactory(char[] passphrase) {
    this(passphrase, (String)null);
  }

  /**
   * Standard constructor.
   *
   * Note that the passphrase supplied here will be filled in with zeros after
   * the database is opened. Ideally, you should not create additional copies
   * of this passphrase, particularly as String objects.
   *
   * If you are using an EditText to collect the passphrase from the user,
   * call getText() on the EditText, and pass that Editable to the
   * SafeHelperFactory.fromUser() factory method.
   *
   * @param passphrase user-supplied passphrase to use for the database
   * @param postKeySql optional callback to be called after database has been
   *                    "keyed" but before any database access is performed
   */
  public SafeHelperFactory(char[] passphrase, String postKeySql) {
    this(SQLiteDatabase.getBytes(passphrase), postKeySql);
  }

  /**
   * Standard constructor.
   *
   * Note that the passphrase supplied here will be filled in with zeros after
   * the database is opened. Ideally, you should not create additional copies
   * of this passphrase, particularly as String objects.
   *
   * If you are using an EditText to collect the passphrase from the user,
   * call getText() on the EditText, and pass that Editable to the
   * SafeHelperFactory.fromUser() factory method.
   *
   * @param passphrase user-supplied passphrase to use for the database
   * @param options options for pre-key, post-key SQL
   */
  public SafeHelperFactory(char[] passphrase, Options options) {
    this(SQLiteDatabase.getBytes(passphrase), options);
  }

  /**
   * Standard constructor.
   *
   * Note that the passphrase supplied here will be filled in with zeros after
   * the database is opened. Ideally, you should not create additional copies
   * of this passphrase, particularly as String objects.
   *
   * @param passphrase user-supplied passphrase to use for the database
   */
  public SafeHelperFactory(byte[] passphrase) {
    this(passphrase, new Options.Builder().build());
  }

  /**
   * Standard constructor.
   *
   * Note that the passphrase supplied here will be filled in with zeros after
   * the database is opened. Ideally, you should not create additional copies
   * of this passphrase, particularly as String objects.
   *
   * @param passphrase user-supplied passphrase to use for the database
   * @param postKeySql optional callback to be called after database has been
   *                    "keyed" but before any database access is performed
   */
  public SafeHelperFactory(byte[] passphrase, String postKeySql) {
    this(passphrase, new Options.Builder().setPostKeySql(postKeySql).build());
  }

  /**
   * Standard constructor.
   *
   * Note that the passphrase supplied here will be filled in with zeros after
   * the database is opened. Ideally, you should not create additional copies
   * of this passphrase, particularly as String objects.
   *
   * If you are using an EditText to collect the passphrase from the user,
   * call getText() on the EditText, and pass that Editable to the
   * SafeHelperFactory.fromUser() factory method.
   *
   * @param passphrase user-supplied passphrase to use for the database
   * @param options options for pre-key, post-key SQL
   */
  public SafeHelperFactory(byte[] passphrase, Options options) {
    this.passphrase = passphrase;
    this.options = options;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SupportSQLiteOpenHelper create(
    SupportSQLiteOpenHelper.Configuration configuration) {
    return(create(configuration.context, configuration.name,
      configuration.callback));
  }

  public SupportSQLiteOpenHelper create(Context context, String name,
                                        SupportSQLiteOpenHelper.Callback callback) {
    return(new Helper(context, name, callback, passphrase, options));
  }

  /**
   * Class for encapsulating pre- and post-key SQL statements to be executed as
   * part of opening the database. Use the static builder() method to get a Builder
   * for creating one of these.
   */
  public static class Options {
    /**
     * SQL to be executed before keying the database
     */
    public final String preKeySql;

    /**
     * SQL to be executed after keying the database
     */
    public final String postKeySql;

    private Options(String preKeySql, String postKeySql) {
      this.preKeySql = preKeySql;
      this.postKeySql = postKeySql;
    }

    /**
     * @return a Builder to use to create an Options instance
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * A builder of Options objects. Use the builder() method on Options to create
     * one of these, call various setters to configure it, then call build() to
     * create the Options matching your requested specifications.
     */
    public static class Builder {
      private String preKeySql;
      private String postKeySql;

      private Builder() {
        // use the builder() method on SafeRoomHelper.Options
      }

      /**
       * @param preKeySql SQL to be executed before keying the database
       * @return the builder, for further configuration
       */
      public Builder setPreKeySql(String preKeySql) {
        this.preKeySql = preKeySql;

        return this;
      }

      /**
       * @param postKeySql SQL to be executed after keying the database
       * @return the builder, for further configuration
       */
      public Builder setPostKeySql(String postKeySql) {
        this.postKeySql = postKeySql;

        return this;
      }

      /**
       * @return the Options object containing your requested SQL
       */
      public Options build() {
        return new Options(preKeySql, postKeySql);
      }
    }
  }
}

/***
 Copyright (c) 2017 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain  a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.
 */

package com.commonsware.cwac.saferoom.test;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DirectTests {
  private static final String PASSPHRASE="even-more-sekrit";
  private SpannableStringBuilder ssb=new SpannableStringBuilder(PASSPHRASE);
  private SupportSQLiteOpenHelper helper;

  @Before
  public void setUp() {
    helper=SafeHelperFactory.fromUser(ssb)
      .create(InstrumentationRegistry.getTargetContext(), null, 1,
        new SupportSQLiteOpenHelper.Callback(1) {
          @Override
          public void onCreate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Customer` (`id` TEXT NOT NULL, `postalCode` TEXT, `displayName` TEXT, `creationDate` INTEGER, `tags` TEXT, `latitude` REAL, `longitude` REAL, PRIMARY KEY(`id`))");
          }

          @Override
          public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                int newVersion) {
            throw new IllegalStateException("Um, how did we get here?");
          }
        });
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void basicOps() {
    SupportSQLiteDatabase db=helper.getWritableDatabase();

    for (int i=0;i<1024;i++) {
      db.delete("Customer", null, null);
    }
  }
}

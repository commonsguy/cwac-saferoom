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
import android.content.ContentValues;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.Assert.assertEquals;

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
    ContentValues cv=new ContentValues(7);

    cv.put("postalCode", "90120");
    cv.put("tags", "");
    cv.put("latitude", 37.386052);
    cv.put("longitude", -122.083851);

    for (int i=0;i<1024;i++) {
      cv.put("id", UUID.randomUUID().toString());
      cv.put("displayName", "Customer #"+(i+1));
      cv.put("creationDate", System.currentTimeMillis());

      db.insert("Customer", 0, cv);
    }

    Cursor c=db.query("SELECT DISTINCT id FROM Customer");

    assertEquals(1024, c.getCount());

    ArrayList<String> ids=new ArrayList<>();

    while (c.moveToNext()) {
      ids.add(c.getString(0));
    }

    assertEquals(1024, ids.size());
    cv.clear();

    for (int i=0;i<1024;i++) {
      cv.put("tags", "foo");
      db.update("Customer", 0, cv, "id=?", new String[] {ids.get(i)});
    }

    c=db.query("SELECT DISTINCT id FROM Customer");
    assertEquals(1024, c.getCount());
    c=db.query("SELECT DISTINCT id FROM Customer WHERE tags='foo'");
    assertEquals(1024, c.getCount());

    for (int i=0;i<1024;i++) {
      db.delete("Customer", null, null);
    }

    c=db.query("SELECT DISTINCT id FROM Customer");
    assertEquals(0, c.getCount());
  }
}

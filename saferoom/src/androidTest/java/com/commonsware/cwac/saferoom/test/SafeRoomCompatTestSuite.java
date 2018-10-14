/*
 * Copyright (c) 2017 CommonsWare, LLC
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

package com.commonsware.cwac.saferoom.test;

import android.content.Context;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import com.commonsware.dbtest.CompatTestSuite;
import org.junit.BeforeClass;
import java.io.File;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

public class SafeRoomCompatTestSuite extends CompatTestSuite {
  @BeforeClass
  public static void beforeSuite() {
    setFactoryProvider(new Provider());
  }

  private static class Provider implements FactoryProvider {
    @Override
    public SupportSQLiteOpenHelper.Factory getFactory() {
      return SafeHelperFactory.fromUser(new SpannableStringBuilder("sekrit"));
    }

    @Override
    public void tearDownDatabase(Context ctxt,
                                 SupportSQLiteOpenHelper.Factory factory,
                                 SupportSQLiteOpenHelper helper) {
      String name=helper.getDatabaseName();

      if (name!=null) {
        File db=ctxt.getDatabasePath(name);

        if (db.exists()) {
          db.delete();
        }

        File journal=new File(db.getParentFile(), name+"-journal");

        if (journal.exists()) {
          journal.delete();
        }
      }
    }
  }
}

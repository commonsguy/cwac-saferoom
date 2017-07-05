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

package com.commonsware.cwac.saferoom.test.room;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(
  entities={Customer.class, VersionedThingy.class, Category.class},
  version=1
)
@TypeConverters({TypeTransmogrifier.class})
abstract public class StuffDatabase extends RoomDatabase {
  abstract public StuffStore stuffStore();

  private static final String DB_NAME="stuff.db";

  public static StuffDatabase build(Context ctxt, boolean memoryOnly,
                                    SupportSQLiteOpenHelper.Factory f) {
    Builder<StuffDatabase> b;

    if (memoryOnly) {
      b=Room.inMemoryDatabaseBuilder(ctxt.getApplicationContext(),
        StuffDatabase.class);
    }
    else {
      b=Room.databaseBuilder(ctxt.getApplicationContext(), StuffDatabase.class,
        DB_NAME);
    }

    return(b.openHelperFactory(f).build());
  }
}

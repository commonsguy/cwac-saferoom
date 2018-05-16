/***
 Copyright (c) 2017 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain	a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _Android's Architecture Components_
 https://commonsware.com/AndroidArch
 */

package com.commonsware.cwac.saferoom.test.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;

@Database(
  entities={Customer.class, VersionedThingy.class, Category.class},
  version=1
)
@TypeConverters({TypeTransmogrifier.class})
abstract class StuffDatabase extends RoomDatabase {
  abstract StuffStore stuffStore();

  static final String DB_NAME="stuff.db";
  private static volatile StuffDatabase INSTANCE=null;

  synchronized static StuffDatabase get(Context ctxt) {
    if (INSTANCE==null) {
      INSTANCE=create(ctxt, false);
    }

    return(INSTANCE);
  }

  static StuffDatabase create(Context ctxt, boolean memoryOnly) {
    RoomDatabase.Builder<StuffDatabase> b;

    if (memoryOnly) {
      b=Room.inMemoryDatabaseBuilder(ctxt.getApplicationContext(),
        StuffDatabase.class);
    }
    else {
      b=Room.databaseBuilder(ctxt.getApplicationContext(), StuffDatabase.class,
        DB_NAME);
    }

    b.openHelperFactory(SafeHelperFactory.fromUser(new SpannableStringBuilder("sekrit")));

    return(b.build());
  }
}

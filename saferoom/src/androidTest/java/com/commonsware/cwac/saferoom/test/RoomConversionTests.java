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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SQLCipherUtils;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import com.commonsware.cwac.saferoom.test.room.Category;
import com.commonsware.cwac.saferoom.test.room.CategoryShadow;
import com.commonsware.cwac.saferoom.test.room.CategoryTuple;
import com.commonsware.cwac.saferoom.test.room.Customer;
import com.commonsware.cwac.saferoom.test.room.CustomerDisplayTuple;
import com.commonsware.cwac.saferoom.test.room.CustomerStats;
import com.commonsware.cwac.saferoom.test.room.LocationColumns;
import com.commonsware.cwac.saferoom.test.room.StuffDatabase;
import com.commonsware.cwac.saferoom.test.room.StuffStore;
import com.commonsware.cwac.saferoom.test.room.VersionedThingy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class RoomConversionTests {
  private static final String POSTAL_CODE="10001";
  private static final String PASSPHRASE="sekrit";
  private SpannableStringBuilder ssb=new SpannableStringBuilder(PASSPHRASE);

  @Test
  public void conversion() throws IOException {
    Context ctxt=InstrumentationRegistry.getTargetContext();
    StuffDatabase originalDb=StuffDatabase.build(ctxt, false, null);
    StuffStore store=originalDb.stuffStore();

    try {
      initStore(store);
      assertStore(store);
      originalDb.close();
      assertEquals(SQLCipherUtils.State.UNENCRYPTED,
        SQLCipherUtils.getDatabaseState(ctxt, StuffDatabase.DB_NAME));
      SQLCipherUtils.encrypt(ctxt, StuffDatabase.DB_NAME, ssb);
      assertEquals(SQLCipherUtils.State.ENCRYPTED,
        SQLCipherUtils.getDatabaseState(ctxt, StuffDatabase.DB_NAME));

      StuffDatabase cryptedDb=
        StuffDatabase.build(InstrumentationRegistry.getTargetContext(),
          false, SafeHelperFactory.fromUser(ssb));
      store=cryptedDb.stuffStore();
      assertStore(store);
    }
    finally {
      ctxt.getDatabasePath(StuffDatabase.DB_NAME).delete();
    }
  }

  private void initStore(StuffStore store) {
    final HashSet<String> tags=new HashSet<>();

    tags.add("scuplture");
    tags.add("bronze");
    tags.add("slow-pay");

    final LocationColumns loc=new LocationColumns(40.7047282, -74.0148544);

    final Customer firstCustomer=new Customer(POSTAL_CODE, "Fearless Girl", loc, tags);

    assertEquals(loc.latitude, firstCustomer.officeLocation.latitude,
      .000001);
    assertEquals(loc.longitude, firstCustomer.officeLocation.longitude,
      .000001);
    assertEquals(tags, firstCustomer.tags);

    store.insert(firstCustomer);
  }

  private void assertStore(StuffStore store) {
    final List<Customer> result=store.findByPostalCodes(10, POSTAL_CODE);

    assertEquals(1, result.size());

    final Customer retrievedCustomer=result.get(0);

    assertNotNull(retrievedCustomer.id);
    assertNotNull(retrievedCustomer.displayName);
    assertEquals(POSTAL_CODE, retrievedCustomer.postalCode);
    assertEquals(3, retrievedCustomer.tags.size());
    assertNotNull(retrievedCustomer.creationDate);

    final List<CustomerDisplayTuple> displayTuples=
      store.loadDisplayTuplesByPostalCodes(10, POSTAL_CODE);

    assertEquals(1, displayTuples.size());

    final CustomerDisplayTuple tuple=displayTuples.get(0);

    assertEquals(retrievedCustomer.id, tuple.id);
    assertEquals(retrievedCustomer.displayName, tuple.displayName);

    assertEquals(1, store.getCustomerCount());

    final CustomerStats stats=store.getCustomerStats();

    assertEquals(stats.count, 1);
    assertEquals(stats.max, retrievedCustomer.postalCode);
  }
}

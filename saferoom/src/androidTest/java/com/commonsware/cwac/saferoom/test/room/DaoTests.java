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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class DaoTests {
  StuffDatabase db;
  StuffStore store;

  @Before
  public void setUp() {
    db=StuffDatabase.create(InstrumentationRegistry.getTargetContext(), false);
    store=db.stuffStore();
  }

  @After
  public void tearDown() {
    db.close();

    String name=StuffDatabase.DB_NAME;
    File db=InstrumentationRegistry.getTargetContext().getDatabasePath(name);

    if (db.exists()) {
      db.delete();
    }

    File journal=new File(db.getParentFile(), name+"-journal");

    if (journal.exists()) {
      journal.delete();
    }
  }

  @Test
  public void versionedThingy() {
    final VersionedThingy firstThingy=new VersionedThingy();

    store.insert(firstThingy);

    final VersionedThingy retrievedThingy=
      store.findById(firstThingy.id, firstThingy.versionCode);

    assertEquals(firstThingy.id, retrievedThingy.id);
    assertEquals(firstThingy.versionCode, retrievedThingy.versionCode);
  }

  @Test
  public void customer() {
    final HashSet<String> tags=new HashSet<>();

    tags.add("scuplture");
    tags.add("bronze");
    tags.add("slow-pay");

    final LocationColumns loc=new LocationColumns(40.7047282, -74.0148544);

    final Customer firstCustomer=new Customer("10001", "Fearless Girl", loc, tags);

    assertEquals(loc.latitude, firstCustomer.officeLocation.latitude,
      .000001);
    assertEquals(loc.longitude, firstCustomer.officeLocation.longitude,
      .000001);
    assertEquals(tags, firstCustomer.tags);

    store.insert(firstCustomer);

    final List<Customer> result=store.findByPostalCodes(10, firstCustomer.postalCode);

    assertEquals(1, result.size());

    final Customer retrievedCustomer=result.get(0);

    assertEquals(firstCustomer.id, retrievedCustomer.id);
    assertEquals(firstCustomer.displayName, retrievedCustomer.displayName);
    assertEquals(firstCustomer.postalCode, retrievedCustomer.postalCode);
    assertEquals(loc.latitude, retrievedCustomer.officeLocation.latitude,
      .000001);
    assertEquals(loc.longitude, retrievedCustomer.officeLocation.longitude,
      .000001);
    assertEquals(tags, retrievedCustomer.tags);
    assertEquals(firstCustomer.creationDate, retrievedCustomer.creationDate);

    final List<Customer> near=store.findCustomersAt(loc.latitude, loc.longitude);

    assertEquals(1, near.size());

    final Customer nearCustomer=near.get(0);

    assertEquals(firstCustomer.id, nearCustomer.id);
    assertEquals(firstCustomer.displayName, nearCustomer.displayName);
    assertEquals(firstCustomer.postalCode, nearCustomer.postalCode);
    assertEquals(loc.latitude, nearCustomer.officeLocation.latitude,
      .000001);
    assertEquals(loc.longitude, nearCustomer.officeLocation.longitude,
      .000001);
    assertEquals(tags, nearCustomer.tags);
    assertEquals(firstCustomer.creationDate, nearCustomer.creationDate);

    final List<CustomerDisplayTuple> displayTuples=
      store.loadDisplayTuplesByPostalCodes(10, firstCustomer.postalCode);

    assertEquals(1, displayTuples.size());

    final CustomerDisplayTuple tuple=displayTuples.get(0);

    assertEquals(firstCustomer.id, tuple.id);
    assertEquals(firstCustomer.displayName, tuple.displayName);

    assertEquals(1, store.getCustomerCount());

    final CustomerStats stats=store.getCustomerStats();

    assertEquals(stats.count, 1);
    assertEquals(stats.max, firstCustomer.postalCode);

    final int deleted=store.nukeCertainCustomersFromOrbit(firstCustomer.id);

    assertEquals(1, deleted);
    assertEquals(0, store.getCustomerCount());
  }

  @Test
  public void categories() {
    final Category root=new Category("Root!");

    store.insert(root);

    List<Category> results=store.selectAllCategories();

    assertEquals(1, results.size());
    assertIdentical(root, results.get(0));
    assertIdentical(root, store.findRootCategory());

    final Category child=new Category("Child!", root.id);

    store.insert(child);

    results=store.findChildCategories(root.id);

    assertEquals(1, results.size());
    assertIdentical(child, results.get(0));
    assertEquals(2, store.selectAllCategories().size());

    final CategoryTuple rootTuple=store.findRootCategoryTuple();

    assertNotNull(rootTuple);
    assertEquals(1, rootTuple.children.size());
    assertIdentical(child, rootTuple.children.get(0));
    assertNull(rootTuple.parents);

    final List<CategoryTuple> tuples=store.findChildCategoryTuples(rootTuple.id);

    assertEquals(1, tuples.size());
    assertEquals(0, tuples.get(0).children.size());
    assertEquals(1, tuples.get(0).parents.size());
    assertIdentical(root, tuples.get(0).parents.get(0));

    final CategoryShadow rootShadow=store.findRootCategoryShadow();

    assertNotNull(rootShadow);
    assertEquals(1, rootShadow.children.size());
    assertIdentical(child, rootShadow.children.get(0));

    store.delete(root);
    results=store.selectAllCategories();
    assertEquals(0, results.size());
  }

  void assertIdentical(Category one, Category two) {
    assertEquals(one.id, two.id);
    assertEquals(one.title, two.title);
    assertEquals(one.parentId, two.parentId);
  }
}

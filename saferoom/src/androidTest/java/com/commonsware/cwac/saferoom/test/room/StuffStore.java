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

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import java.util.List;

@Dao
interface StuffStore {
  @Insert
  void insert(VersionedThingy... thingies);

  @Query("SELECT * FROM thingy WHERE id=:id AND version_code=:versionCode")
  VersionedThingy findById(String id, int versionCode);

  @Insert
  void insert(Customer... customers);

  @Query("SELECT * FROM Customer WHERE postalCode IN (:postalCodes) LIMIT :max")
  List<Customer> findByPostalCodes(int max, String... postalCodes);

  @Query("SELECT id, displayName FROM Customer WHERE postalCode IN (:postalCodes) LIMIT :max")
  List<CustomerDisplayTuple> loadDisplayTuplesByPostalCodes(int max,
                                                            String... postalCodes);

  @Query("SELECT COUNT(*) FROM Customer")
  int getCustomerCount();

  @Query("SELECT COUNT(*) AS count, MAX(postalCode) AS max FROM Customer")
  CustomerStats getCustomerStats();

  @Query("DELETE FROM Customer WHERE id IN (:ids)")
  int nukeCertainCustomersFromOrbit(String... ids);

  @Query("SELECT * FROM Customer WHERE ABS(latitude-:lat)<.000001 AND ABS(longitude-:lon)<.000001")
  List<Customer> findCustomersAt(double lat, double lon);

  @Query("SELECT * FROM categories")
  List<Category> selectAllCategories();

  @Query("SELECT * FROM categories WHERE parentId IS NULL")
  Category findRootCategory();

  @Query("SELECT * FROM categories WHERE parentId=:parentId")
  List<Category> findChildCategories(String parentId);

  @Insert
  void insert(Category... categories);

  @Delete
  void delete(Category... categories);

  @Transaction
  @Query("SELECT * FROM categories WHERE parentId IS NULL")
  CategoryTuple findRootCategoryTuple();

  @Transaction
  @Query("SELECT * FROM categories WHERE parentId=:parentId")
  List<CategoryTuple> findChildCategoryTuples(String parentId);

  @Transaction
  @Query("SELECT * FROM categories WHERE parentId IS NULL")
  CategoryShadow findRootCategoryShadow();
}

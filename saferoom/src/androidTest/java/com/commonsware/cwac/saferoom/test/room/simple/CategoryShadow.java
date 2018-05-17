/***
 Copyright (c) 2017 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain  a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _Android's Architecture Components_
 https://commonsware.com/AndroidArch
 */

package com.commonsware.cwac.saferoom.test.room.simple;

import android.arch.persistence.room.Relation;
import java.util.List;

public class CategoryShadow extends Category {
  public CategoryShadow(String id, String title, String parentId) {
    super(id, title, parentId);
  }

  @Relation(parentColumn="id", entityColumn="parentId")
  public List<Category> children;
}

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

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import java.util.UUID;
import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(
  tableName="categories",
  foreignKeys=@ForeignKey(
    entity=Category.class,
    parentColumns="id",
    childColumns="parentId",
    onDelete=CASCADE),
  indices=@Index(value="parentId"))
public class Category {
  @PrimaryKey
  @NonNull
  public final String id;
  public final String title;
  public final String parentId;

  @Ignore
  public Category(String title) {
    this(title, null);
  }

  @Ignore
  public Category(String title, String parentId) {
    this(UUID.randomUUID().toString(), title, parentId);
  }

  public Category(String id, String title, String parentId) {
    this.id=id;
    this.title=title;
    this.parentId=parentId;
  }
}

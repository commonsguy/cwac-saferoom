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

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Entity(indices={@Index(value="postalCode", unique=true)})
public class Customer {
  @PrimaryKey
  @NonNull
  public final String id;

  public final String postalCode;
  public final String displayName;
  public final Date creationDate;

  @Embedded
  public final LocationColumns officeLocation;

  public final Set<String> tags;

  @Ignore
  public Customer(String postalCode, String displayName, LocationColumns officeLocation,
           Set<String> tags) {
    this(UUID.randomUUID().toString(), postalCode, displayName, new Date(),
      officeLocation, tags);
  }

  Customer(String id, String postalCode, String displayName, Date creationDate,
           LocationColumns officeLocation, Set<String> tags) {
    this.id=id;
    this.postalCode=postalCode;
    this.displayName=displayName;
    this.creationDate=creationDate;
    this.officeLocation=officeLocation;
    this.tags=tags;
  }
}
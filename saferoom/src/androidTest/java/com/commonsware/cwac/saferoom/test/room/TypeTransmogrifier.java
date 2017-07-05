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

import android.arch.persistence.room.TypeConverter;
import android.location.Location;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TypeTransmogrifier {
  private static final String TAG="TypeTransmogrifier";

  @TypeConverter
  public static Long fromDate(Date date) {
    if (date==null) {
      return(null);
    }

    return(date.getTime());
  }

  @TypeConverter
  public static Date toDate(Long millisSinceEpoch) {
    if (millisSinceEpoch==null) {
      return(null);
    }

    return(new Date(millisSinceEpoch));
  }

  @TypeConverter
  public static String fromLocation(Location location) {
    if (location==null) {
      return(null);
    }

    return(String.format(Locale.US, "%f,%f", location.getLatitude(),
      location.getLongitude()));
  }

  @TypeConverter
  public static Location toLocation(String latlon) {
    if (latlon==null) {
      return(null);
    }

    String[] pieces=latlon.split(",");
    Location result=new Location("");

    result.setLatitude(Double.parseDouble(pieces[0]));
    result.setLongitude(Double.parseDouble(pieces[1]));

    return(result);
  }

  @TypeConverter
  public static String fromStringSet(Set<String> strings) {
    if (strings==null) {
      return(null);
    }

    StringWriter result=new StringWriter();
    JsonWriter json=new JsonWriter(result);

    try {
      json.beginArray();

      for (String s : strings) {
        json.value(s);
      }

      json.endArray();
      json.close();
    }
    catch (IOException e) {
      Log.e(TAG, "Exception creating JSON", e);
    }

    return(result.toString());
  }

  @TypeConverter
  public static Set<String> toStringSet(String strings) {
    if (strings==null) {
      return(null);
    }

    StringReader reader=new StringReader(strings);
    JsonReader json=new JsonReader(reader);
    HashSet<String> result=new HashSet<>();

    try {
      json.beginArray();

      while (json.hasNext()) {
        result.add(json.nextString());
      }

      json.endArray();
    }
    catch (IOException e) {
      Log.e(TAG, "Exception parsing JSON", e);
    }

    return(result);
  }
}

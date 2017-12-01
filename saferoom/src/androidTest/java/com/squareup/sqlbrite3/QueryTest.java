/*
 * Copyright (C) 2015 Square, Inc.
 * Modifications Copyright (c) 2017 CommonsWare, LLC
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
package com.squareup.sqlbrite3;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Factory;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import com.squareup.sqlbrite3.SqlBrite.Query;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class QueryTest {
  private static final String PASSPHRASE="sekrit";
  private SpannableStringBuilder ssb=new SpannableStringBuilder(PASSPHRASE);
  private BriteDatabase db;

  @Before public void setUp() {
    Configuration configuration = Configuration.builder(InstrumentationRegistry.getContext())
      .callback(new TestDb())
      .build();

//    Factory factory = new FrameworkSQLiteOpenHelperFactory();
    Factory factory = SafeHelperFactory.fromUser(ssb);
    SupportSQLiteOpenHelper helper = factory.create(configuration);

    SqlBrite sqlBrite = new SqlBrite.Builder().build();
    db = sqlBrite.wrapDatabaseHelper(helper, Schedulers.trampoline());
  }

  @Test public void mapToOne() {
    TestDb.Employee
      employees = db.createQuery(
      TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 1")
      .lift(Query.mapToOne(TestDb.Employee.MAPPER))
      .blockingFirst();
    assertThat(employees).isEqualTo(new TestDb.Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneThrowsWhenMapperReturnsNull() {
    db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 1")
      .lift(Query.mapToOne(new Function<Cursor, TestDb.Employee>() {
        @Override public TestDb.Employee apply(Cursor cursor) throws Exception {
          return null;
        }
      }))
      .test()
      .assertError(NullPointerException.class)
      .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneThrowsOnMultipleRows() {
    Observable<TestDb.Employee> employees =
      db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 2") //
        .lift(Query.mapToOne(TestDb.Employee.MAPPER));
    try {
      employees.blockingFirst();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cursor returned more than 1 row");
    }
  }

  @Test public void mapToOneIgnoresNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    TestObserver<TestDb.Employee> observer = new TestObserver<>();
    Observable.just(nully)
      .lift(Query.mapToOne(TestDb.Employee.MAPPER))
      .subscribe(observer);

    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test public void mapToOneOrDefault() {
    TestDb.Employee employees = db.createQuery(
      TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 1")
      .lift(Query.mapToOneOrDefault(
        TestDb.Employee.MAPPER, new TestDb.Employee("fred", "Fred Frederson")))
      .blockingFirst();
    assertThat(employees).isEqualTo(new TestDb.Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneOrDefaultDisallowsNullDefault() {
    try {
      Query.mapToOneOrDefault(TestDb.Employee.MAPPER, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("defaultValue == null");
    }
  }

  @Test public void mapToOneOrDefaultThrowsWhenMapperReturnsNull() {
    db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 1")
      .lift(Query.mapToOneOrDefault(new Function<Cursor, TestDb.Employee>() {
        @Override public TestDb.Employee apply(Cursor cursor) throws Exception {
          return null;
        }
      }, new TestDb.Employee("fred", "Fred Frederson")))
      .test()
      .assertError(NullPointerException.class)
      .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneOrDefaultThrowsOnMultipleRows() {
    Observable<TestDb.Employee> employees =
      db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 2") //
        .lift(Query.mapToOneOrDefault(
          TestDb.Employee.MAPPER, new TestDb.Employee("fred", "Fred Frederson")));
    try {
      employees.blockingFirst();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cursor returned more than 1 row");
    }
  }

  @Test public void mapToOneOrDefaultReturnsDefaultWhenNullCursor() {
    TestDb.Employee defaultEmployee = new TestDb.Employee("bob", "Bob Bobberson");
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    TestObserver<TestDb.Employee> observer = new TestObserver<>();
    Observable.just(nully)
      .lift(Query.mapToOneOrDefault(TestDb.Employee.MAPPER, defaultEmployee))
      .subscribe(observer);

    observer.assertValues(defaultEmployee);
    observer.assertComplete();
  }

  @Test public void mapToList() {
    List<TestDb.Employee> employees = db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES)
      .lift(Query.mapToList(TestDb.Employee.MAPPER))
      .blockingFirst();
    assertThat(employees).containsExactly( //
      new TestDb.Employee("alice", "Alice Allison"), //
      new TestDb.Employee("bob", "Bob Bobberson"), //
      new TestDb.Employee("eve", "Eve Evenson"));
  }

  @Test public void mapToListEmptyWhenNoRows() {
    List<TestDb.Employee> employees = db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " WHERE 1=2")
      .lift(Query.mapToList(TestDb.Employee.MAPPER))
      .blockingFirst();
    assertThat(employees).isEmpty();
  }

  @Test public void mapToListReturnsNullOnMapperNull() {
    Function<Cursor, TestDb.Employee> mapToNull = new Function<Cursor, TestDb.Employee>() {
      private int count;

      @Override public TestDb.Employee apply(Cursor cursor) throws Exception {
        return count++ == 2 ? null : TestDb.Employee.MAPPER.apply(cursor);
      }
    };
    List<TestDb.Employee> employees = db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES) //
      .lift(Query.mapToList(mapToNull)) //
      .blockingFirst();

    assertThat(employees).containsExactly(
      new TestDb.Employee("alice", "Alice Allison"),
      new TestDb.Employee("bob", "Bob Bobberson"),
      null);
  }

  @Test public void mapToListIgnoresNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    TestObserver<List<TestDb.Employee>> subscriber = new TestObserver<>();
    Observable.just(nully)
      .lift(Query.mapToList(TestDb.Employee.MAPPER))
      .subscribe(subscriber);

    subscriber.assertNoValues();
    subscriber.assertComplete();
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
  @Test public void mapToOptional() {
    db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 1")
      .lift(Query.mapToOptional(TestDb.Employee.MAPPER))
      .test()
      .assertValue(Optional.of(new TestDb.Employee("alice", "Alice Allison")));
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
  @Test public void mapToOptionalThrowsWhenMapperReturnsNull() {
    db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 1")
      .lift(Query.mapToOptional(new Function<Cursor, TestDb.Employee>() {
        @Override public TestDb.Employee apply(Cursor cursor) throws Exception {
          return null;
        }
      }))
      .test()
      .assertError(NullPointerException.class)
      .assertErrorMessage("QueryToOne mapper returned null");
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
  @Test public void mapToOptionalThrowsOnMultipleRows() {
    db.createQuery(TestDb.TABLE_EMPLOYEE, TestDb.SELECT_EMPLOYEES + " LIMIT 2") //
      .lift(Query.mapToOptional(TestDb.Employee.MAPPER))
      .test()
      .assertError(IllegalStateException.class)
      .assertErrorMessage("Cursor returned more than 1 row");
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
  @Test public void mapToOptionalIgnoresNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    Observable.just(nully)
      .lift(Query.mapToOptional(TestDb.Employee.MAPPER))
      .test()
      .assertValue(Optional.<TestDb.Employee>empty());
  }
}

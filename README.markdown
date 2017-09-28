CWAC-SafeRoom: A Room<->SQLCipher for Android Bridge
==========================================

This project implements the `Support...` series of classes and interfaces
that [Room](https://developer.android.com/topic/libraries/architecture/room.html)
can use for working with a particular edition of SQLite. Specficially, this
project's classes connect Room with [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/),
a version of SQLite that offers transparent encryption of its contents.

Right now,
this project is for experimentation purposes and for helping to prove the
practicality of the `Support...` class setup. Do not use this in production
applications just yet.

This Android library project is 
[available as a JAR](https://github.com/commonsguy/cwac-saferoom/releases)
or as an artifact for use with Gradle. To use that, add the following
blocks to your `build.gradle` file:

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:saferoom:0.0.4'
}
```

Or, if you cannot use SSL, use `http://repo.commonsware.com` for the repository
URL.

Usage
-----
When you use Room, you use `Room.databaseBuilder()` or `Room.inMemoryDatabaseBuilder()`
to get a `RoomDatabase.Builder`. After configuring that object, you call
`build()` to get an instance of your custom subclass of `RoomDatabase`, whichever
one that you supplied as a Java class object to the
`Room.databaseBuilder()` or `Room.inMemoryDatabaseBuilder()` method.

To use SafeRoom, on the `RoomDatabase.Builder`, before calling `build()`:

- Create an instance of `com.commonsware.cwac.saferoom.SafeHelperFactory`,
passing in the passphrase to use

- Pass that `SafeHelperFactory` to the `RoomDatabase.Builder` via the
`openHelperFactory()` method

```java
// EditText passphraseField;
SafeHelperFactory factory=SafeHelperFactory.fromUser(passphraseField.getText());

StuffDatabase db=Room.databaseBuilder(ctxt, StuffDatabase.class, DB_NAME)
  .openHelperFactory(factory)
  .build();
```

## Passphrase Management

A cardinal rule of passphrases in Java is: do not hold them in `String`
objects. You have no means of clearing those from memory, as a `String`
is an immutable value.

The `SafeHelperFactory` constructor takes a `char[]` for the passphrase. If
you are getting the passphrase from the user via an `EditText` widget,
use the `fromUser()` factory method instead, supplying the `Editable`
that you get from `getText()` on the `EditText`.

SafeRoom will zero out the `char[]` once the database is opened. If you use
`fromUser()`, SafeRoom will also clear the contents of the `Editable`.

Dependencies
------------
As one might expect, this project depends on SQLCipher for Android.

This project also depends on the Room `runtime` artifact. Eventually, that
will be downgraded to only depending on the `support-db` artifact, once
we no longer need to use reflection with `RoomSQLiteQuery`
[to get the argument count](https://issuetracker.google.com/issues/67038952).

Version
-------
This is version v0.0.4 of this module, meaning it still has that new-code smell.

Demo
----
Right now, there is no demo project, though you can putter around the instrumentation
tests.

Additional Documentation
------------------------
[JavaDocs are available](http://javadocs.commonsware.com/cwac/saferoom/index.html),
though most of the library is not `public`, as it does not need to be.

[Android's Architecture Components](https://commonsware.com/AndroidArch)
contains a chapter dedicated to SafeRoom.

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [Stack Overflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+saferoom). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please post an [issue](https://github.com/commonsguy/cwac-saferoom/issues).
Be certain to include complete steps for reproducing the issue.
The [contribution guidelines](CONTRIBUTING.md)
provide some suggestions for how to create a bug report that will get
the problem fixed the fastest.

You are also welcome to join
[the CommonsWare Community](https://community.commonsware.com/)
and post questions
and ideas to [the CWAC category](https://community.commonsware.com/c/cwac).

Do not ask for help via social media.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.
Again, the [contribution guidelines](CONTRIBUTING.md) provide a bit
of guidance here.

Release Notes
-------------
- v0.0.4: raised Room dependencies to `1.0.0-beta1` and SQLCipher for Android to `3.5.7`
- v0.0.3: raised Room dependencies to `1.0.0-alpha8`
- v0.0.2: raised Room dependencies to `1.0.0-alpha5`
- v0.0.1: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>


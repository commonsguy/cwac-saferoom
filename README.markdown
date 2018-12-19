# CWAC-SafeRoom: A Room<->SQLCipher for Android Bridge

This project implements the `Support...` series of classes and interfaces
that [Room](https://developer.android.com/topic/libraries/architecture/room.html)
can use for working with a particular edition of SQLite. Specficially, this
project's classes connect Room with [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/),
a version of SQLite that offers transparent encryption of its contents.

Right now,
this project is for experimentation purposes and for helping to prove the
practicality of the `Support...` class setup. Do not use this in production
applications just yet.

See [a separate `README`](README-1.0.0.md) for information about
the `1.0.0-alpha1` release.

## Installation

There are two versions of this library, for AndroidX and for the older Android Support Library.

If you cannot use SSL, use `http://repo.commonsware.com` for the repository URL.

### AndroidX

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}

dependencies {
    implementation "com.commonsware.cwac:saferoom.x:0.5.1"
}
```

### Android Support Library

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}

dependencies {
    implementation "com.commonsware.cwac:saferoom:0.4.5"
}
```

## Usage

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

### Supplying a Passphrase

A cardinal rule of passphrases in Java is: do not hold them in `String`
objects. You have no means of clearing those from memory, as a `String`
is an immutable value.

The `SafeHelperFactory` constructor takes a `char[]` for the passphrase. If
you are getting the passphrase from the user via an `EditText` widget,
use the `fromUser()` factory method instead, supplying the `Editable`
that you get from `getText()` on the `EditText`.

SafeRoom will zero out the `char[]` once the database is opened. If you use
`fromUser()`, SafeRoom will also clear the contents of the `Editable`.

### Encrypting Existing Databases

If you have an existing SQLite database &mdash; created with Room or
otherwise &mdash; the `SQLCipherUtils` class has `getDatabaseState()`
and `encrypt()` methods for you.

`getDatabaseState()` returns a `State` object indicating whether a database
is `ENCRYPTED`, `UNENCRYPTED`, or `DOES_NOT_EXIST`. The determination
of whether the database is unencrypted is based on whether we can open it
without a passphrase. There are two versions of `getDatabaseState()`:

- `getDatabaseState(Context, String)` for a `Context` and database name

- `getDatabaseState(File)`, where the `File` points to the database

`encrypt()` will take an unencrypted database as input and encrypt it
using the supplied passphrase. Technically, it will encrypt a copy
of the database, then delete the unencrypted one and rename the copy to
the original name. There are three versions of `encrypt()`:

- `encrypt(Context, String, Editable)` where the `String` is the database
name and the `Editable` is the passphrase (e.g., from `getText()` on
an `EditText`)

- `encrypt(Context, String, char[])` where the `String` is the database
name and the `char[]` is the passphrase

- `encrypt(Context, File, char[])` where the `File` points to the database
and the `char[]` is the passphrase

The passphrase is left untouched by `encrypt()`, so you can turn around and
use it with `SafeHelperFactory`. If you are not planning on opening the database,
please clear out the passphrase after `encrypt()` returns.

Only call `encrypt()` when the database is closed. Ideally, call `encrypt()`
before opening the database in Room. At minimum, call `close()` on your
`RoomDatabase` before calling `encrypt()`.

### Changing the Passphrase

If you want to change the passphrase for an existing database:

- Open it in writeable mode

- Call `SafeHelperFactory.rekey()`, supplying that database plus either a
`char[]` or an `Editable` reflecting the new passphrase to use

Note that this does *not* encrypt an unencrypted database. Use the `encrypt()`
option listed above for that.

The `Editable` will be cleared as part of this work, but the `char[]` will
not be zero'd out. Please clear that array as soon as you are done with it.

### Decrypting Existing Databases

You can call `decrypt()` on `SQLCipherUtils` to decrypt an existing
SQLCipher-encrypted database. Supply the `Context`, the `File` pointing
to the database, and a `char[]` with the passphrase. `decrypt()` will
replace the encrypted database with a decrypted one, so that database can
be opened using ordinary SQLite.

## Dependencies

As one might expect, this project depends on SQLCipher for Android.

This project also depends upon `android.arch.persistence:db` (Android Support Library edition)
or `androidx.sqlite:sqlite-framework` (AndroidX edition), which is
the support database API that Room uses.

The Android Support Library edition of CWAC-SafeRoom is frozen at supporting `1.1.1`
of `android.arch.persistence:db`.

The AndroidX edition of CWAC-SafeRoom supports `2.0.0` of `androidx.sqlite:sqlite-framework`
and should be updated to support newer versions of AndroidX over time.

## Tests

This project has two sources of tests. Some are local to the project. The
rest come from the [`support-db-tests`](https://gitlab.com/commonsguy/support-db-tests)
project. That project contains tests that exercise any support database API
implementation.

TL;DR: to run the full set of CWAC-SafeRoom tests, use `SafeRoomSuite`.
Either run that directly from your IDE, or set up a run configuration pointing
to it, etc.

## Version

This is version v0.5.1 of this module.

See [a separate `README`](README-1.0.0.md) for information about
the `1.0.0-alpha2` release.

## Demo

Right now, there is no demo project.

## Additional Documentation

[JavaDocs are available](http://javadocs.commonsware.com/cwac/saferoom/index.html),
though most of the library is not `public`, as it does not need to be.

[Android's Architecture Components](https://commonsware.com/AndroidArch)
contains a chapter dedicated to SafeRoom.

## License

The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file. The copyrights are owned by CommonsWare for things unique to this
library and a combination of CommonsWare and the Android Open Source
Project for code modified from the Architecture Components' `Framework*`
set of classes.

## Questions

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

## Release Notes

See [a separate `README`](README-1.0.0.md) for information about
the `1.0.0-alpha2` release.

### Android X

- v0.5.1: added more synchronization
- v0.5.0: released AndroidX edition

### Android Support Library

- v0.4.5: added more synchronization
- v0.4.4: addressed [thread-safety issue](https://github.com/commonsguy/cwac-saferoom/issues/27)
- v0.4.3: bumped `android.arch.persistence:db` dependency to `1.1.1`
- v0.4.2: fixed [edge case WAL issue](https://github.com/commonsguy/cwac-saferoom/issues/23)
- v0.4.1: added Room-specific tests, fixed [WAL issue](https://github.com/commonsguy/cwac-saferoom/issues/17)
- v0.4.0: updated to `1.1.0` of the support database API
- v0.3.4: changed non-WAL journal mode to TRUNCATE
- v0.3.3: added WAL support, with an assist from [plackemacher](https://github.com/commonsguy/cwac-saferoom/pull/20)
- v0.3.2: added `decrypt()` utility method
- v0.3.1: changed `rekey()` to use the existing `changePassword()`
- v0.3.0: added `rekey()`, upgraded to SQLCipher for Android 3.5.9, replaced tests
- v0.2.1: added temporary implementation of `getDatabaseName()` to `Helper`
- v0.2.0: added `SQLCipherUtils` to [help encrypt existing databases](https://github.com/commonsguy/cwac-saferoom/issues/6)
- v0.1.3: upgraded to Android Gradle Plugin 3.0.0, set transitive dependencies to `api`
- v0.1.2: fixed [issue #3](https://github.com/commonsguy/cwac-saferoom/issues/3), related to closing statements
- v0.1.1: updated support database dependency to `1.0.0`
- v0.1.0: eliminated Room dependency
- v0.0.4: raised Room dependencies to `1.0.0-beta1` and SQLCipher for Android to `3.5.7`
- v0.0.3: raised Room dependencies to `1.0.0-alpha8`
- v0.0.2: raised Room dependencies to `1.0.0-alpha5`
- v0.0.1: initial release

## Who Made This?

<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

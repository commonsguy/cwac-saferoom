# CWAC-SafeRoom: A Retired Room<->SQLCipher for Android Bridge

This project implements the `SupportSQLite...` series of classes and interfaces
that [Room](https://developer.android.com/topic/libraries/architecture/room.html)
can use for working with a particular edition of SQLite. Specficially, this
project's classes connect Room with [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/),
a version of SQLite that offers transparent encryption of its contents.

However, SQLCipher for Android now has its own implementation of
the `SupportSQLite...` classes and interfaces, eliminating the need for SafeRoom.
As such, SafeRoom is no longer under active development.

See [the SQLCipher for Android documentation](https://github.com/sqlcipher/android-database-sqlcipher#using-sqlcipher-for-android-with-room)
for more about using SQLCipher for Android with Room, SQLDelight, and other
`SupportSQLite...` clients. For support with SQLCipher for Android, please
visit [the SQLCipher support board](https://discuss.zetetic.net/c/sqlcipher).

You can still use SafeRoom if you wish &mdash; instructions can be found in
[the original `README`](./README-original.markdown).


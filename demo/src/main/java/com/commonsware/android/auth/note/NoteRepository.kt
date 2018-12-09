/***
 * Copyright (c) 2018 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain	a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * Covered in detail in the book _The Busy Coder's Guide to Android Development_
 * https://commonsware.com/Android
 */

package com.commonsware.android.auth.note

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.commonsware.cwac.saferoom.SafeHelperFactory
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import java.util.*

private const val SCHEMA = 1
private const val DATABASE_NAME = "note.db"
val EMPTY = Note(-1, null)

class NoteRepository private constructor(ctxt: Context, passphrase: CharArray) {
    private val db: SupportSQLiteDatabase

    init {
        val factory = SafeHelperFactory(passphrase)
        val cfgBuilder = SupportSQLiteOpenHelper.Configuration.builder(ctxt)

        cfgBuilder
            .name(DATABASE_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(SCHEMA) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE note (_id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT);")
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase, oldVersion: Int,
                    newVersion: Int
                ) {
                    throw RuntimeException("How did we get here?")
                }
            })

        db = factory.create(cfgBuilder.build()).writableDatabase
    }

    private class LoadObservable(ctxt: Context, private val passphrase: CharArray) : ObservableOnSubscribe<Note> {
        private val app: Context = ctxt.applicationContext

        @Throws(Exception::class)
        override fun subscribe(e: ObservableEmitter<Note>) {
            val c = NoteRepository.init(app, passphrase).db.query("SELECT _id, content FROM note")

            if (c.isAfterLast) {
                e.onNext(EMPTY)
            } else {
                c.moveToFirst()
                e.onNext(Note(c.getLong(0), c.getString(1)))
                Arrays.fill(passphrase, '\u0000')
            }

            c.close()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NoteRepository? = null

        @Synchronized
        private fun init(ctxt: Context, passphrase: CharArray): NoteRepository {
            return INSTANCE ?: NoteRepository(ctxt.applicationContext, passphrase).apply { INSTANCE = this }
        }

        @Synchronized
        private fun get(): NoteRepository {
            return INSTANCE!!
        }

        fun load(ctxt: Context, passphrase: CharArray): Observable<Note> {
            return Observable.create(LoadObservable(ctxt, passphrase))
        }

        fun save(note: Note, content: String): Note {
            val cv = ContentValues(1)

            cv.put("content", content)

            return if (note === EMPTY) {
                val id = NoteRepository.get().db.insert("note", SQLiteDatabase.CONFLICT_ABORT, cv)

                Note(id, content)
            } else {
                NoteRepository.get().db.update(
                        "note", SQLiteDatabase.CONFLICT_REPLACE, cv, "_id=?",
                        arrayOf(note.id.toString())
                    )

                Note(note.id, content)
            }
        }
    }
}

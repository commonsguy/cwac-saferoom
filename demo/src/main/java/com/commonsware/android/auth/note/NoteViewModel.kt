package com.commonsware.android.auth.note

import android.app.Application
import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File

private const val FILENAME = "sekrits.bin"
private const val KEY_NAME = "sooper-sekrit-key"
private const val TIMEOUT_SECONDS = 60

enum class AuthCase {
    RETRY_LOAD, RETRY_SAVE
}

private fun <T> LiveData<T>.post(data: T) =
    (this as MutableLiveData<T>).postValue(data)

private fun <T> LiveData<Event<T>>.postEvent(data: T) = this.post(Event(data))

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    val notes: LiveData<Note> = MutableLiveData()
    val authEvents: LiveData<Event<AuthCase>> = MutableLiveData()
    val problems: LiveData<Event<Throwable>> = MutableLiveData()
    val savedEvents: LiveData<Event<Unit>> = MutableLiveData()
    private val disposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()

        disposable.dispose()
    }

    fun load() {
        val app = getApplication<Application>()
        val encryptedFile = File(app.filesDir, FILENAME)

        disposable.add(
            RxPassphrase[encryptedFile, KEY_NAME, TIMEOUT_SECONDS]
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { chars -> NoteRepository.load(app, chars) }
                .subscribe({ note -> notes.post(note) },
                    { t ->
                        if (t is UserNotAuthenticatedException) {
                            authEvents.postEvent(AuthCase.RETRY_LOAD)
                        } else {
                            problems.postEvent(t)
                        }
                    })
        )
    }

    fun save(content: String) {
        disposable.add(Observable.just(content)
            .map { NoteRepository.save(notes.value ?: EMPTY, it) }
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ note ->
                savedEvents.postEvent(Unit)
                notes.post(note)
            },
                { t ->
                    if (t is UserNotAuthenticatedException) {
                        authEvents.postEvent(AuthCase.RETRY_SAVE)
                    } else {
                        problems.postEvent(t)
                    }
                })
        )
    }
}
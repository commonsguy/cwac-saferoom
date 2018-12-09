package com.commonsware.android.auth.note

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

open class Event<out T>(private val content: T) {
    private val hasBeenHandled = AtomicBoolean()

    fun process(block: (T) -> Unit) {
        if (!hasBeenHandled.getAndSet(true)) {
            block(content)
        }
    }
}

fun <T> LiveData<Event<T>>.observeEvent(owner: LifecycleOwner, block: (T) -> Unit) {
    observe(owner, Observer { event -> event?.process(block) })
}
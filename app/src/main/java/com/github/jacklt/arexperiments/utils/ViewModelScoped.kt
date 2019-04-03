package com.github.jacklt.arexperiments.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

open class ViewModelScoped : ViewModel(), CoroutineScope {
    private val job = Job()
    private val onClear = ArrayList<() -> Unit>()

    override val coroutineContext = Dispatchers.Main + job

    override fun onCleared() {
        super.onCleared()
        job.cancel()
        onClear.forEach { it() }
    }

    fun <T> LiveData<T>.toChannel(): ReceiveChannel<T> = Channel<T>().also { channel ->
        Observer<T> {
            launch { channel.send(it) }
        }.also { observer ->
            observeForever(observer)
            onClear += {
                channel.close()
                removeObserver(observer)
            }
        }
    }
}
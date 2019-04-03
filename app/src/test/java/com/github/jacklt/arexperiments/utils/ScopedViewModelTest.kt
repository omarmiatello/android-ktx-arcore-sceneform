package com.github.jacklt.arexperiments.utils

import androidx.lifecycle.MutableLiveData
import junit.framework.Assert.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ScopedViewModelTest {

    @Test
    fun onClear_cancelCoroutineScope() {
        object : ViewModelScoped() {
            init {
                assertTrue(coroutineContext.isActive)

                onCleared()

                assertFalse(coroutineContext.isActive)
            }
        }
    }

    @Test
    fun liveDataToChannel() {
        object : ViewModelScoped() {
            init {
                val liveData = MutableLiveData<Int>()
                val channel = liveData.toChannel()
                liveData.value = 2

                val valueFromChannel = runBlocking { channel.receive() }

                assertEquals(2, valueFromChannel)

                onCleared()
            }
        }
    }

    @Test
    fun liveDataToChannel_closeChannelOnClearResources() {
        object : ViewModelScoped() {
            init {
                val liveData = MutableLiveData<String>()
                val channel = liveData.toChannel()

                assertFalse(channel.isClosedForReceive)

                onCleared()

                assertTrue(channel.isClosedForReceive)
            }
        }
    }
}
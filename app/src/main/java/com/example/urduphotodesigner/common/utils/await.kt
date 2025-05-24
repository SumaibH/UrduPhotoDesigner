package com.example.urduphotodesigner.common.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            if (it.isCanceled) {
                cont.cancel()
            } else if (it.isSuccessful) {
                cont.resume(it.result, null)
            } else {
                cont.resumeWithException(it.exception!!)
            }
        }
    }
}
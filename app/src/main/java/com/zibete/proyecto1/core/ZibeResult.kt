package com.zibete.proyecto1.core

sealed interface ZibeResult<out T> {
    data class Success<out T>(val data: T? = null) : ZibeResult<T>
    data class Failure(val exception: Throwable) : ZibeResult<Nothing>
}
inline fun <T> ZibeResult<T>.onFailure(action: (Throwable) -> Unit): ZibeResult<T> {
    if (this is ZibeResult.Failure) action(exception)
    return this
}

inline fun <T> ZibeResult<T>.onSuccess(action: (T?) -> Unit): ZibeResult<T> {
    if (this is ZibeResult.Success) action(data)
    return this
}
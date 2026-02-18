package com.zibete.proyecto1.core.utils

sealed interface ZibeResult<out T> {
    data class Success<out T>(val data: T? = null) : ZibeResult<T>
    data class Failure(val exception: Throwable) : ZibeResult<Nothing>
}

inline fun <T> zibeCatching(block: () -> T): ZibeResult<T> =
    runCatching(block).fold(
        onSuccess = { ZibeResult.Success(it) },
        onFailure = { ZibeResult.Failure(it) }
    )

fun <T> ZibeResult<T>.getOrThrow(): T =
    when (this) {
        is ZibeResult.Success -> data as T
        is ZibeResult.Failure -> throw exception
    }

fun <T> ZibeResult<T>.getOrDefault(default: T): T =
    when (this) {
        is ZibeResult.Success -> data ?: default
        is ZibeResult.Failure -> default
    }

inline fun <T> ZibeResult<T>.onFailure(action: (Throwable) -> Unit): ZibeResult<T> {
    if (this is ZibeResult.Failure) action(exception)
    return this
}

inline fun <T> ZibeResult<T>.onSuccess(action: (T?) -> Unit): ZibeResult<T> {
    if (this is ZibeResult.Success) action(data)
    return this
}

inline fun <T : Any> ZibeResult<T?>.onSuccessNotNull(action: (T) -> Unit): ZibeResult<T?> {
    if (this is ZibeResult.Success) data?.let(action)
    return this
}

inline fun <T> ZibeResult<T>.onFinally(action: () -> Unit): ZibeResult<T> {
    action()
    return this
}


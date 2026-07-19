package io.github.daisukikaffuchino.utils

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE, initializer)
}

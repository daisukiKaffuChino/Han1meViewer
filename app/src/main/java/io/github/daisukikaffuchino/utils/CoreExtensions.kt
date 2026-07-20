package io.github.daisukikaffuchino.utils

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE, initializer)
}

inline fun <I, reified O> List<I>.mapToArray(transform: (I) -> O): Array<O> {
    return Array(size) { index -> transform(this[index]) }
}

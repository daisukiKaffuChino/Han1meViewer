package io.github.daisukikaffuchino.utils

inline fun <I, reified O> List<I>.mapToArray(transform: (I) -> O): Array<O> {
    return Array(size) { index -> transform(this[index]) }
}

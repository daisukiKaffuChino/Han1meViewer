package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

class TopLevelBackStack<T : Any>(startKey: T) {
    private val topLevelStacks = linkedMapOf<T, SnapshotStateList<T>>(
        startKey to mutableStateListOf(startKey),
    )

    var topLevelKey: T by mutableStateOf(startKey)
        private set

    val backStack = mutableStateListOf(startKey)

    val currentKey: T
        get() = backStack.last()

    private fun updateBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStacks.values.flatten())
    }

    fun addTopLevel(key: T) {
        val stack = topLevelStacks.remove(key) ?: mutableStateListOf(key)
        topLevelStacks[key] = stack
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: T, launchSingleTop: Boolean = false) {
        if (launchSingleTop && currentKey == key) return
        topLevelStacks.getValue(topLevelKey).add(key)
        updateBackStack()
    }

    fun replaceLast(key: T) {
        val currentStack = topLevelStacks.getValue(topLevelKey)
        if (currentStack.isEmpty()) return
        currentStack[currentStack.lastIndex] = key
        updateBackStack()
    }

    fun removeLast(): Boolean {
        if (backStack.size <= 1) return false

        val currentStack = topLevelStacks.getValue(topLevelKey)
        currentStack.removeAt(currentStack.lastIndex)
        if (currentStack.isEmpty()) {
            topLevelStacks.remove(topLevelKey)
            topLevelKey = topLevelStacks.keys.last()
        }
        updateBackStack()
        return true
    }

    fun popTo(key: T, inclusive: Boolean = false): Boolean {
        val targetEntry = topLevelStacks.entries.lastOrNull { (_, stack) -> key in stack }
            ?: return false
        val targetIndex = targetEntry.value.indexOfLast { it == key }
        val targetSize = targetIndex + if (inclusive) 0 else 1
        if (targetSize < 1) return false

        val topLevelKeysToRemove = topLevelStacks.keys
            .dropWhile { it != targetEntry.key }
            .drop(1)
        topLevelKeysToRemove.forEach(topLevelStacks::remove)
        while (targetEntry.value.size > targetSize) {
            targetEntry.value.removeAt(targetEntry.value.lastIndex)
        }
        topLevelKey = targetEntry.key
        updateBackStack()
        return true
    }
}

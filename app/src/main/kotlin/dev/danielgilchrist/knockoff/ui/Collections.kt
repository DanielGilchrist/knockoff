package dev.danielgilchrist.knockoff.ui

internal fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

internal fun <T> List<T>.replacedAt(
    index: Int,
    item: T,
): List<T> = mapIndexed { position, existing -> if (position == index) item else existing }

internal fun <T> List<T>.removedAt(index: Int): List<T> = filterIndexed { position, _ -> position != index }

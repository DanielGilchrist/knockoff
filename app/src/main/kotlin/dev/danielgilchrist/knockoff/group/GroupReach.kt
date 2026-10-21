package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GroupReach {
    private val reach = MutableStateFlow<ListenerReach>(ListenerReach.Unbound)

    val state: StateFlow<ListenerReach> = reach.asStateFlow()

    fun bind(reader: GroupReader) {
        reach.value = ListenerReach.Bound(reader)
    }

    fun unbind(reader: GroupReader) {
        reach.compareAndSet(ListenerReach.Bound(reader), ListenerReach.Unbound)
    }

    suspend fun discover(packageName: PackageName): GroupDiscovery =
        when (val current = reach.value) {
            is ListenerReach.Bound -> current.reader.discover(packageName)
            ListenerReach.Unbound -> GroupDiscovery.Unbound
        }
}

package dev.danielgilchrist.knockoff.system

import android.os.BadParcelableException
import android.os.RemoteException

private const val DEAD_SYSTEM = "android.os.DeadSystemRuntimeException"

internal fun <T> systemCall(call: () -> T): SystemCall<T> =
    try {
        SystemCall.Done(call())
    } catch (exception: SecurityException) {
        SystemCall.Refused(exception.toString())
    } catch (exception: BadParcelableException) {
        SystemCall.Unavailable(exception.toString())
    } catch (exception: RuntimeException) {
        unavailableOrRethrown(exception)
    }

private fun unavailableOrRethrown(exception: RuntimeException): SystemCall.Unavailable =
    when {
        exception.javaClass.name == DEAD_SYSTEM || exception.cause is RemoteException -> {
            SystemCall.Unavailable(exception.toString())
        }

        else -> {
            throw exception
        }
    }

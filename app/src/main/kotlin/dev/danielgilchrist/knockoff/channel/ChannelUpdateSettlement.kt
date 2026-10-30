package dev.danielgilchrist.knockoff.channel

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.channel.ChannelChange

internal fun ChannelUpdate.settle(change: ChannelChange): Settlement =
    when (this) {
        ChannelUpdate.Applied -> {
            Log.i(LOG_TAG, "channel ${change.describe()}")
            Settlement.Settled
        }

        ChannelUpdate.Missing -> {
            Log.w(LOG_TAG, "channel ${change.describe()} skipped, the channel is gone")
            Settlement.Settled
        }

        is ChannelUpdate.Unconfirmed -> {
            Settlement.Unsettled("channel ${change.describe()} unconfirmed: $detail")
        }

        is ChannelUpdate.NotLinked -> {
            Settlement.Unsettled("channel ${change.describe()} refused: $detail")
        }

        ChannelUpdate.Unbound -> {
            Settlement.Unsettled("channel ${change.describe()} skipped, listener unbound")
        }

        is ChannelUpdate.Unavailable -> {
            Settlement.Unsettled("channel ${change.describe()} skipped, the system didn't send it: $detail")
        }
    }

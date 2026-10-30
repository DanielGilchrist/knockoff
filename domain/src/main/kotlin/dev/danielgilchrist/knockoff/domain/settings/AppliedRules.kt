package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import java.time.ZoneId

data class AppliedRules(
    val rules: RuleSet,
    val zone: ZoneId,
) {
    companion object {
        fun fromJson(text: String): AppliedRulesDecode =
            when (
                val decode =
                    VersionedJson.decode<StoredAppliedRulesDocument>(
                        text,
                        mapOf(
                            StoredAppliedRulesV1.VERSION to StoredAppliedRulesV1.serializer(),
                            StoredAppliedRules.CURRENT_VERSION to StoredAppliedRules.serializer(),
                        ),
                    )
            ) {
                is VersionedDecode.Decoded -> decode.value.toBaseline()
                is VersionedDecode.Unreadable -> AppliedRulesDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> AppliedRulesDecode.UnsupportedVersion(decode.version)
            }

        fun toJson(baseline: Baseline.Known<AppliedRules>): String =
            VersionedJson.json.encodeToString(StoredAppliedRules.from(baseline))
    }

    fun evaluation(
        chats: SeenChats,
        holidays: Holidays,
    ): Evaluation = Evaluation(chats.expand(rules), zone, holidays)
}

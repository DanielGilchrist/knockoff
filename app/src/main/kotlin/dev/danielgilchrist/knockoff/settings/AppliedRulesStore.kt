package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules
import dev.danielgilchrist.knockoff.domain.settings.AppliedRulesDecode
import dev.danielgilchrist.knockoff.storage.FileRead
import dev.danielgilchrist.knockoff.storage.FileWrite
import dev.danielgilchrist.knockoff.storage.StagedFile
import java.io.File

class AppliedRulesStore(
    file: File,
) : AppliedRulesKeeper {
    private val file = StagedFile(file)

    override suspend fun load(): AppliedRulesLoad =
        when (val read = file.read()) {
            FileRead.Missing -> {
                AppliedRulesLoad.Missing
            }

            is FileRead.Failed -> {
                AppliedRulesLoad.Failed(read.detail)
            }

            is FileRead.Undecodable -> {
                AppliedRulesLoad.Failed(read.detail)
            }

            is FileRead.Text -> {
                when (val decode = AppliedRules.fromJson(read.text)) {
                    is AppliedRulesDecode.Decoded -> AppliedRulesLoad.Loaded(decode.baseline)
                    is AppliedRulesDecode.Failure -> AppliedRulesLoad.Failed(decode.detail)
                }
            }
        }

    override suspend fun save(baseline: Baseline.Known<AppliedRules>): AppliedRulesSave =
        when (val write = file.write(AppliedRules.toJson(baseline))) {
            FileWrite.Written -> AppliedRulesSave.Saved
            is FileWrite.Failed -> AppliedRulesSave.Failed(write.detail)
        }
}

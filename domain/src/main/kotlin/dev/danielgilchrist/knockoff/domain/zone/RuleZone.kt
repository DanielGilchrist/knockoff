package dev.danielgilchrist.knockoff.domain.zone

sealed interface RuleZone {
    data object Default : RuleZone

    data class Chosen(
        val choice: ZoneChoice,
    ) : RuleZone

    companion object {
        private const val DEFAULT = "default"

        fun parse(raw: String): RuleZoneParse =
            when (raw) {
                DEFAULT -> {
                    RuleZoneParse.Valid(Default)
                }

                else -> {
                    when (val parse = ZoneChoice.parse(raw)) {
                        is ZoneChoiceParse.Valid -> RuleZoneParse.Valid(Chosen(parse.choice))
                        is ZoneChoiceParse.Unknown -> RuleZoneParse.Unknown(parse)
                    }
                }
            }
    }

    fun resolve(default: ZoneChoice): ZoneChoice =
        when (this) {
            Default -> default
            is Chosen -> choice
        }

    fun stored(): String =
        when (this) {
            Default -> DEFAULT
            is Chosen -> choice.stored()
        }
}

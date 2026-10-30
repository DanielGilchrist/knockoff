package dev.danielgilchrist.knockoff.domain.dayoff

sealed interface HolidayKind {
    data object Everywhere : HolidayKind

    data class Regional(
        val regions: Set<HolidayRegion>,
    ) : HolidayKind

    data object Observance : HolidayKind

    companion object {
        private const val REGIONAL = "Public holiday in "
        private const val OBSERVANCE = "Observance"

        fun of(description: String): HolidayKind {
            val line = description.substringBefore('\n').trim()

            return when {
                line.startsWith(REGIONAL) -> regional(line.removePrefix(REGIONAL))
                line == OBSERVANCE -> Observance
                else -> Everywhere
            }
        }

        private fun regional(list: String): HolidayKind {
            val regions =
                list.split(",").flatMap { raw ->
                    when (val parse = HolidayRegion.parse(raw)) {
                        is HolidayRegionParse.Valid -> listOf(parse.region)
                        HolidayRegionParse.Blank -> emptyList()
                    }
                }

            return when {
                regions.isEmpty() -> Everywhere
                else -> Regional(regions.toSet())
            }
        }
    }

    fun observedIn(chosen: Set<HolidayRegion>): Boolean =
        when (this) {
            Everywhere -> true
            is Regional -> regions.any { it in chosen }
            Observance -> false
        }
}

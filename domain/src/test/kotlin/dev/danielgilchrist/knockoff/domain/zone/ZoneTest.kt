package dev.danielgilchrist.knockoff.domain.zone

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.ZoneId

class ZoneTest {
    private val london = ZoneId.of("Europe/London")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun `zone choices parse from their stored form`() {
        ZoneChoice.parse("device") shouldBe ZoneChoiceParse.Valid(ZoneChoice.Device)
        ZoneChoice.parse("Asia/Tokyo") shouldBe ZoneChoiceParse.Valid(ZoneChoice.Fixed(tokyo))
        ZoneChoice.parse("Mars/Base").shouldBeInstanceOf<ZoneChoiceParse.Unknown>().raw shouldBe "Mars/Base"
    }

    @Test
    fun `zone choices store as device or the zone id`() {
        ZoneChoice.Device.stored() shouldBe "device"
        ZoneChoice.Fixed(tokyo).stored() shouldBe "Asia/Tokyo"
    }

    @Test
    fun `the device choice follows the device and a fixed choice doesn't`() {
        ZoneChoice.Device.zoneFor(london) shouldBe london
        ZoneChoice.Fixed(tokyo).zoneFor(london) shouldBe tokyo
    }

    @Test
    fun `rule zones parse from their stored form`() {
        RuleZone.parse("default") shouldBe RuleZoneParse.Valid(RuleZone.Default)
        RuleZone.parse("device") shouldBe RuleZoneParse.Valid(RuleZone.Chosen(ZoneChoice.Device))
        RuleZone.parse("Asia/Tokyo") shouldBe RuleZoneParse.Valid(RuleZone.Chosen(ZoneChoice.Fixed(tokyo)))

        val unknown = RuleZone.parse("Mars/Base").shouldBeInstanceOf<RuleZoneParse.Unknown>()

        unknown.cause.raw shouldBe "Mars/Base"
    }

    @Test
    fun `rule zones store and resolve against the default`() {
        RuleZone.Default.stored() shouldBe "default"
        RuleZone.Chosen(ZoneChoice.Fixed(tokyo)).stored() shouldBe "Asia/Tokyo"
        RuleZone.Default.resolve(ZoneChoice.Fixed(london)) shouldBe ZoneChoice.Fixed(london)
        RuleZone.Chosen(ZoneChoice.Device).resolve(ZoneChoice.Fixed(london)) shouldBe ZoneChoice.Device
    }
}

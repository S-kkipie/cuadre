package pe.aido.cuadre

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet
import pe.aido.cuadre.sync.SyncCodec

class SyncCodecTest {

    private val yape = ConfirmedPayment(
        "YAPE:25.00:1790940118437",
        PaymentEvent(Wallet.YAPE, 25.0, "ROSA HUAMAN", PaymentDirection.INCOMING, 1790940118437, "raw", "418"),
    )

    @Test fun uploadBodyMatchesBackendSchema() {
        val j = JSONObject(SyncCodec.uploadBody(yape))
        assertEquals("YAPE:25.00:1790940118437", j.getString("id"))
        assertEquals("YAPE", j.getString("wallet"))
        assertEquals(25.0, j.getDouble("amount"), 0.001)
        assertEquals("ROSA HUAMAN", j.getString("counterparty"))
        assertEquals("418", j.getString("securityCode"))
        assertEquals(1790940118437, j.getLong("postedAt"))
    }

    @Test fun pushRoundTripsToTheSamePayment() {
        val r = SyncCodec.fromPush(
            mapOf(
                "type" to "payment", "id" to yape.id, "wallet" to "YAPE", "amount" to "25.00",
                "counterparty" to "ROSA HUAMAN", "securityCode" to "418", "postedAt" to "1790940118437",
                "fromDevice" to "Caja", "storeName" to "Bodega Rosa",
            ),
        )!!
        assertEquals(yape.id, r.payment.id)               // same id → never doubled locally
        assertEquals(yape.event.copy(rawText = "remoto"), r.payment.event)
        assertEquals("Caja", r.fromDevice)
    }

    @Test fun malformedPushesAreDropped() {
        assertNull(SyncCodec.fromPush(mapOf("type" to "other")))
        assertNull(SyncCodec.fromPush(mapOf("type" to "payment", "id" to "x", "amount" to "abc", "postedAt" to "1")))
        assertNull(SyncCodec.fromPush(mapOf("type" to "payment", "id" to "x", "amount" to "-5", "postedAt" to "1")))
        assertNull(SyncCodec.fromPush(mapOf("type" to "payment", "amount" to "5", "postedAt" to "1")))
    }

    @Test fun parsesPairResponse() {
        val p = SyncCodec.parsePaired(
            """{"response":{"deviceId":"d1","token":"t0k","storeId":"s1","storeName":"Bodega Rosa"},"code":"CREATED","status":201}""",
        )
        assertEquals(SyncCodec.Paired("d1", "t0k", "s1", "Bodega Rosa"), p)
    }

    @Test fun storePaymentsKeepSourcePhoneAndDropUnreadableRows() {
        val list = SyncCodec.parseStorePayments(
            """{"response":[
              {"id":"p1","wallet":"YAPE","amount":18.5,"counterparty":"Carmen Quispe","securityCode":"274",
               "postedAt":"2026-10-03T21:02:11.000Z","sourceDevice":"Caja 2"},
              {"id":"p2","wallet":"PLIN_BBVA","amount":12,"counterparty":null,"securityCode":null,
               "postedAt":"2026-10-03T20:56:00.000Z","sourceDevice":null},
              {"id":"bad","wallet":"YAPE","amount":0,"postedAt":"2026-10-03T20:00:00.000Z"},
              {"id":"bad2","wallet":"YAPE","amount":5,"postedAt":"ayer"}
            ]}""",
        )
        assertEquals(listOf("p1", "p2"), list.map { it.id })
        assertEquals("Caja 2", list[0].fromDevice)
        assertEquals(1791061331000, list[0].event.postedAtMillis)
        assertEquals("274", list[0].event.securityCode)
        assertNull(list[1].fromDevice)
        assertNull(list[1].event.counterparty)
    }

    @Test fun pushCarriesSourcePhone() {
        val r = SyncCodec.fromPush(
            mapOf("type" to "payment", "id" to "p1", "amount" to "18.50", "postedAt" to "1790974931000",
                "wallet" to "YAPE", "fromDevice" to "Caja 2"),
        )
        assertEquals("Caja 2", r?.payment?.fromDevice)
    }

    @Test
    fun `latest app reads version and download page`() {
        val latest = SyncCodec.parseLatestApp(
            """{"response":{"versionCode":2,"versionName":"1.0.1","url":"https://cuadre.aido.lat/descargar"},"code":"OK","status":200}""",
        )
        assertEquals(2, latest.versionCode)
        assertEquals("1.0.1", latest.versionName)
        assertEquals("https://cuadre.aido.lat/descargar", latest.url)
    }
}

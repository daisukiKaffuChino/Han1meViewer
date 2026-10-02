package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class EchDohParserTest {

    @Test
    fun parsesEchParameterFromHttpsRecord() {
        val ech = byteArrayOf(0x00, 0x03, 0x01, 0x02, 0x03)
        val message = dnsResponseWithHttpsEch("cloudflare-ech.com", ech, ttlSeconds = 300)

        val parsed = EchDoh.parseSvcbEch(message)

        assertNotNull(parsed)
        assertArrayEquals(ech, parsed!!.first)
        assertEquals(300_000L, parsed.second)
    }

    @Test
    fun ignoresHttpsRecordWithoutEchParameter() {
        val message = dnsResponseWithHttpsEch("cloudflare-ech.com", null, ttlSeconds = 60)

        assertEquals(null, EchDoh.parseSvcbEch(message))
    }

    private fun dnsResponseWithHttpsEch(
        host: String,
        ech: ByteArray?,
        ttlSeconds: Long,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(
            byteArrayOf(
                0x12, 0x34, 0x81.toByte(), 0x80.toByte(), 0, 1, 0, 1, 0, 0, 0, 0,
            ),
        )
        writeName(output, host)
        output.write(byteArrayOf(0x00, 65, 0x00, 1))
        output.write(byteArrayOf(0xC0.toByte(), 0x0C))
        output.write(byteArrayOf(0x00, 65, 0x00, 1))
        writeInt(output, ttlSeconds)

        val rdata = ByteArrayOutputStream()
        rdata.write(0) // SvcPriority
        rdata.write(0) // SvcPriority low byte
        rdata.write(0) // TargetName root
        if (ech != null) {
            rdata.write(0)
            rdata.write(5) // SvcParamKey=ech
            rdata.write((ech.size ushr 8) and 0xFF)
            rdata.write(ech.size and 0xFF)
            rdata.write(ech)
        }
        val data = rdata.toByteArray()
        output.write((data.size ushr 8) and 0xFF)
        output.write(data.size and 0xFF)
        output.write(data)
        return output.toByteArray()
    }

    private fun writeName(output: ByteArrayOutputStream, host: String) {
        host.split('.').forEach { label ->
            output.write(label.length)
            output.write(label.toByteArray(Charsets.US_ASCII))
        }
        output.write(0)
    }

    private fun writeInt(output: ByteArrayOutputStream, value: Long) {
        output.write(((value ushr 24) and 0xFF).toInt())
        output.write(((value ushr 16) and 0xFF).toInt())
        output.write(((value ushr 8) and 0xFF).toInt())
        output.write((value and 0xFF).toInt())
    }
}

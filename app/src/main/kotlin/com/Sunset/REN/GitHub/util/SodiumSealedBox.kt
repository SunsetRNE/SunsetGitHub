package com.Sunset.REN.GitHub.util

/** Minimal crypto_box_seal helper for GitHub Actions Secrets without native libsodium. */
object SodiumSealedBox {
    private const val PublicKeyBytes = 32
    private const val SecretKeyBytes = 32
    private const val NonceBytes = 24
    private const val MacBytes = 16

    fun seal(message: ByteArray, recipientPublicKey: ByteArray): ByteArray {
        require(recipientPublicKey.size == PublicKeyBytes) { "GitHub public key must be 32 bytes." }
        val ephemeralSecret = ByteArray(SecretKeyBytes).also { java.security.SecureRandom().nextBytes(it) }
        ephemeralSecret[0] = (ephemeralSecret[0].toInt() and 248).toByte()
        ephemeralSecret[31] = (ephemeralSecret[31].toInt() and 127).toByte()
        ephemeralSecret[31] = (ephemeralSecret[31].toInt() or 64).toByte()
        val ephemeralPublic = scalarMultBase(ephemeralSecret)
        val shared = scalarMult(ephemeralSecret, recipientPublicKey)
        val nonce = java.security.MessageDigest.getInstance("SHA-512").digest(ephemeralPublic + recipientPublicKey).copyOf(NonceBytes)
        val cipher = secretBox(message, nonce, hsalsa20(ByteArray(16), shared))
        ephemeralSecret.fill(0); shared.fill(0)
        return ephemeralPublic + cipher
    }

    private fun secretBox(message: ByteArray, nonce: ByteArray, key: ByteArray): ByteArray {
        val subKey = hsalsa20(nonce.copyOfRange(0, 16), key)
        val stream = salsa20Stream(message.size + MacBytes, nonce.copyOfRange(16, 24), subKey)
        val polyKey = stream.copyOfRange(0, 32)
        val encrypted = ByteArray(message.size)
        for (i in message.indices) encrypted[i] = (message[i].toInt() xor stream[MacBytes + i].toInt()).toByte()
        return poly1305(encrypted, polyKey) + encrypted
    }

    private fun hsalsa20(nonce16: ByteArray, key: ByteArray): ByteArray {
        val x = salsaInitialState(key, nonce16, true)
        salsaCore(x)
        val out = ByteArray(32)
        intToLittle(x[0], out, 0); intToLittle(x[5], out, 4); intToLittle(x[10], out, 8); intToLittle(x[15], out, 12)
        intToLittle(x[6], out, 16); intToLittle(x[7], out, 20); intToLittle(x[8], out, 24); intToLittle(x[9], out, 28)
        return out
    }

    private fun salsa20Stream(size: Int, nonce8: ByteArray, key: ByteArray): ByteArray {
        val out = ByteArray(size); var offset = 0; var counter = 0L
        while (offset < size) {
            val block = salsa20Block(key, nonce8, counter++)
            val take = minOf(64, size - offset)
            System.arraycopy(block, 0, out, offset, take)
            offset += take
        }
        return out
    }

    private fun salsa20Block(key: ByteArray, nonce8: ByteArray, counter: Long): ByteArray {
        val state = salsaInitialState(key, nonce8, false)
        state[8] = counter.toInt(); state[9] = (counter ushr 32).toInt()
        val working = state.copyOf(); salsaCore(working)
        val out = ByteArray(64)
        for (i in 0 until 16) intToLittle(working[i] + state[i], out, i * 4)
        return out
    }

    private fun salsaInitialState(key: ByteArray, nonce: ByteArray, hsalsa: Boolean): IntArray {
        val c = "expand 32-byte k".toByteArray(Charsets.US_ASCII)
        return intArrayOf(
            littleToInt(c, 0), littleToInt(key, 0), littleToInt(key, 4), littleToInt(key, 8),
            littleToInt(key, 12), littleToInt(c, 4), littleToInt(nonce, 0), littleToInt(nonce, 4),
            if (hsalsa) littleToInt(nonce, 8) else 0, if (hsalsa) littleToInt(nonce, 12) else 0,
            littleToInt(c, 8), littleToInt(key, 16), littleToInt(key, 20), littleToInt(key, 24),
            littleToInt(key, 28), littleToInt(c, 12)
        )
    }

    private fun salsaCore(x: IntArray) {
        repeat(10) {
            qr(x, 0, 4, 8, 12); qr(x, 5, 9, 13, 1); qr(x, 10, 14, 2, 6); qr(x, 15, 3, 7, 11)
            qr(x, 0, 1, 2, 3); qr(x, 5, 6, 7, 4); qr(x, 10, 11, 8, 9); qr(x, 15, 12, 13, 14)
        }
    }

    private fun qr(x: IntArray, a: Int, b: Int, c: Int, d: Int) {
        x[b] = x[b] xor Integer.rotateLeft(x[a] + x[d], 7)
        x[c] = x[c] xor Integer.rotateLeft(x[b] + x[a], 9)
        x[d] = x[d] xor Integer.rotateLeft(x[c] + x[b], 13)
        x[a] = x[a] xor Integer.rotateLeft(x[d] + x[c], 18)
    }

    private fun poly1305(message: ByteArray, key: ByteArray): ByteArray {
        val p = java.math.BigInteger.ONE.shiftLeft(130).subtract(java.math.BigInteger.valueOf(5))
        val rBytes = key.copyOfRange(0, 16)
        rBytes[3] = (rBytes[3].toInt() and 15).toByte(); rBytes[7] = (rBytes[7].toInt() and 15).toByte(); rBytes[11] = (rBytes[11].toInt() and 15).toByte(); rBytes[15] = (rBytes[15].toInt() and 15).toByte()
        rBytes[4] = (rBytes[4].toInt() and 252).toByte(); rBytes[8] = (rBytes[8].toInt() and 252).toByte(); rBytes[12] = (rBytes[12].toInt() and 252).toByte()
        val r = leBigInt(rBytes); val s = leBigInt(key.copyOfRange(16, 32))
        var acc = java.math.BigInteger.ZERO; var offset = 0
        while (offset < message.size) {
            val len = minOf(16, message.size - offset)
            val block = ByteArray(len + 1)
            System.arraycopy(message, offset, block, 0, len); block[len] = 1
            acc = acc.add(leBigInt(block)).multiply(r).mod(p)
            offset += len
        }
        return bigIntToLe(acc.add(s).mod(java.math.BigInteger.ONE.shiftLeft(128)), 16)
    }

    private fun scalarMultBase(secret: ByteArray): ByteArray = scalarMult(secret, BasePoint)

    private fun scalarMult(secret: ByteArray, point: ByteArray): ByteArray {
        val p = java.math.BigInteger.ONE.shiftLeft(255).subtract(java.math.BigInteger.valueOf(19))
        val a24 = java.math.BigInteger.valueOf(121665)
        val k = secret.copyOf().also { it[0] = (it[0].toInt() and 248).toByte(); it[31] = (it[31].toInt() and 127).toByte(); it[31] = (it[31].toInt() or 64).toByte() }
        val x1 = leBigInt(point).mod(p)
        var x2 = java.math.BigInteger.ONE; var z2 = java.math.BigInteger.ZERO; var x3 = x1; var z3 = java.math.BigInteger.ONE; var swap = 0
        for (t in 254 downTo 0) {
            val kt = (k[t / 8].toInt() ushr (t and 7)) and 1
            swap = swap xor kt
            if (swap == 1) { val tx = x2; x2 = x3; x3 = tx; val tz = z2; z2 = z3; z3 = tz }
            swap = kt
            val a = x2.add(z2).mod(p); val aa = a.multiply(a).mod(p)
            val b = x2.subtract(z2).mod(p); val bb = b.multiply(b).mod(p)
            val e = aa.subtract(bb).mod(p)
            val c = x3.add(z3).mod(p); val d = x3.subtract(z3).mod(p)
            val da = d.multiply(a).mod(p); val cb = c.multiply(b).mod(p)
            x3 = da.add(cb).mod(p).pow(2).mod(p)
            z3 = x1.multiply(da.subtract(cb).mod(p).pow(2).mod(p)).mod(p)
            x2 = aa.multiply(bb).mod(p)
            z2 = e.multiply(aa.add(a24.multiply(e)).mod(p)).mod(p)
        }
        if (swap == 1) { val tx = x2; x2 = x3; x3 = tx; val tz = z2; z2 = z3; z3 = tz }
        return bigIntToLe(x2.multiply(z2.modInverse(p)).mod(p), 32)
    }

    private fun littleToInt(input: ByteArray, offset: Int): Int = (input[offset].toInt() and 255) or ((input[offset + 1].toInt() and 255) shl 8) or ((input[offset + 2].toInt() and 255) shl 16) or (input[offset + 3].toInt() shl 24)
    private fun intToLittle(value: Int, out: ByteArray, offset: Int) { out[offset] = value.toByte(); out[offset + 1] = (value ushr 8).toByte(); out[offset + 2] = (value ushr 16).toByte(); out[offset + 3] = (value ushr 24).toByte() }
    private fun leBigInt(bytes: ByteArray): java.math.BigInteger = java.math.BigInteger(1, bytes.reversedArray())
    private fun bigIntToLe(value: java.math.BigInteger, size: Int): ByteArray = value.toByteArray().let { be -> ByteArray(size) { index -> be.getOrNull(be.size - 1 - index) ?: 0 } }
    private val BasePoint = ByteArray(32).also { it[0] = 9 }
}

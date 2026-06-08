# Minimal Jam/Cue for Kotlin

Subset implementation for Controller conn.sock integration. Only needs to handle %peel requests and responses - no full Nock runtime.

## Noun Types

```kotlin
sealed class Noun {
    abstract fun jam(): BigInteger
}

data class Atom(val value: BigInteger) : Noun() {
    constructor(v: Long) : this(BigInteger.valueOf(v))
    constructor(cord: String) : this(cordToBigInt(cord))
}

data class Cell(val head: Noun, val tail: Noun) : Noun()
```

## Cord Encoding

```kotlin
fun cordToBigInt(s: String): BigInteger {
    if (s.isEmpty()) return BigInteger.ZERO
    val bytes = s.toByteArray(Charsets.UTF_8)
    // Little-endian: reverse bytes
    return BigInteger(1, bytes.reversedArray())
}

fun bigIntToCord(n: BigInteger): String {
    if (n == BigInteger.ZERO) return ""
    val bytes = n.toByteArray()
    // Remove sign byte if present, reverse for little-endian
    val start = if (bytes[0] == 0.toByte()) 1 else 0
    return String(bytes.sliceArray(start until bytes.size).reversedArray(), Charsets.UTF_8)
}
```

## Jam Algorithm

Jam produces a bitstream. Reference: https://developers.urbit.org/reference/hoon/stdlib/2p#jam

```kotlin
fun jam(noun: Noun): BigInteger {
    val bits = mutableListOf<Boolean>()
    val cache = mutableMapOf<Noun, Int>() // noun -> bit position

    fun mat(atom: BigInteger): List<Boolean> {
        // Encode atom length, then atom bits
        if (atom == BigInteger.ZERO) return listOf(true) // just 1
        val bitLen = atom.bitLength()
        val lenLen = bitLen.toBigInteger().bitLength()
        val result = mutableListOf<Boolean>()
        // Length prefix: lenLen zeros, then 1, then (lenLen-1) low bits of bitLen
        repeat(lenLen) { result.add(false) }
        result.add(true)
        for (i in 0 until lenLen - 1) {
            result.add(bitLen and (1 shl i) != 0)
        }
        // Then the atom bits
        for (i in 0 until bitLen) {
            result.add(atom.testBit(i))
        }
        return result
    }

    fun jamInner(n: Noun, pos: Int): Int {
        val cached = cache[n]
        if (cached != null && cached < pos) {
            // Back-reference: 11 + mat(cached)
            bits.add(true)
            bits.add(true)
            bits.addAll(mat(cached.toBigInteger()))
            return bits.size
        }

        cache[n] = pos

        when (n) {
            is Atom -> {
                // Atom: 0 + mat(value)
                bits.add(false)
                bits.addAll(mat(n.value))
            }
            is Cell -> {
                // Cell: 10 + jam(head) + jam(tail)
                bits.add(true)
                bits.add(false)
                jamInner(n.head, bits.size)
                jamInner(n.tail, bits.size)
            }
        }
        return bits.size
    }

    jamInner(noun, 0)

    // Convert bits to BigInteger (little-endian)
    var result = BigInteger.ZERO
    for (i in bits.indices) {
        if (bits[i]) result = result.setBit(i)
    }
    return result
}
```

## Cue Algorithm

```kotlin
fun cue(jammed: BigInteger): Noun {
    val cache = mutableMapOf<Int, Noun>()

    fun rub(pos: Int): Pair<Int, BigInteger> {
        // Decode mat-encoded atom, return (bits consumed, value)
        var zeros = 0
        while (!jammed.testBit(pos + zeros)) zeros++
        if (zeros == 0) return Pair(1, BigInteger.ZERO)

        val lenLen = zeros
        var bitLen = 1 shl (lenLen - 1) // High bit implied
        for (i in 0 until lenLen - 1) {
            if (jammed.testBit(pos + lenLen + 1 + i)) {
                bitLen = bitLen or (1 shl i)
            }
        }

        var value = BigInteger.ZERO
        val dataStart = pos + lenLen + lenLen
        for (i in 0 until bitLen) {
            if (jammed.testBit(dataStart + i)) {
                value = value.setBit(i)
            }
        }

        return Pair(lenLen + lenLen + bitLen, value)
    }

    fun cueInner(pos: Int): Pair<Int, Noun> {
        return if (!jammed.testBit(pos)) {
            // 0 prefix: atom
            val (consumed, value) = rub(pos + 1)
            val noun = Atom(value)
            cache[pos] = noun
            Pair(1 + consumed, noun)
        } else if (!jammed.testBit(pos + 1)) {
            // 10 prefix: cell
            cache[pos] = Atom(BigInteger.ZERO) // placeholder
            val (headBits, head) = cueInner(pos + 2)
            val (tailBits, tail) = cueInner(pos + 2 + headBits)
            val noun = Cell(head, tail)
            cache[pos] = noun
            Pair(2 + headBits + tailBits, noun)
        } else {
            // 11 prefix: back-reference
            val (consumed, ref) = rub(pos + 2)
            val noun = cache[ref.toInt()] ?: error("Invalid back-reference")
            Pair(2 + consumed, noun)
        }
    }

    return cueInner(0).second
}
```

## Newt Framing

```kotlin
fun newtEncode(jammed: BigInteger): ByteArray {
    val payload = jammedToBytes(jammed)
    val frame = ByteArray(5 + payload.size)
    frame[0] = 0x00 // version
    // Little-endian length
    frame[1] = (payload.size and 0xFF).toByte()
    frame[2] = ((payload.size shr 8) and 0xFF).toByte()
    frame[3] = ((payload.size shr 16) and 0xFF).toByte()
    frame[4] = ((payload.size shr 24) and 0xFF).toByte()
    System.arraycopy(payload, 0, frame, 5, payload.size)
    return frame
}

fun newtDecode(data: ByteArray): BigInteger? {
    if (data.size < 5 || data[0] != 0x00.toByte()) return null
    val len = (data[1].toInt() and 0xFF) or
              ((data[2].toInt() and 0xFF) shl 8) or
              ((data[3].toInt() and 0xFF) shl 16) or
              ((data[4].toInt() and 0xFF) shl 24)
    if (data.size < 5 + len) return null
    return bytesToJammed(data.sliceArray(5 until 5 + len))
}

fun jammedToBytes(n: BigInteger): ByteArray {
    if (n == BigInteger.ZERO) return byteArrayOf(0)
    val bytes = n.toByteArray()
    // Remove sign byte, reverse for little-endian
    val start = if (bytes[0] == 0.toByte()) 1 else 0
    return bytes.sliceArray(start until bytes.size).reversedArray()
}

fun bytesToJammed(bytes: ByteArray): BigInteger {
    if (bytes.isEmpty()) return BigInteger.ZERO
    return BigInteger(1, bytes.reversedArray())
}
```

## Building %peel Requests

```kotlin
fun buildPeelRequest(requestId: Long, command: String): Noun {
    // [rid %peel [%cmd ~]]
    val path = Cell(Atom(command), Atom(0)) // [%cmd ~]
    val peel = Atom("peel")
    return Cell(Atom(requestId), Cell(peel, path))
}

// Usage:
val liveReq = buildPeelRequest(1, "live")  // [1 %peel %live ~]
val whoReq = buildPeelRequest(2, "who")    // [2 %peel %who ~]
```

## Parsing Responses

```kotlin
fun parseResponse(noun: Noun): Pair<Long, Noun?> {
    // Response: [rid result]
    // result for %peel: [~ value] or ~
    val cell = noun as? Cell ?: error("Response not a cell")
    val rid = (cell.head as? Atom)?.value?.toLong() ?: error("Invalid rid")

    val result = cell.tail
    if (result is Atom && result.value == BigInteger.ZERO) {
        return Pair(rid, null) // ~ = none
    }
    if (result is Cell && result.head is Atom &&
        (result.head as Atom).value == BigInteger.ZERO) {
        return Pair(rid, result.tail) // [~ value] = some
    }
    return Pair(rid, result)
}

fun parseLiveResponse(noun: Noun): Boolean {
    val (_, value) = parseResponse(noun)
    // %.y = 0, %.n = 1
    return value is Atom && value.value == BigInteger.ZERO
}

fun parseWhoResponse(noun: Noun): BigInteger? {
    val (_, value) = parseResponse(noun)
    return (value as? Atom)?.value
}

fun parseVersionResponse(noun: Noun): String? {
    val (_, value) = parseResponse(noun)
    return (value as? Atom)?.let { bigIntToCord(it.value) }
}
```

## Test Vectors

```kotlin
// From working conn-client.js tests:

// %peel %live request jams to: 0x71c087b2b2b63207cb7b2b
// Response [1 ~ %.y] cues to: Cell(Atom(1), Cell(Atom(0), Atom(0)))

// %peel %who response contains @p as raw BigInteger
// %peel %v response contains version cord
```

## Size Estimate

- Noun classes: ~30 lines
- Cord encoding: ~20 lines
- Jam: ~60 lines
- Cue: ~50 lines
- Newt framing: ~30 lines
- Request/response helpers: ~40 lines

**Total: ~230 lines of Kotlin**

No external dependencies beyond standard library BigInteger.

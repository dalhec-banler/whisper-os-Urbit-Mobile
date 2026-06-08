package io.nativeplanet.controller;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal jam/cue implementation for conn.sock %peel protocol.
 * Only handles atoms and cells needed for health check requests/responses.
 */
public class NounCodec {

    // --- Noun types ---

    public static abstract class Noun {
        public abstract boolean isAtom();
        public boolean isCell() { return !isAtom(); }
    }

    public static class Atom extends Noun {
        public final BigInteger value;

        public Atom(BigInteger value) {
            this.value = value;
        }

        public Atom(long value) {
            this.value = BigInteger.valueOf(value);
        }

        public static Atom fromCord(String s) {
            if (s == null || s.isEmpty()) {
                return new Atom(BigInteger.ZERO);
            }
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            // Little-endian: reverse bytes
            byte[] reversed = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                reversed[i] = bytes[bytes.length - 1 - i];
            }
            return new Atom(new BigInteger(1, reversed));
        }

        public String toCord() {
            if (value.equals(BigInteger.ZERO)) {
                return "";
            }
            byte[] bytes = value.toByteArray();
            // Remove sign byte if present
            int start = (bytes[0] == 0 && bytes.length > 1) ? 1 : 0;
            // Reverse for little-endian
            byte[] result = new byte[bytes.length - start];
            for (int i = 0; i < result.length; i++) {
                result[i] = bytes[bytes.length - 1 - i];
            }
            return new String(result, StandardCharsets.UTF_8);
        }

        @Override
        public boolean isAtom() { return true; }

        @Override
        public String toString() {
            if (value.bitLength() <= 32) {
                return value.toString();
            }
            return "0x" + value.toString(16);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Atom)) return false;
            return value.equals(((Atom) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        public static final Atom ZERO = new Atom(BigInteger.ZERO);
    }

    public static class Cell extends Noun {
        public final Noun head;
        public final Noun tail;

        public Cell(Noun head, Noun tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public boolean isAtom() { return false; }

        @Override
        public String toString() {
            return "[" + head + " " + tail + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cell)) return false;
            Cell cell = (Cell) o;
            return head.equals(cell.head) && tail.equals(cell.tail);
        }

        @Override
        public int hashCode() {
            return 31 * head.hashCode() + tail.hashCode();
        }
    }

    // --- Jam ---

    public static BigInteger jam(Noun noun) {
        if (noun == null) {
            throw new IllegalArgumentException("Cannot jam null noun");
        }
        List<Boolean> bits = new ArrayList<>();
        Map<Noun, Integer> cache = new HashMap<>();
        jamInner(noun, 0, bits, cache);
        return bitsToNumber(bits);
    }

    private static int jamInner(Noun noun, int pos, List<Boolean> bits, Map<Noun, Integer> cache) {
        Integer cached = cache.get(noun);
        if (cached != null && cached < pos) {
            // Back-reference: 11 + mat(cached)
            bits.add(true);
            bits.add(true);
            matInto(BigInteger.valueOf(cached), bits);
            return bits.size();
        }

        cache.put(noun, pos);

        if (noun.isAtom()) {
            // Atom: 0 + mat(value)
            bits.add(false);
            matInto(((Atom) noun).value, bits);
        } else {
            // Cell: 10 + jam(head) + jam(tail)
            Cell cell = (Cell) noun;
            bits.add(true);
            bits.add(false);
            jamInner(cell.head, bits.size(), bits, cache);
            jamInner(cell.tail, bits.size(), bits, cache);
        }
        return bits.size();
    }

    private static void matInto(BigInteger atom, List<Boolean> bits) {
        if (atom.equals(BigInteger.ZERO)) {
            bits.add(true); // just 1
            return;
        }

        int bitLen = atom.bitLength();
        int lenLen = BigInteger.valueOf(bitLen).bitLength();

        // Length prefix: lenLen zeros, then 1, then (lenLen-1) low bits of bitLen
        for (int i = 0; i < lenLen; i++) {
            bits.add(false);
        }
        bits.add(true);

        for (int i = 0; i < lenLen - 1; i++) {
            bits.add((bitLen & (1 << i)) != 0);
        }

        // Atom bits
        for (int i = 0; i < bitLen; i++) {
            bits.add(atom.testBit(i));
        }
    }

    private static BigInteger bitsToNumber(List<Boolean> bits) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                result = result.setBit(i);
            }
        }
        return result;
    }

    // --- Cue ---

    public static Noun cue(BigInteger jammed) {
        Map<Integer, Noun> cache = new HashMap<>();
        return cueInner(jammed, 0, cache).noun;
    }

    private static class CueResult {
        final int consumed;
        final Noun noun;

        CueResult(int consumed, Noun noun) {
            this.consumed = consumed;
            this.noun = noun;
        }
    }

    private static class RubResult {
        final int consumed;
        final BigInteger value;

        RubResult(int consumed, BigInteger value) {
            this.consumed = consumed;
            this.value = value;
        }
    }

    private static CueResult cueInner(BigInteger jammed, int pos, Map<Integer, Noun> cache) {
        if (!jammed.testBit(pos)) {
            // 0 prefix: atom
            RubResult rub = rub(jammed, pos + 1);
            Noun noun = new Atom(rub.value);
            cache.put(pos, noun);
            return new CueResult(1 + rub.consumed, noun);
        } else if (!jammed.testBit(pos + 1)) {
            // 10 prefix: cell
            CueResult headRes = cueInner(jammed, pos + 2, cache);
            CueResult tailRes = cueInner(jammed, pos + 2 + headRes.consumed, cache);
            Noun noun = new Cell(headRes.noun, tailRes.noun);
            cache.put(pos, noun);
            return new CueResult(2 + headRes.consumed + tailRes.consumed, noun);
        } else {
            // 11 prefix: back-reference
            RubResult rub = rub(jammed, pos + 2);
            Noun noun = cache.get(rub.value.intValue());
            if (noun == null) {
                throw new IllegalArgumentException("Invalid back-reference at " + pos);
            }
            return new CueResult(2 + rub.consumed, noun);
        }
    }

    private static RubResult rub(BigInteger jammed, int pos) {
        // Count leading zeros
        int zeros = 0;
        while (!jammed.testBit(pos + zeros)) {
            zeros++;
            if (zeros > 64) {
                throw new IllegalArgumentException("Oversized atom length");
            }
        }

        if (zeros == 0) {
            return new RubResult(1, BigInteger.ZERO);
        }

        int lenLen = zeros;
        int bitLen = 1 << (lenLen - 1); // High bit implied

        for (int i = 0; i < lenLen - 1; i++) {
            if (jammed.testBit(pos + lenLen + 1 + i)) {
                bitLen |= (1 << i);
            }
        }

        BigInteger value = BigInteger.ZERO;
        int dataStart = pos + lenLen + lenLen;
        for (int i = 0; i < bitLen; i++) {
            if (jammed.testBit(dataStart + i)) {
                value = value.setBit(i);
            }
        }

        return new RubResult(lenLen + lenLen + bitLen, value);
    }

    // --- Newt framing ---

    public static byte[] newtEncode(BigInteger jammed) {
        byte[] payload = jammedToBytes(jammed);
        byte[] frame = new byte[5 + payload.length];

        frame[0] = 0x00; // version tag

        // Little-endian length
        int len = payload.length;
        frame[1] = (byte) (len & 0xFF);
        frame[2] = (byte) ((len >> 8) & 0xFF);
        frame[3] = (byte) ((len >> 16) & 0xFF);
        frame[4] = (byte) ((len >> 24) & 0xFF);

        System.arraycopy(payload, 0, frame, 5, payload.length);
        return frame;
    }

    public static BigInteger newtDecode(byte[] data) {
        if (data == null || data.length < 5) {
            return null;
        }
        if (data[0] != 0x00) {
            return null; // Invalid version
        }

        int len = (data[1] & 0xFF) |
                  ((data[2] & 0xFF) << 8) |
                  ((data[3] & 0xFF) << 16) |
                  ((data[4] & 0xFF) << 24);

        if (data.length < 5 + len) {
            return null; // Incomplete frame
        }

        byte[] payload = new byte[len];
        System.arraycopy(data, 5, payload, 0, len);
        return bytesToJammed(payload);
    }

    public static byte[] jammedToBytes(BigInteger n) {
        if (n.equals(BigInteger.ZERO)) {
            return new byte[]{0};
        }

        byte[] bytes = n.toByteArray();
        // Remove sign byte if present
        int start = (bytes[0] == 0 && bytes.length > 1) ? 1 : 0;
        // Reverse for little-endian
        byte[] result = new byte[bytes.length - start];
        for (int i = 0; i < result.length; i++) {
            result[i] = bytes[bytes.length - 1 - i];
        }
        return result;
    }

    public static BigInteger bytesToJammed(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return BigInteger.ZERO;
        }
        // Reverse from little-endian
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }
        return new BigInteger(1, reversed);
    }

    // --- Request/response builders ---

    public static Noun buildPeelRequest(long requestId, String command) {
        // [rid %peel [%cmd ~]]
        Atom cmdAtom = Atom.fromCord(command);
        Noun path = new Cell(cmdAtom, Atom.ZERO); // [%cmd ~]
        Atom peel = Atom.fromCord("peel");
        return new Cell(new Atom(requestId), new Cell(peel, path));
    }

    public static Noun buildFyrdKhanEvalRequest(long requestId, String hoon) {
        // [rid %fyrd %base %khan-eval %noun %ted-eval hoon-cord]
        return list(
                new Atom(requestId),
                Atom.fromCord("fyrd"),
                Atom.fromCord("base"),
                Atom.fromCord("khan-eval"),
                Atom.fromCord("noun"),
                Atom.fromCord("ted-eval"),
                Atom.fromCord(hoon)
        );
    }

    private static Noun list(Noun... items) {
        if (items == null || items.length == 0) {
            return Atom.ZERO;
        }

        Noun noun = items[items.length - 1];
        for (int i = items.length - 2; i >= 0; i--) {
            noun = new Cell(items[i], noun);
        }
        return noun;
    }

    public static class PeelResponse {
        public final long requestId;
        public final boolean hasValue;
        public final Noun value;

        PeelResponse(long requestId, boolean hasValue, Noun value) {
            this.requestId = requestId;
            this.hasValue = hasValue;
            this.value = value;
        }
    }

    public static PeelResponse parsePeelResponse(Noun noun) {
        // Response: [rid result]
        // result: [~ value] = some, ~ = none
        if (!noun.isCell()) {
            throw new IllegalArgumentException("Response not a cell");
        }

        Cell cell = (Cell) noun;
        if (!cell.head.isAtom()) {
            throw new IllegalArgumentException("Invalid request id");
        }

        long rid = ((Atom) cell.head).value.longValue();
        Noun result = cell.tail;

        // Check for ~ (null/none)
        if (result.isAtom() && ((Atom) result).value.equals(BigInteger.ZERO)) {
            return new PeelResponse(rid, false, null);
        }

        // Check for [~ value] (some)
        if (result.isCell()) {
            Cell resCell = (Cell) result;
            if (resCell.head.isAtom() && ((Atom) resCell.head).value.equals(BigInteger.ZERO)) {
                return new PeelResponse(rid, true, resCell.tail);
            }
        }

        // Unknown format, return as-is
        return new PeelResponse(rid, true, result);
    }

    public static boolean parseLiveResponse(Noun noun) {
        PeelResponse resp = parsePeelResponse(noun);
        if (!resp.hasValue || resp.value == null) {
            return false;
        }
        // %.y = 0, %.n = 1
        if (resp.value.isAtom()) {
            return ((Atom) resp.value).value.equals(BigInteger.ZERO);
        }
        return false;
    }

    public static BigInteger parseWhoResponse(Noun noun) {
        PeelResponse resp = parsePeelResponse(noun);
        if (!resp.hasValue || resp.value == null) {
            return null;
        }
        if (resp.value.isAtom()) {
            return ((Atom) resp.value).value;
        }
        return null;
    }

    public static String parseVersionResponse(Noun noun) {
        PeelResponse resp = parsePeelResponse(noun);
        if (!resp.hasValue || resp.value == null) {
            return null;
        }
        if (resp.value.isAtom()) {
            return ((Atom) resp.value).toCord();
        }
        return null;
    }

    public static boolean parseFyrdSuccessResponse(Noun noun) {
        // Expected success shape from %khan-eval:
        // [rid %avow %.y %noun %success]
        if (!noun.isCell()) {
            throw new IllegalArgumentException("Fyrd response not a cell");
        }

        Cell top = (Cell) noun;
        if (!top.tail.isCell()) {
            throw new IllegalArgumentException("Fyrd response missing tag");
        }

        Cell tagged = (Cell) top.tail;
        if (!isCord(tagged.head, "avow")) {
            throw new IllegalArgumentException("Fyrd response tag not %avow");
        }

        if (!tagged.tail.isCell()) {
            return false;
        }

        Cell result = (Cell) tagged.tail;
        // %.y is atom 0; %.n is atom 1.
        if (!isAtomValue(result.head, BigInteger.ZERO)) {
            return false;
        }

        if (!result.tail.isCell()) {
            return false;
        }

        Cell page = (Cell) result.tail;
        return isCord(page.head, "noun") && isCord(page.tail, "success");
    }

    private static boolean isCord(Noun noun, String cord) {
        return noun.isAtom() && ((Atom) noun).toCord().equals(cord);
    }

    private static boolean isAtomValue(Noun noun, BigInteger value) {
        return noun.isAtom() && ((Atom) noun).value.equals(value);
    }
}

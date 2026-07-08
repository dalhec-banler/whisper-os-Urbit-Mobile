package io.nativeplanet.controller;

import java.math.BigInteger;

/**
 * Renders an Urbit ship number as its @p phonetic name (scow %p).
 *
 * Ports +fein:ob / +fe:ob from hoon.hoon (see vere jets/e/fein_ob.c for the
 * reference C implementation) and the +po syllable tables. Planets scramble
 * their full 32 bits; moons scramble only the low 32 (the parent part), so a
 * moon's name visibly ends with its parent planet's name. Galaxies, stars,
 * and comets render unscrambled.
 */
final class PatpFormatter {
    private PatpFormatter() {}

    private static final String PREFIXES =
            "dozmarbinwansamlitsighidfidlissogdirwacsabwissib"
            + "rigsoldopmodfoglidhopdardorlorhodfolrintogsilmir"
            + "holpaslacrovlivdalsatlibtabhanticpidtorbolfosdot"
            + "losdilforpilramtirwintadbicdifrocwidbisdasmidlop"
            + "rilnardapmolsanlocnovsitnidtipsicropwitnatpanmin"
            + "ritpodmottamtolsavposnapnopsomfinfonbanmorworsip"
            + "ronnorbotwicsocwatdolmagpicdavbidbaltimtasmallig"
            + "sivtagpadsaldivdactansidfabtarmonranniswolmispal"
            + "lasdismaprabtobrollatlonnodnavfignomnibpagsopral"
            + "bilhaddocridmocpacravripfaltodtiltinhapmicfanpat"
            + "taclabmogsimsonpinlomrictapfirhasbosbatpochactid"
            + "havsaplindibhosdabbitbarracparloddosbortochilmac"
            + "tomdigfilfasmithobharmighinradmashalraglagfadtop"
            + "mophabnilnosmilfopfamdatnoldinhatnacrisfotribhoc"
            + "nimlarfitwalrapsarnalmoslandondanladdovrivbacpol"
            + "laptalpitnambonrostonfodponsovnocsorlavmatmipfip";

    private static final String SUFFIXES =
            "zodnecbudwessevpersutletfulpensytdurwepserwylsun"
            + "rypsyxdyrnuphebpeglupdepdysputlughecryttyvsydnex"
            + "lunmeplutseppesdelsulpedtemledtulmetwenbynhexfeb"
            + "pyldulhetmevruttylwydtepbesdexsefwycburderneppur"
            + "rysrebdennutsubpetrulsynregtydsupsemwynrecmegnet"
            + "secmulnymtevwebsummutnyxrextebfushepbenmuswyxsym"
            + "selrucdecwexsyrwetdylmynmesdetbetbeltuxtugmyrpel"
            + "syptermebsetdutdegtexsurfeltudnuxruxrenwytnubmed"
            + "lytdusnebrumtynseglyxpunresredfunrevrefmectedrus"
            + "bexlebduxrynnumpyxrygryxfeptyrtustyclegnemfermer"
            + "tenlusnussyltecmexpubrymtucfyllepdebbermughuttun"
            + "bylsudpemdevlurdefbusbeprunmelpexdytbyttyplevmyl"
            + "wedducfurfexnulluclennerlexrupnedlecrydlydfenwel"
            + "nydhusrelrudneshesfetdesretdunlernyrsebhulryllud"
            + "remlysfynwerrycsugnysnyllyndyndemluxfedsedbecmun"
            + "lyrtesmudnytbyrsenwegfyrmurtelreptegpecnelnevfes";

    //  +fe:ob constants
    private static final long FE_A = 0xffffL;
    private static final long FE_B = 0x10000L;
    private static final long FE_K = 0xffff0000L;

    //  +raku:ob murmur3 seeds
    private static final long[] RAKU = {0xb76d5eedL, 0xee281300L, 0x85bcae01L, 0x4b387af7L};

    static String format(BigInteger shipId) {
        if (shipId == null || shipId.signum() < 0 || shipId.bitLength() > 128) {
            return null;
        }

        BigInteger scrambled = fein(shipId);

        if (scrambled.compareTo(BigInteger.valueOf(256)) < 0) {
            return "~" + suffix(scrambled.intValue());
        }

        byte[] bytes = scrambled.toByteArray();
        int start = (bytes.length > 1 && bytes[0] == 0) ? 1 : 0;
        int len = bytes.length - start;
        int padded = (len % 2 == 0) ? len : len + 1;

        int[] value = new int[padded];
        for (int i = 0; i < len; i++) {
            value[padded - len + i] = bytes[start + i] & 0xff;
        }

        StringBuilder out = new StringBuilder("~");
        int pairs = padded / 2;
        for (int p = 0; p < pairs; p++) {
            if (p > 0) {
                // scow %p separates 64-bit groups with a double dash (comets)
                boolean groupBreak = ((pairs - p) % 4 == 0);
                out.append(groupBreak ? "--" : "-");
            }
            out.append(prefix(value[p * 2])).append(suffix(value[p * 2 + 1]));
        }
        return out.toString();
    }

    private static String prefix(int i) {
        return PREFIXES.substring(i * 3, i * 3 + 3);
    }

    private static String suffix(int i) {
        return SUFFIXES.substring(i * 3, i * 3 + 3);
    }

    /** +fein:ob — scramble the planet-sized part, pass everything else through. */
    private static BigInteger fein(BigInteger pyn) {
        int bits = pyn.bitLength();
        if (bits <= 16) {
            return pyn;
        }
        if (bits <= 32) {
            return BigInteger.valueOf(feis(pyn.longValueExact()));
        }
        if (bits <= 64) {
            long lo = pyn.and(BigInteger.valueOf(0xffffffffL)).longValueExact();
            BigInteger hi = pyn.shiftRight(32);
            if (lo < FE_B) {
                return pyn;
            }
            return hi.shiftLeft(32).or(BigInteger.valueOf(feis(lo)));
        }
        return pyn;
    }

    /** +feis:ob over [0x1.0000, 0xffff.ffff]. */
    private static long feis(long m) {
        long c = fe(m - FE_B);
        return FE_B + ((c < FE_K) ? c : fe(c));
    }

    /** +fe:ob — 4-round Feistel over the a*b domain. */
    private static long fe(long m) {
        long l = m % FE_A;
        long r = m / FE_A;

        for (int j = 0; j < 4; j++) {
            byte[] key = {(byte) (r & 0xff), (byte) ((r >>> 8) & 0xff)};
            long f = murmur32(key, RAKU[j]);
            long t = (f + l) % ((j & 1) == 0 ? FE_A : FE_B);
            l = r;
            r = t;
        }

        return (r == FE_A) ? (r * FE_A) + l : (l * FE_A) + r;
    }

    /** MurmurHash3 x86 32-bit for a 2-byte key. */
    private static long murmur32(byte[] data, long seed) {
        long c1 = 0xcc9e2d51L;
        long c2 = 0x1b873593L;
        long h = seed & 0xffffffffL;

        // tail (no 4-byte blocks for a 2-byte key)
        long k = ((data[1] & 0xffL) << 8) | (data[0] & 0xffL);
        k = (k * c1) & 0xffffffffL;
        k = ((k << 15) | (k >>> 17)) & 0xffffffffL;
        k = (k * c2) & 0xffffffffL;
        h ^= k;

        h ^= data.length;
        h ^= h >>> 16;
        h = (h * 0x85ebca6bL) & 0xffffffffL;
        h ^= h >>> 13;
        h = (h * 0xc2b2ae35L) & 0xffffffffL;
        h ^= h >>> 16;
        return h;
    }
}

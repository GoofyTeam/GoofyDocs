package com.goofy.GoofyDocs.chunking;

import org.rabinfingerprint.fingerprint.RabinFingerprintLong;
import org.rabinfingerprint.polynomial.Polynomial;

public class RabinKarp {
    private static final int WINDOW_SIZE = 48;
    private static final int MASK = (1 << 13) - 1;

    private final RabinFingerprintLong fingerprint;
    private int windowPos;

    public RabinKarp() {
        this.windowPos = 0;
        Polynomial polynomial = Polynomial.createFromLong(9223372036854775783L);
        this.fingerprint = new RabinFingerprintLong(polynomial);
    }

    public void reset() {
        this.windowPos = 0;
        this.fingerprint.reset();
    }

    public boolean pushByte(byte b) {
        fingerprint.pushByte(b);
        windowPos++;

        return windowPos >= WINDOW_SIZE && (fingerprint.getFingerprintLong() & MASK) == 0;
    }
}

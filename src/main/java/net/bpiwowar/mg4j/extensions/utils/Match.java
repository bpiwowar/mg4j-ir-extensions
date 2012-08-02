package net.bpiwowar.mg4j.extensions.utils;

import java.io.UnsupportedEncodingException;

/**
 * Useful to match a series of bytes
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Match {
    final byte[] bytes;
    final byte[] alt;

    int index = 0;

    public Match(byte[] bytes, byte [] alt) {
        this.bytes = bytes;
        this.alt = alt;
    }

    static public Match create(String string) {
        return create(string, false);
    }

    static public Match create(String string, boolean noCase) {
        try {
            if (!noCase)
                return new Match(string.getBytes("ASCII"), null);

            return new Match(string.toLowerCase().getBytes("ASCII"), string.toUpperCase().getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Byte-by-byte check whether the whole string-to-match was matched.
     * If only a part of  the string to match was matched so far, it just
     * updates the pointer and returns false. If all characters are matched,
     * it returns {@code true}. If we have a character mismatch, it returns
     * {@code false} and reset the matching pointer.
     *
     * @param b the next input byte to be checked
     * @return {@code true} if the whole string was matched, {@code false}
     *         if there's a character mismatch or just a part of the string
     *         was matched so far
     */
    public boolean match(byte b) {
        if (b == bytes[index] || (alt != null && b == alt[index])) {
            index++;
            if (index == bytes.length) {
                index = 0;
                return true;
            }
        } else
            index = 0;

        return false;
    }

    /**
     * Resets the internal matching pointer
     */
    public void reset() {
        index = 0;
    }

    /**
     * Returns the size of the string to match
     *
     * @return the size
     */
    public int size() {
        return bytes.length;
    }
}

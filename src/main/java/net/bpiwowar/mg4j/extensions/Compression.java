package net.bpiwowar.mg4j.extensions;

import bpiwowar.argparser.EnumValue;

/**
 * Sets the compression
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 20/6/12
*/
public enum Compression {
    @EnumValue("none")
    NONE,

    @EnumValue("gzip")
    GZIP,

    @EnumValue("bgzip")
    BLOCK_GZIP,;

    /**
     * Get the enum from a string
     */
    public static Compression fromString(String string) {
        if (string == null || string.length()==0) return NONE;

        for(Compression s: Compression.values()) {
            try {
                final EnumValue values =
                        Compression.class.getField(s.name()).getAnnotation(EnumValue.class);
                for(String value: values.value()) {
                    if (value.equals(string))
                        return s;
                }
            } catch (NoSuchFieldException e) {
                throw new AssertionError("Should not happen");
            }
        }

        return Compression.valueOf(string);
    }
}

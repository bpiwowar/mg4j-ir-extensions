package net.bpiwowar.mg4j.extensions.utils;

import it.unimi.di.big.mg4j.index.DiskBasedIndex;
import it.unimi.di.big.mg4j.index.FileIndex;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.io.InputBitStream;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.IdentityHashMap;

import static java.lang.String.format;

/**
 * Utility methods to access MG4J indexes
 */
public class IndexUtils {
    /**
     * Cache for term frequencies
     */
    static private final IdentityHashMap<Index, SoftReference<LongBigArrayBigList>> TERM_FREQUENCIES = new IdentityHashMap<>();

    static private File getFile(String basename, CharSequence extension) {
        return new File(format("%s%s", basename, extension));
    }

    /**
     * Returns the total number of occurrences of each term of an index.
     *
     * Uses a cache to store the frequencies
     *
     * @param index The index for which those statistics should be returned
     * @return A list of term frequencies
     * @throws IOException If something wrong occurs while reading the index
     */
    static public LongBigList getTermFrequency(Index index) throws IOException {
        final SoftReference<LongBigArrayBigList> listReference = TERM_FREQUENCIES.get(index);
        LongBigArrayBigList list = listReference == null ? null : listReference.get();

        if (list == null) {
            if (index instanceof FileIndex) {
                final String basename = ((FileIndex) index).basename;
                File frequenciesFile = getFile(basename, DiskBasedIndex.COUNTS_EXTENSION);
                list = new LongBigArrayBigList(index.numberOfTerms);

                final InputBitStream in = new InputBitStream(frequenciesFile);
                for (long i = 0; i < list.size64(); i++)
                    list.set(i, in.readLongGamma());
                in.close();
                TERM_FREQUENCIES.put(index, new SoftReference<>(list));
            } else throw new AssertionError("Cannot handle index of class " + index.getClass());
        }

        return list;

    }

}

/**
 *
 */
package net.bpiwowar.mg4j.extensions.conf;

import it.unimi.di.big.mg4j.index.DiskBasedIndex;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.IndexReader;
import it.unimi.di.big.mg4j.index.QuasiSuccinctIndex;
import it.unimi.di.big.mg4j.io.IOFactory;
import it.unimi.dsi.fastutil.ints.IntBigList;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import it.unimi.dsi.io.InputBitStream;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import static java.lang.String.format;

final public class IndexedField {
    final static private Logger LOGGER = LoggerFactory.getLogger(IndexedField.class);
    private final File basedir;
    private final String basename;
    public final Index index;

    private long unknownTermId;
    public String field;

    public IndexedField(File basepath, String field) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, InstantiationException, URISyntaxException, ConfigurationException, ClassNotFoundException {
        this.field = field;
        basedir = basepath.getParentFile();
        basename = format("%s-%s", basepath.getName(), field);
        index = Index.getInstance(getFile("").getAbsolutePath(), true, true);
    }


    /**
     * Returns the ID of an unknown term
     */
    public long getUnknownTermId() {
        checkTermMap(false);
        return unknownTermId;
    }
    it.unimi.dsi.big.util.StringMap<? extends CharSequence> termMap;
    private ObjectBigList<? extends CharSequence> list;

    /**
     * Return the total length of the documents
     *
     * @return
     */
    public long getNumberOfPostings() {
        return index.numberOfPostings;
    }

    /**
     * Return the size of a document
     */
    public int getSize(long docId) {
        if (index.sizes == null)
            return -1;

        return index.sizes.get(docId);
    }

    /**
     * Get a term id for a given word
     *
     * @param word
     * @return
     */
    public long getTermId(CharSequence word) {
        checkTermMap(false);

        return termMap.getLong(word);
    }


    private void checkTermMap(boolean getList) {
        if (termMap == null) {
            termMap = index.termMap;
            unknownTermId = termMap.defaultReturnValue();
        }

        if (getList && list == null)
            list = termMap.list();
    }

    public ObjectBigList<? extends CharSequence> getTerms() {
        checkTermMap(true);
        return list;
    }

    /**
     * Get term
     *
     * @param i The term id
     * @return The string corresponding to this term
     */
    public CharSequence getTerm(long i) {
        checkTermMap(true);
        return list.get(i);
    }

    /**
     * Weak reference to document frequencies
     */
    SoftReference<IntBigList> frequencies = new SoftReference<IntBigList>(null);

    /**
     * Get document frequencies (i.e., number of documents in which a term
     * appears)
     *
     * @return
     * @throws java.io.IOException
     */
    public IntBigList getFrequencies() throws IOException {
        IntBigList list = frequencies.get();
        if (list == null) {
            File frequenciesFile = getFile(DiskBasedIndex.FREQUENCIES_EXTENSION);
            LOGGER.info("Loading term frequencies from file " +
                    frequenciesFile);
            list = DiskBasedIndex.readSizes(frequenciesFile.toString(),
                    index.numberOfTerms);
            frequencies = new SoftReference<>(list);
        }
        return list;

    }

    /**
     * Weak reference to document frequencies
     */
    SoftReference<LongBigList> termfrequencies = new SoftReference<>(null);

    /**
     * Get term frequencies (i.e. the number of times a term occurs in the
     * whole index).
     *
     * @return the term frequencies as an array which is parallel to the term
     * ids
     * @throws java.io.IOException
     */
    public LongBigList getTermFrequency() throws IOException {
        LongBigList list = termfrequencies.get();

        if (list == null) {
            if (index.hasCounts) {

                final String filename = getFile(DiskBasedIndex.COUNTS_EXTENSION + DiskBasedIndex.OFFSETS_POSTFIX)
                        .getAbsolutePath();
                LOGGER.info("Loading term frequencies from file " +
                        filename);
                list = DiskBasedIndex.offsets(IOFactory.FILESYSTEM_FACTORY,
                        filename,
                        index.numberOfTerms, 0 );
//                list = new LongBigArrayBigList(index.numberOfTerms);
//
//                final InputBitStream in = new InputBitStream(frequenciesFile);
//                for (long i = 0; i < index.numberOfTerms; i++)
//                    list.add(i, in.readLongGamma());
//                in.close();
                termfrequencies = new SoftReference<>(list);

                LOGGER.info("Completed.");
                throw new AssertionError("Does not work anymore...  to be fixed!");
            }

        }
        return list;

    }

    private File getFile(CharSequence extension) {
        return new File(basedir, format("%s%s", basename, extension));
    }

    public IndexReader getReader() throws IOException {
        return index.getReader();
    }
}
/**
 * 
 */
package net.bpiwowar.mg4j.extensions.conf;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker.ValidDirectory;
import it.unimi.di.big.mg4j.index.DiskBasedIndex;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.dsi.fastutil.ints.IntBigList;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import it.unimi.dsi.io.InputBitStream;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;

final public class IndexConfiguration {
	final static private Logger LOGGER = Logger.getLogger(IndexConfiguration.class);

	@Argument(name = "dir", help = "Index directory", checkers = ValidDirectory.class, required = true)
	public File directory;
	@Argument(name = "basename", help = "Index basename")
	public String basename = "index";
	@Argument(name = "field", help = "Field to use (by default \"text\")")
	public String field = "text";

	transient public Index index;

    private long unknownTermId;

    public IndexConfiguration() {
	}

    /** Returns the ID of an unknown term */
    public long getUnknownTermId() {
        checkTermMap(false);
        return unknownTermId;
    }

    /**
	 * 
	 * @param directory
	 *            index directory
	 * @param basename
	 *            index basename
	 * @param field
	 *            index field (e.g., "text")
	 * @throws Exception
	 */
	public IndexConfiguration(File directory, String basename, String field)
			throws Exception {
		this.directory = directory;
		this.basename = basename;
		this.field = field;
		init();
	}

	/**
	 * Initialise the index
	 */
	public Index init() throws Exception {
        if (index != null)
            return index;

		return index = Index.getInstance(
				new File(directory, String.format("%s-%s", basename, field))
						.toString(), true, true);
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
	public double getSize(int docId) {
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
	 * @param i
	 * @return
	 */
	public CharSequence getTerm(long i) {
		checkTermMap(true);
		return list.get(i);
	}

	/** Weak reference to document frequencies */
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
			File frequenciesFile = new File(directory, String.format("%s-%s%s",
					basename, field, DiskBasedIndex.FREQUENCIES_EXTENSION));
			LOGGER.info("Loading term frequencies from file "+
					frequenciesFile);
			list = DiskBasedIndex.readSizes(frequenciesFile.toString(),
					index.numberOfTerms);
			frequencies = new SoftReference<>(list);
		}
		return list;

	}

	/** Weak reference to document frequencies */
	SoftReference<LongBigList> termfrequencies = new SoftReference<>(null);
	
	/**
	 * Get term frequencies (i.e. the number of times a term occurs in the 
	 * whole index).
	 * 
	 * @return the term frequencies as an array which is parallel to the term
	 * 		ids
	 * @throws java.io.IOException
	 */
	public LongBigList getTermFrequency() throws IOException {
        LongBigList list = termfrequencies.get();
		
		if (list == null) {
			File frequenciesFile = new File(directory, String.format("%s-%s%s",
					basename, field, DiskBasedIndex.COUNTS_EXTENSION));
			LOGGER.info("Loading term frequencies from file " +
					frequenciesFile);
			list = new LongBigArrayBigList(index.numberOfTerms);

			final InputBitStream in = new InputBitStream(frequenciesFile);
			for (long i = 0; i < list.size64(); i++)
				list.set(i, in.readLongGamma());
			in.close();
			termfrequencies = new SoftReference<>(list);
			
			LOGGER.info("Completed.");
		}
		return list;

	}
}
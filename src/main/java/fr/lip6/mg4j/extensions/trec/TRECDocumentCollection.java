package fr.lip6.mg4j.extensions.trec;

/*		 
 * MG4J: Managing Gigabytes for Java
 *
 * Copyright (C) 2006-2007 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

import bpiwowar.argparser.*;
import fr.lip6.mg4j.extensions.Metadata;
import it.unimi.di.mg4j.document.*;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.io.SegmentedInputStream;
import it.unimi.dsi.logging.ProgressLogger;
import net.sf.samtools.util.BlockCompressedInputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * A collection for the TREC data set.
 * 
 * <p>
 * The documents are stored as a set of descriptors, representing the (possibly
 * gzipped) file they are contained in and the start and stop position in that
 * file. To manage descriptors later we rely on {@link SegmentedInputStream}.
 * 
 * <p>
 * To interpret a file, we read up to <samp>&lt;DOC&gt;</samp> and place a start
 * marker there, we advance to the header and store the URI. An intermediate
 * marker is placed at the end of the doc header tag and a stop marker just
 * before <samp>&lt;/DOC&gt;</samp>.
 * 
 * <p>
 * The collection provides both sequential access to all documents via the
 * iterator and random access to a given document. However, the two operations
 * are performed very differently as the sequential operation is much more
 * performant than calling {@link #document(int)} repeatedly.
 * 
 * @author Alessio Orlandi
 * @author Luca Natali
 * @author Benjamin Piwowarski
 */
public class TRECDocumentCollection extends AbstractDocumentCollection
		implements Serializable {

    static final private Logger LOGGER = Logger.getLogger(TRECDocumentCollection.class);

	/**
	 * Useful to match a series of bytes
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static public class Match {
		final byte[] bytes;
		int index = 0;

		Match(byte[] bytes) {
			this.bytes = bytes;
        }

        static public Match create(String string) {
            try {
                return new Match(string.getBytes("ASCII"));
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
		 * 		if there's a character mismatch or just a part of the string
		 * 		was matched so far
		 */
		boolean match(byte b) {
			if (b == bytes[index]) {
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
		void reset() {
			index = 0;
		}

		/**
		 * Returns the size of the string-to-match
		 * @return the size
		 */
		public int size() {
			return bytes.length;
		}
	}

	static public enum Compression {
		@EnumValue("uncompressed")
		NONE,

		@EnumValue("gzip")
		GZIP,

		@EnumValue("bgzip")
		BGZIP,
	}

	static public class Options {
		@Argument(name = "collection", required = true, help = "The filename for the serialised collection")
		String collection;

		@Argument(name = "unsorted", help = "Keep the file list unsorted (otherwise, files are sorted before the sequence is built - useful to move the built index on another computer)")
		boolean unsorted = false;

		@Argument(name = "compression", help = "File compression")
		Compression compression = Compression.NONE;

		@Argument(name = "bufferSize", help = "The size of an I/O buffer in bytes (default 64000)")
		int bufferSize = DEFAULT_BUFFER_SIZE;

		@Argument(name = "property", help = "A property (name=value)")
		ArrayList<String> properties = new ArrayList<String>();
	}

	/**
	 * A compact description of the location and of the internal segmentation of
	 * a TREC document inside a file.
	 */

	protected static class TRECDocumentDescriptor implements Cloneable {
		/** A reference to the file containing this document. */
		public int fileIndex;
		/** The starting position of this document in the file. */
		public long startMarker;
		/** The ending position. */
		public int stopMarkerDiff;
		/** The document ID */
		public String docid;

		public TRECDocumentDescriptor(String docid, int findex, long start,
				int stopMarkerDiff) {
			this.docid = docid;
			this.fileIndex = findex;
			this.startMarker = start;
			this.stopMarkerDiff = stopMarkerDiff;
		}

		public TRECDocumentDescriptor(String docid, int findex, long start,
				long stop) {
			this(docid, findex, start, (int) (stop - start));

		}

		@Override
		public Object clone() {
			return new TRECDocumentDescriptor(docid, fileIndex, startMarker,
					stopMarkerDiff);
		}

		public final long[] toSegments() {
			return new long[] { startMarker, stopMarkerDiff + startMarker };

		}

	}

	private static final long serialVersionUID = 1;

	transient private static final boolean DEBUG = false;
	/** Default buffer size, set up after some experiments. */
	transient public static final int DEFAULT_BUFFER_SIZE = 64000;

	protected static boolean equals(byte[] a, int len, byte[] b) {
		if (len != b.length)
			return false;
		while (len-- != 0)
			if (a[len] != b[len])
				return false;
		return true;
	}

	public static void main(String[] arg) throws IOException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
            InvalidHolderException, ConfigurationException,
            ArgParserException {

		Options options = new Options();
		ArgParser argParser = new ArgParser("TRECDocumentCollection");
		argParser.addOptions(options);
		String[] file = argParser.matchAllArgs(arg, 0,
				ArgParserOption.STOP_FIRST_UNMATCHED);

		run(options, file);
	}

	/**
	 * Parses the document collection and finally stores the 
	 * TRECDocumentCollection in a file
	 * @param options the set of options
	 * @param file the list of document files to parse
	 * @throws java.io.IOException
	 */
	public static void run(Options options, String[] file) throws IOException,
            org.apache.commons.configuration.ConfigurationException {

		if (file.length == 0) {
			final ObjectArrayList<String> files = new ObjectArrayList<String>();
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(System.in));
			String s;
			while ((s = bufferedReader.readLine()) != null)
				files.add(s);
			file = files.toArray(new String[0]);
		}

		// To avoid problems with find and similar utilities, we sort the file
		// names
		if (!options.unsorted)
			Arrays.sort(file);

		final DocumentFactory documentFactory = new TRECDocumentFactory(
				options.properties
						.toArray(new String[options.properties.size()]));

		if (file.length == 0)
			System.err.println("WARNING: empty file set.");
		TRECDocumentCollection coll = new TRECDocumentCollection(file,
				documentFactory, options.bufferSize, options.compression);
		BinIO.storeObject(coll, options.collection);

	}

	/** The list of the files containing the documents. */
	private String[] file;

	/** Whether the files in {@link #file} are gzipped. */
	private Compression compression = Compression.NONE;

	/** The document factory. */
	protected DocumentFactory factory;

	/**
	 * The list of document descriptors. We assume that descriptors within the
	 * same file are contiguous - descriptors are saved separately, that's why
	 * they are transient
	 */
	protected transient ObjectArrayList<TRECDocumentDescriptor> descriptors;

	/** The buffer size. */
	private final int bufferSize;

	/** The last returned stream. */
	transient private SegmentedInputStream lastStream;

	transient byte buffer[] = new byte[8 * 1024];

	transient byte docnoBuffer[] = new byte[1024];

	transient static final Match DOC_OPEN = Match.create("<DOC>"),
			DOC_CLOSE = Match.create("</DOC>"), DOCNO_OPEN = Match.create("<DOCNO>"),
			DOCNO_CLOSE = Match.create("</DOCNO>");

	/**
	 * Creates a new TREC collection by parsing the given files.
	 * 
	 * @param file
	 *            an array of file names containing documents in TREC GOV2
	 *            format.
	 * @param factory
	 *            the document factory (usually, a composite one).
	 * @param bufferSize
	 *            the buffer size.
	 * @param compression
	 *            true if the files are gzipped.
	 */
	public TRECDocumentCollection(String[] file, DocumentFactory factory,
			int bufferSize, Compression compression) throws IOException {
		this.file = file;
		this.factory = factory;
		this.bufferSize = bufferSize;
		this.descriptors = new ObjectArrayList<TRECDocumentDescriptor>();
		this.compression = compression;

		final ProgressLogger progressLogger = new ProgressLogger(LOGGER);
		progressLogger.expectedUpdates = file.length;
		progressLogger.itemsName = "files";

		progressLogger.start("Parsing files with compression \"" + compression
				+ "\"");

		for (int i = 0; i < file.length; i++) {
			parseContent(i, openFileStream(file[i]));
			progressLogger.update();
		}

		progressLogger.done();
	}

	/**
	 * Copy constructor (that is, the one used by {@link #copy()}. Just
	 * initializes final fields
	 */
	protected TRECDocumentCollection(String[] file, DocumentFactory factory,
			ObjectArrayList<TRECDocumentDescriptor> descriptors,
			int bufferSize, Compression compression) {
		this.compression = compression;
		this.file = file;
		this.bufferSize = bufferSize;
		this.factory = factory;
		this.descriptors = descriptors;
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (lastStream != null)
			lastStream.close();
		descriptors = null;
	}

	@Override
	public TRECDocumentCollection copy() {
		return new TRECDocumentCollection(file, factory.copy(), descriptors,
				bufferSize, compression);
	}

	/**
	 * Returns a document
	 * @param n number of the document to return
	 * @return the document 
	 */
	@Override
	public Document document(int n) throws IOException {
		Reference2ObjectMap<Enum<?>, Object> metadata = metadata(n);
		return factory.getDocument(stream(n), metadata);
	}

	/**
	 * Returns the underlying document factory
	 * @return the document factory
	 */
	@Override
	public DocumentFactory factory() {
		return this.factory;
	}

	/**
	 * Returns the iterator over the documents. Use this method if you want
	 * sequential access to the documents.
	 * @return the document iterator
	 * @throws java.io.IOException
	 */
	@Override
	public DocumentIterator iterator() throws IOException {
		return new AbstractDocumentIterator() {
			/**
			 * An iterator returning the descriptors of the documents in the
			 * enveloping collection.
			 */
			private final ObjectIterator<TRECDocumentDescriptor> descriptorIterator = descriptors
					.iterator();
			/** The current stream. */
			private SegmentedInputStream siStream;
			/** The current document. */
			private int currentDocument = 0;
			/** The last returned document. */
			private Document last;
			/**
			 * The first descriptor of the next file, if any, or
			 * <code>null</code> if nextFile() has never been called.
			 */
			private TRECDocumentDescriptor firstNextDescriptor;

			@Override
			public void close() throws IOException {
				if (siStream != null) {
					if (last != null)
						last.close();
					super.close();
					siStream.close();
					siStream = null;
				}
			}

			@Override
			public Document nextDocument() throws IOException {
				/*
				 * If necessary, skip to the next segment, else, try skipping to
				 * the next file.
				 */
				if (DEBUG)
					LOGGER.debug("nextDocument() has been called ");

				if (last != null) {
					last.close();
					if (!siStream.hasMoreBlocks()) {
						if (!nextFile())
							return last = null;
					} else
						siStream.nextBlock();
				} else if (!nextFile())
					return null; // First call

				return last = factory.getDocument(siStream,
						metadata(currentDocument++));
			}

			private boolean nextFile() throws FileNotFoundException,
					IOException {
				if (size() == 0)
					return false;
				if (siStream != null)
					siStream.close();
				if (!descriptorIterator.hasNext())
					return false;

				/*
				 * We assume documents contained in the same gzip file are
				 * contigous so we collect all of them until we find a different
				 * file index.
				 */
				TRECDocumentDescriptor currentDescriptor = firstNextDescriptor != null ? firstNextDescriptor
						: descriptorIterator.next();
				int currentFileIndex = currentDescriptor.fileIndex;

				if (DEBUG)
					LOGGER.debug("Skipping to contents file "
							+ currentFileIndex + " (" + file[currentFileIndex]
							+ ")");

				/*
				 * We create the segmented input stream with all just collected
				 * descriptors
				 */
				siStream = new SegmentedInputStream(
						openFileStream(file[currentFileIndex]));

				do {
					siStream.addBlock(currentDescriptor.toSegments());
					if (!descriptorIterator.hasNext())
						break;
					currentDescriptor = descriptorIterator.next();
				} while (currentDescriptor.fileIndex == currentFileIndex);

				firstNextDescriptor = currentDescriptor; // The last assignment
				// will be
				// meaningless, but
				// it won't be used
				// anyway
				return true;
			}
		};
	}

	/**
	 * Merges a new collection in this one, by rebuilding the gzFile array and
	 * appending the other object one, concatenating the descriptors while
	 * rebuilding all.
	 * <p>
	 * It is supposed that the passed object contains no duplicates for the
	 * local collection.
	 */
	public void merge(TRECDocumentCollection other) {
		int oldLength = this.file.length;

		this.file = ObjectArrays.ensureCapacity(this.file, this.file.length
				+ other.file.length);
		System
				.arraycopy(other.file, 0, this.file, oldLength,
						other.file.length);

		ObjectIterator<TRECDocumentDescriptor> iter = other.descriptors
				.iterator();
		while (iter.hasNext()) {
			final TRECDocumentDescriptor tdd = (TRECDocumentDescriptor) iter
					.next().clone();
			tdd.fileIndex += oldLength;
			this.descriptors.add(tdd);
		}
	}

	@Override
	public Reference2ObjectMap<Enum<?>, Object> metadata(final int index) {
		ensureDocumentIndex(index);
		final Reference2ObjectArrayMap<Enum<?>, Object> metadata = new Reference2ObjectArrayMap<Enum<?>, Object>(
				4);

		TRECDocumentDescriptor trecDocumentDescriptor = descriptors.get(index);
		metadata.put(PropertyBasedDocumentFactory.MetadataKeys.URI, "Document #" + index);
		metadata.put(Metadata.DOCID, trecDocumentDescriptor.docid);
		return metadata;
	}

	/**
	 * Opens the file stream, supporting certain kinds of compression
	 * @param fileName the file name
	 * @return the file stream
	 * @throws java.io.IOException if something went wrong or compression is not
	 * 			supported
	 */
	private final InputStream openFileStream(String fileName)
			throws IOException {
		if (compression == null) compression = Compression.NONE;

		switch (compression) {
		case BGZIP:
			return new BlockCompressedInputStream(new File(fileName));
		case GZIP:
			return new GZIPInputStream(new FileInputStream(fileName));
		case NONE:
			return new FileInputStream(fileName);
		}

		throw new AssertionError(
				"Missing case in the switch for handling compression in files");
	}

	/**
	 * Process one of the file in order to find the blocks. This identifies 
	 * for each document its exact position and length in the set of files (see
	 * also {@link fr.lip6.mg4j.extensions.trec.TRECDocumentCollection.TRECDocumentDescriptor} in this class).
	 * 
	 * @param fileIndex
	 *            The index in the file array
	 * @param is
	 *            The input stream for this file
	 * @throws java.io.IOException
	 */
	protected void parseContent(int fileIndex, InputStream is) throws IOException {
		
		long currStart = 0, currStop = 0, position;

		// Are we within a document
		boolean startedBlock = false;
		boolean startedDocNo = false;

		LOGGER.debug("Processing file %d (%s)", fileIndex, file[fileIndex]);

		FastBufferedInputStream fbis = new FastBufferedInputStream(is,
				bufferSize);

		// Process the document
		int read;
		DOC_OPEN.reset();
		position = 0;
		int docnoCount = 0;

		final boolean debugEnabled = LOGGER.isDebugEnabled();
		while ((read = fbis.read(buffer, 0, buffer.length)) > 0) {
			for (int offset = 0; offset < read; offset++) {
				final byte b = buffer[offset];

				if (!startedBlock) {
					if (DOC_OPEN.match(b)) {
						// we matched <DOC>, now getting ready for
						// <DOCNO> and </DOC>. A new document block starts
						startedBlock = true;
						currStart = position + offset + 1 - DOC_OPEN.size();
						DOC_CLOSE.reset();
						DOCNO_OPEN.reset();
					}
				} else {
					if (startedDocNo) {
						// read and store document number byte
						docnoBuffer = ByteArrays.grow(docnoBuffer,
								docnoCount + 1);
						docnoBuffer[docnoCount++] = b;

						// check if we already read </DOCNO>
						if (DOCNO_CLOSE.match(b)) {
							startedDocNo = false;
							docnoCount -= DOCNO_CLOSE.size(); // cut </DOCNO>
						}
					} else if (DOCNO_OPEN.match(b)) {
						// we matched <DOCNO>. Prepare to read its content
						startedDocNo = true;
						docnoCount = 0;
						DOCNO_CLOSE.reset();
					}

					// Handle separately the </DOC> tag
					if (DOC_CLOSE.match(b)) {
						// matched </DOCNO>
						startedDocNo = startedBlock = false;
						currStop = position + offset + 1;
						DOC_OPEN.reset();
						
						// get the document number
						final String docno = new String(docnoBuffer, 0,
								docnoCount, "ASCII").trim();
						
						// Store document pointers in a document descriptor,
						// add to descriptors. This stores all required
						// information to identify the document among the set
						// of input files.
						if (debugEnabled)
							LOGGER.debug("Setting markers {%s, %d, %d}", docno,
									currStart, currStop);
						descriptors.add(new TRECDocumentDescriptor(docno,
								fileIndex, currStart, currStop));
						startedBlock = false;

					}
				}
			}

			// Update the number of read bytes
			position += read;
		}

		fbis.close();
	}

	/**
	 * Deserialization
	 * 
	 * @param s
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(final ObjectInputStream s) throws IOException,
			ClassNotFoundException {
		s.defaultReadObject();

		final int size = s.readInt();
		final ObjectArrayList<TRECDocumentDescriptor> descriptors = new ObjectArrayList<TRECDocumentDescriptor>();
		descriptors.ensureCapacity(size);
		for (int i = 0; i < size; i++)
			descriptors.add(new TRECDocumentDescriptor(s.readUTF(),
					s.readInt(), s.readLong(), s.readInt()));
		this.descriptors = descriptors;
	}

	@Override
	public int size() {
		return descriptors.size();
	}

	@Override
	public InputStream stream(final int n) throws IOException {
		// Creates a Segmented Input Stream with only one segment in (the
		// requested one).
		ensureDocumentIndex(n);
		if (lastStream != null)
			lastStream.close();
		final TRECDocumentDescriptor descr = descriptors.get(n);
		return lastStream = new SegmentedInputStream(
				openFileStream(file[descr.fileIndex]), descr.toSegments());
	}

	/**
	 * Write the object to disk
     *
	 * @param s The serialisation step
	 * @throws java.io.IOException
	 */
	private void writeObject(final ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(descriptors.size());

		for (TRECDocumentDescriptor descriptor : descriptors) {
			s.writeUTF(descriptor.docid);
			s.writeInt(descriptor.fileIndex);
			s.writeLong(descriptor.startMarker);
			s.writeInt(descriptor.stopMarkerDiff);
		}
	}
}

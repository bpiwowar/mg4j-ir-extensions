/**
 * $Author:$
 * $Id:$
 * $Rev:$
 */

package fr.lip6.mg4j.extensions.warc;

import fr.lip6.mg4j.extensions.trec.TRECDocumentCollection;
import fr.lip6.mg4j.extensions.trec.TRECDocumentFactory;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.document.DocumentFactory;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.configuration.ConfigurationException;

import uk.ac.gla.dcs.renaissance.mg4j.BuildWARCFileSequence;
import uk.ac.gla.dcs.renaissance.mg4j.trec.TRECDocumentFactory.CollectionType;
import uk.ac.gla.dcs.renaissance.util.WarcHTMLResponseRecord;
import uk.ac.gla.dcs.renaissance.util.WarcRecord;
import bpiwowar.log.Logger;

/**
 * Managing TREC collections provided in a WARC format, as used for instance
 * by the TREC session track. A document collection basically consists of a set
 * of descriptors pointing to important locations in the (possibly zipped) 
 * document archive. This is called a <em>sequence</em>.
 * @author <a href="mailto:ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 * @see BuildWARCFileSequence
 * @see fr.lip6.mg4j.extensions.trec.TRECDocumentCollection
 * @see DocumentFactory
 */
public class WARCDocumentCollection extends TRECDocumentCollection {

	private static final long serialVersionUID = 1;
	private static final Logger LOGGER = Logger.getLogger();
	final boolean debugEnabled = LOGGER.isDebugEnabled();

	/**
	 * Creates a new TREC WARC collection by parsing the given files.
	 * 
	 * @param file
	 *            an array of file names containing documents in TREC WARC
	 *            format.
	 * @param factory
	 *            the document factory (usually, a composite one).
	 * @param bufferSize
	 *            the buffer size.
	 * @param compression
	 *            true if the files are gzipped.
	 */
	public WARCDocumentCollection(String[] file, DocumentFactory factory,
			int bufferSize, Compression compression) throws IOException {
		super(file, factory, bufferSize, compression);
	}

	/**
	 * Copy constructor (that is, the one used by {@link #copy()}. Just
	 * initializes final fields
	 */
	public WARCDocumentCollection(String[] file, DocumentFactory factory,
			ObjectArrayList<TRECDocumentDescriptor> descriptors,
			int bufferSize, Compression compression) {
		super(file, factory, descriptors, bufferSize, compression);
	}
	

	
	
	@Override
	protected void parseContent(int fileIndex, InputStream is)
			throws IOException {
		WarcRecord.newFile();
		WarcRecord warcRecord = null;
		DataInputStream dis = new DataInputStream(is);
		boolean oldReadContentFlag =
				WarcRecord.readContent(false); // don't read content
		while ((warcRecord = WarcRecord.readNextWarcRecord(dis)) != null) {
			// ignore if no WARC response type
			if (warcRecord.getHeaderRecordType().equals("response")) {
				WarcHTMLResponseRecord warcResponse = 
					new WarcHTMLResponseRecord(warcRecord);
				String docno = warcResponse.getTargetTrecID();
				long currStart = warcResponse.getStartMarker();
				long currStop = warcResponse.getStopMarker();
				if (debugEnabled)
					LOGGER.debug("Setting markers {%s, %d, %d}", docno,
							currStart, currStop);
				descriptors.add(new TRECDocumentDescriptor(docno,
						fileIndex, currStart, currStop));
				LOGGER.debug("Descriptor size is " + descriptors.size());
			}
		}
		dis.close();
		WarcRecord.readContent(oldReadContentFlag); // reset readContent flag
	}
	
	

	/**
	 * Parses the document collection and finally stores the created
	 * WARCDocumentCollection in a file
	 * @param options the set of options
	 * @param file the list of document files to parse. If the list is 
	 * 		empty, the files are read from STDIN
	 * @throws java.io.IOException
	 * @throws ConfigurationException
	 */
	public static void run(Options options, String[] file) throws IOException,
			ConfigurationException {

		// If we don't have files given, we read a list of file names from STDIN
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

		final TRECDocumentFactory documentFactory = new TRECDocumentFactory(
				options.properties
						.toArray(new String[options.properties.size()]));
		documentFactory.setCollectionType(CollectionType.WARC);

		if (file.length == 0)
			System.err.println("WARNING: empty file set.");
		WARCDocumentCollection coll = new WARCDocumentCollection(file,
				documentFactory, options.bufferSize, options.compression);
		BinIO.storeObject(coll, options.collection);

	}


	

}

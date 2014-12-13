/**
 * $Author:$
 * $Id:$
 * $Rev:$
 */

package net.bpiwowar.mg4j.extensions.warc;

import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentDescriptor;
import net.bpiwowar.mg4j.extensions.utils.HTMLDocumentFactory;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Managing TREC collections provided in a WARC format, as used for instance
 * by the TREC session track. A document collection basically consists of a set
 * of descriptors pointing to important locations in the (possibly zipped)
 * document archive. This is called a <em>sequence</em>.
 *
 * @author <a href="mailto:ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 * @author <a href="mailto:benjamin@bpiwowar.net">Benjamin Piwowarski</a>
 * @see net.bpiwowar.mg4j.extensions.trec.TRECDocumentCollection
 * @see DocumentFactory
 */
public class WARCDocumentCollection extends SegmentedDocumentCollection {

    private static final long serialVersionUID = 1;
    private static final Logger LOGGER = Logger.getLogger(WARCDocumentCollection.class);
    final boolean debugEnabled = LOGGER.isDebugEnabled();


    /**
     * Creates a new TREC WARC collection by parsing the given files.
     *
     * @param file        an array of file names containing documents in TREC WARC
     *                    format.
     * @param bufferSize  the buffer size.
     * @param compression true if the files are gzipped.
     */
    public WARCDocumentCollection(String[] file,
                                  int bufferSize, Compression compression, File metadataFile) throws IOException {
        super(file, new HTMLDocumentFactory(), bufferSize, compression, metadataFile);
    }

    /**
     * Copy constructor (that is, the one used by {@link #copy()}. Just
     * initializes final fields
     */
    public WARCDocumentCollection(String[] file, DocumentFactory factory,
                                  ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors,
                                  int bufferSize, Compression compression, File metadataFile) {
        super(file, factory, descriptors, bufferSize, compression, metadataFile);
    }

    @Override
    public Reference2ObjectMap<Enum<?>, Object> metadata(long index) {
        ensureDocumentIndex(index);
        final Reference2ObjectArrayMap<Enum<?>, Object> metadata
                = new Reference2ObjectArrayMap<>(4);

        try {
            metadataRandomAccess.seek(descriptors.get(index).metadataPosition);
            String docno = metadataRandomAccess.readUTF();
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.URI, docno);
        } catch (IOException e) {
            LOGGER.error(String.format("Could not retrieve metadata for file %d [%s]", index, e));
        }
        return metadata;
    }

    @Override
    protected void parseContent(int fileIndex, InputStream is, DataOutputStream metadataOutput)
            throws IOException {
        WarcRecord.newFile();
        WarcRecord warcRecord;
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
                    LOGGER.debug(String.format("Setting markers {%s, %d, %d}", docno,
                            currStart, currStop));

                long metadataPos = metadataOutput.size();
                metadataOutput.writeUTF(docno);

                descriptors.add(SegmentedDocumentDescriptor.create(fileIndex, currStart, currStop, metadataPos));
                LOGGER.debug("Descriptor size is " + size());
            }
        }
        dis.close();
        WarcRecord.readContent(oldReadContentFlag); // reset readContent flag
    }


    @Override
    public WARCDocumentCollection copy() {
        return new WARCDocumentCollection(files, factory().copy(), descriptors,
                bufferSize, compression, metadataFile);
    }


}

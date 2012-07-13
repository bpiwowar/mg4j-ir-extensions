package net.bpiwowar.mg4j.extensions;

import bpiwowar.argparser.Argument;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentCollection;

import java.util.ArrayList;

/**
 * Options common to collection building processes
 *
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 20/6/12
*/
public class CollectionBuilderOptions {
    @Argument(name = "collection", required = true, help = "The filename for the serialised collection")
    public String collection;

    @Argument(name = "unsorted", help = "Keep the file list unsorted (otherwise, files are sorted before the sequence is built - useful to move the built index on another computer)")
    public boolean unsorted = false;

    @Argument(name = "compression", help = "File compression")
    public Compression compression = Compression.NONE;

    @Argument(name = "bufferSize", help = "The size of an I/O buffer in bytes (default 64000)")
    public int bufferSize = TRECDocumentCollection.DEFAULT_BUFFER_SIZE;

    @Argument(name = "property", help = "A property (name=value)")
    public ArrayList<String> properties = new ArrayList<String>();
}

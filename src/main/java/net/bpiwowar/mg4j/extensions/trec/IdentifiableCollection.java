package net.bpiwowar.mg4j.extensions.trec;

import java.io.IOException;

/**
 * A collection where documents can be identified through an ID
 */
public interface IdentifiableCollection {
    long getDocumentFromURI(String uri);

    Object getDocumentURI(long id) throws IOException;
}

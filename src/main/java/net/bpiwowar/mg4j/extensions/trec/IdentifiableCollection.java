package net.bpiwowar.mg4j.extensions.trec;

import it.unimi.di.big.mg4j.document.DocumentCollection;

import java.io.IOException;

/**
 * A collection where documents can be identified through an ID
 */
public interface IdentifiableCollection extends DocumentCollection {
    long getDocumentFromURI(String uri);

    String getDocumentURI(long id) throws IOException;
}

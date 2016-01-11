package net.bpiwowar.mg4j.extensions.trec;

/**
 * A collection where documents can be identified through an ID
 */
public interface IdentifiableCollection {
    long getDocumentFromURI(String uri);
}

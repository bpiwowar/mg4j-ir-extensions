package net.bpiwowar.mg4j.extensions.rf;

/**
 * Bi-directional dictionary between internal and external identifiers
 *
 * @param <T>
 */
public abstract class DocumentFactory<T extends Document> {
    /**
     * Get the external document identifier
     */
    abstract public String getDocNo(T document);

    /* Get the internal document ID */
    abstract public T getDocument(String docno);
}

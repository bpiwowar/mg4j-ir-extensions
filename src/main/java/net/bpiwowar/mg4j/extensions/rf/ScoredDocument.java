package net.bpiwowar.mg4j.extensions.rf;

/**
 * A scored document
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/11/12
 */
public class ScoredDocument<T> {
    T document;
    float score;

    public ScoredDocument(T document, float score) {
        this.document = document;
        this.score = score;
    }
}

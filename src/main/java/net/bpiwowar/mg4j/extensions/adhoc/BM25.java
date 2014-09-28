package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.search.score.BM25Scorer;
import net.bpiwowar.mg4j.extensions.conf.IndexConfiguration;
import net.bpiwowar.mg4j.extensions.tasks.Adhoc;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/10/12
 */
@XmlRootElement(name = "bm25", namespace = Adhoc.MG4J_NAMESPACE)
public class BM25 extends MG4JScorer {
    @XmlAttribute
    double b;

    @XmlAttribute
    double k1;

    @Override
    public String toString() {
        return String.format("BM25(b=%f,k1=%f)", b, k1);
    }

    @Override
    public void init(DocumentCollection collection, IndexConfiguration index) throws Exception {
        scorer = new BM25Scorer(k1, b);
        super.init(collection, index);
    }

}

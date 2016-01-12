package net.bpiwowar.mg4j.extensions.positions;

import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.di.big.mg4j.index.IndexReader;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.dsi.fastutil.ints.IntBigList;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

/**
 * Sample terms with their positions in documents
 */
final public class Sampler {
    public static final Logger LOGGER = LoggerFactory.getLogger(Sampler.class);
    private final Source[] sources;
    private final Random random;
    private final long maxdocuments;

    public long getTermId() {
        return termId;
    }

    public long getDocumentId() {
        return docId;
    }

    public int count() throws IOException {
        return documents.count();
    }


    static public class Source {
        IndexedField indexedField;
        double weight;
        IndexReader indexReader;

        public Source(IndexedField indexedField, double weight, IndexReader indexReader) {
            this.indexedField = indexedField;
            this.weight = weight;
            this.indexReader = indexReader;
        }
    }

    public Sampler(long seed, Map<String, Double> fieldNames, IndexedCollection index, long maxdocuments) throws Exception {
        this.maxdocuments = maxdocuments;
        if (fieldNames.isEmpty()) {
            throw new RuntimeException("At least one field name should be given");
        }

        // Get all the indices
        sources = new Source[fieldNames.size()];

        int i = 0;
        for (Map.Entry<String, Double> entry : fieldNames.entrySet()) {
            String fieldName = entry.getKey();
            IndexedField _index = index.get(fieldName);
            double v = entry.getValue();
            if (v <= 0) v = _index.getNumberOfPostings();
            if (i > 0) v += sources[i - 1].weight;
            sources[i] = new Sampler.Source(_index, v, _index.getReader());
            if (!_index.index.hasPositions) {
                throw new RuntimeException("No positions for index " + fieldName + "!");
            }
            ++i;
        }

        double totalWeight = sources[sources.length - 1].weight;
        for (Sampler.Source source : sources) {
            source.weight /= totalWeight;
        }

        random = new Random(seed);
        LOGGER.info("Random seed is {}", seed);

    }

    // Current state
    Source source;
    long termId;
    long docId;
    private IntBigList sizes;
    private IndexIterator documents;
    private double samplingRate;

    public Integer getDocumentSize() {
        return sizes.get(docId);
    }

    public int nextPosition() throws IOException {
        return documents.nextPosition();
    }

    public CharSequence getTerm() {
        return source.indexedField.getTerm(termId);
    }

    public Source getSource() {
        return source;
    }

    /**
     * Move to the next sample. Information about the current sample can be retrieved through
     * a set of methods
     * <ul>
     * <li>Document information: document id {@link #getDocumentId()} and size {@link #getDocumentSize()}</li>
     * <li>Term information: term id with {@link #getTermId()} and term string with {@link #getTerm()}</li>
     * </ul>
     *
     * @throws IOException If something goes wrong
     */
    public boolean next() throws IOException {
        while (true) {
            // If source is null, we have to choose
            if (source == null) {
                // Choose index and term
                final double v = random.nextDouble();
                int ix = 0;
                for (; ix < sources.length; ++ix) {
                    if (sources[ix].weight >= v) {
                        break;
                    }
                }
                assert ix < sources.length;

                final Sampler.Source source = sources[ix];
                long termId = (long) (random.nextFloat() * source.indexedField.index.numberOfTerms);
                sizes = source.indexedField.index.sizes;
                // Outputs
                documents = source.indexReader.documents(termId);
                samplingRate = maxdocuments / (double) documents.frequency();
            }


            while ((docId = documents.nextDocument()) != DocumentIterator.END_OF_LIST) {
                if (samplingRate >= 1. || random.nextDouble() < samplingRate) {
                    // Found one
                    documents.count();
                    return false;
                }
            }
        }
    }
}

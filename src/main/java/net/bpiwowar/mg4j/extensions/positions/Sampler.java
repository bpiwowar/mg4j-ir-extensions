package net.bpiwowar.mg4j.extensions.positions;

import it.unimi.di.big.mg4j.search.DocumentIterator;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.trec.IdentifiableCollection;
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


    public Sampler(long seed, Map<String, Double> fieldNames, IndexedCollection index, long maxdocuments) throws Exception {
        this.maxdocuments = maxdocuments;
        if (fieldNames.isEmpty()) {
            throw new RuntimeException("At least one field name should be given");
        }

        // Get all the indices
        sources = Source.getSources(fieldNames, index);

        double totalWeight = sources[sources.length - 1].weight;
        for (Source source : sources) {
            source.weight /= totalWeight;
        }

        random = new Random(seed);
        LOGGER.info("Random seed is {}", seed);

    }

    public SamplerState state = new SamplerState();
    double samplingRate;

    /**
     * Move to the next sample. Information about the current sample can be retrieved through
     * a set of methods
     * <ul>
     * <li>Document information: document id {@link SamplerState#getDocumentId()} and size {@link SamplerState#getDocumentSize()}</li>
     * <li>Term information: term id with {@link SamplerState#getTermId()} and term string with {@link SamplerState#getTerm()}</li>
     * </ul>
     *
     * @throws IOException If something goes wrong
     */
    public boolean next() throws IOException {
        while (true) {
            // If source is null, we have to choose
            if (state.source == null) {
                // Choose index and term
                final double v = random.nextDouble();
                int ix = 0;
                for (; ix < sources.length; ++ix) {
                    if (sources[ix].weight >= v) {
                        break;
                    }
                }
                assert ix < sources.length;

                Source source = sources[ix];
                long termId = (long) (random.nextFloat() * source.indexedField.index.numberOfTerms);
                state.setSample(source, termId);
                samplingRate = maxdocuments / (double) state.documents.frequency();
            }


            while ((state.docId = state.documents.nextDocument()) != DocumentIterator.END_OF_LIST) {
                if (samplingRate >= 1. || random.nextDouble() < samplingRate) {
                    // Found one
                    return false;
                }
            }

            state.source = null;
        }
    }


}

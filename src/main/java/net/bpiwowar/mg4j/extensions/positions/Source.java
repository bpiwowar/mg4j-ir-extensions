package net.bpiwowar.mg4j.extensions.positions;

import it.unimi.di.big.mg4j.index.IndexReader;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.trec.IdentifiableCollection;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A source for sampling: a field of an index
 */
public class Source {
    final public IndexedField indexedField;
    public final IndexReader indexReader;
    final IdentifiableCollection collection;

    double weight;
    public final long unknownTermId;

    // Internal index
    public byte index;

    static public class Input {
        @JsonArgument(required = true)
        IndexedCollection index;

        @JsonArgument(name = "fields", help = "The fields to output, associated to a weight (for sampling)." +
                "If the weight is < 0, then it is initialized to the number of values", required = true)
        Map<String, Double> fieldNames = new HashMap<>();
    }

    public Source(IndexedField indexedField, double weight, IndexReader indexReader, IdentifiableCollection collection) {
        this.indexedField = indexedField;
        this.weight = weight;
        this.indexReader = indexReader;
        this.collection = collection;
        this.unknownTermId = indexedField.getUnknownTermId();
    }

    public static Source[] getSources(Input[] inputs) throws Exception {
        final Integer size = Stream.of(inputs).map(i -> i.fieldNames.size()).reduce(0, (a, b) -> a + b);
        final Source[] sources = new Source[size];
        int i = 0;
        for (Input input : inputs) {
            addSources(input.fieldNames, input.index, sources, i);
        }
        return sources;
    }

    public static Source[] getSources(Map<String, Double> fieldNames, IndexedCollection index) throws Exception {
        final Source[] sources = new Source[fieldNames.size()];
        int i = 0;
        addSources(fieldNames, index, sources, i);
        return sources;
    }

    private static void addSources(Map<String, Double> fieldNames, IndexedCollection index, Source[] sources, int i) throws Exception {
        for (Map.Entry<String, Double> entry : fieldNames.entrySet()) {
            String fieldName = entry.getKey();
            IndexedField _index = index.get(fieldName);
            double v = entry.getValue();
            if (v <= 0) v = _index.getNumberOfPostings();
            if (i > 0) v += sources[i - 1].weight;
            sources[i] = new Source(_index, v, _index.getReader(), (IdentifiableCollection) index.getCollection().get());
            if (!_index.index.hasPositions) {
                throw new RuntimeException("No positions for index " + fieldName + "!");
            }
            ++i;
        }
    }

    public String name() {
        return indexedField.field;
    }

    public long getTermId(CharSequence term) {
        return indexedField.getTermId(term);
    }

    public int getDocumentFrequency(long tid) throws IOException {
        return indexedField.getFrequencies().get(tid);
    }

    public int getDocumentLength(long document) {
        return indexedField.getSize(document);
    }
}

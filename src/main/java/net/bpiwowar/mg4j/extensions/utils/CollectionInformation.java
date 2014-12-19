package net.bpiwowar.mg4j.extensions.utils;

import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentIterator;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.tasks.Collection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by bpiwowar on 16/12/14.
 */
public class CollectionInformation {
    private final DocumentCollection collection;
    private final DocumentFactory factory;
    public final int[] fields;
    public final DocumentFactory.FieldType[] types;
    public final long size;

    public CollectionInformation(Collection collection, ArrayList<String> fieldNames) throws IllegalAccessException, InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
        this.collection = collection.get();
        this.factory = this.collection.factory();
        this.fields = new int[fieldNames.size()];
        this.types = new DocumentFactory.FieldType[fields.length];
        for (int i = fields.length; --i >= 0; ) {
            fields[i] = factory.fieldIndex(fieldNames.get(i));
            types[i] = factory.fieldType(i);
        }
        this.size = this.collection.size();
    }

    /**
     * Returns an iterator over a range
     *
     * @param start Start of the range
     * @param end   End of the range
     * @return
     * @throws java.io.IOException
     */
    public CloseableIterator<DocumentInformation> range(long offset, long start, long end) throws IOException {
        // In a segmented collection, prefer the iterator way
        if (collection instanceof SegmentedDocumentCollection) {
            assert start >= 0;
            final DocumentIterator iterator = ((SegmentedDocumentCollection) collection).iterator(start);
            return new CloseableIterator<DocumentInformation>() {
                long position = start;

                @Override
                public void close() throws Exception {
                    iterator.close();
                }

                @Override
                protected DocumentInformation computeNext() {
                    if (position >= end) return endOfData();
                    final Document document = Streams.propagateProducer(() -> iterator.nextDocument()).produce();
                    return document == null ? endOfData() : new DocumentInformation(offset + position++, document);
                }
            };
        }

        // Slow random access
        return new CloseableIterator<DocumentInformation>() {
            long position = start;

            @Override
            protected DocumentInformation computeNext() {
                if (position >= end) return endOfData();
                try {
                    final Document document = collection.document(position);
                    return new DocumentInformation(offset + position++, document);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public DocumentIterator iterator() throws IOException {
        return collection.iterator();
    }

    public Document document(Long index) throws IOException {
        return collection.document(index);
    }


    public class DocumentInformation implements AutoCloseable {
        public long docid;
        public Document document;

        public DocumentInformation(long docid, Document document) {
            this.docid = docid;
            this.document = document;
        }


        @Override
        public void close() throws Exception {
            document.close();
        }

        public int[] fields() {
            return CollectionInformation.this.fields;
        }

        public DocumentFactory.FieldType[] types() {
            return CollectionInformation.this.types;
        }
    }
}

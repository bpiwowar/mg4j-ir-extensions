package net.bpiwowar.mg4j.extensions.positions;

import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.dsi.fastutil.ints.IntBigList;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Represents a sample
 */
public class SamplerState {
    Source source;
    long termId;
    long docId;
    private IntBigList sizes;
    IndexIterator documents;


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

    public long getTermId() {
        return termId;
    }

    public long getDocumentId() {
        return docId;
    }

    /** Number of occurrences in document */
    public int count() throws IOException {
        return documents.count();
    }

    /** Number of documents the term appears in */
    public long frequency() throws IOException {
        return documents.frequency();
    }

    /**
     * Ouptuts
     * @param out
     * @throws IOException
     */
    public void print(PrintStream out) throws IOException {
        out.print(getTerm());
        out.print('\t');
        out.print(source.collection.getDocumentURI(docId));
        out.print('\t');
        out.print(getSource().name());
        out.print('\t');
        out.print(getTermId());
        out.print('\t');
        out.print(getDocumentSize());
        int position;
        while ((position = documents.nextPosition()) != IndexIterator.END_OF_POSITIONS) {
            out.print('\t');
            out.print(position);
        }
        out.println();
    }

    public void setSample(Source source, long termId) throws IOException {
        this.source = source;
        this.termId = termId;
        this.sizes = this.source.indexedField.index.sizes;
        this.documents = this.source.indexReader.documents(this.termId);
    }

    public long nextDocument() throws IOException {
        return docId = documents.nextDocument();
    }
}

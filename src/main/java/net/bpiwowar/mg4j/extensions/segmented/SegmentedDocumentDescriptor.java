package net.bpiwowar.mg4j.extensions.segmented;

/**
 * A compact description of the location and of the internal segmentation of
 * a TREC document inside a file.
 */
public class SegmentedDocumentDescriptor implements Cloneable {
    /**
     * A reference to the file containing this document.
     */
    public int fileIndex;

    /**
     * The starting position of this document in the file.
     */
    public long startMarker;

    /**
     * The ending position.
     */
    public int stopMarkerDiff;

    /**
     * The document ID
     */
    public String docid;

    public SegmentedDocumentDescriptor(String docid, int findex, long start,
                                       int stopMarkerDiff) {
        this.docid = docid;
        this.fileIndex = findex;
        this.startMarker = start;
        this.stopMarkerDiff = stopMarkerDiff;
    }

    public SegmentedDocumentDescriptor(String docid, int findex, long start,
                                       long stop) {
        this(docid, findex, start, (int) (stop - start));

    }

    @Override
    public Object clone() {
        return new SegmentedDocumentDescriptor(docid, fileIndex, startMarker,
                stopMarkerDiff);
    }

    public final long[] toSegments() {
        return new long[]{startMarker, stopMarkerDiff + startMarker};

    }

}

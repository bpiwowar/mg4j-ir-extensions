package net.bpiwowar.mg4j.extensions.segmented;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
     * The position in the metadata stream
     */
    public long metadataPosition;


    final static public SegmentedDocumentDescriptor create(int findex, long start,
                                        long stop, long metadataPosition) {
        return new SegmentedDocumentDescriptor(findex, start, (int) (stop - start), metadataPosition);
    }

    private SegmentedDocumentDescriptor(int findex, long start,
                                       int stopMarkerDiff, long metadataPosition) {
        this.fileIndex = findex;
        this.startMarker = start;
        this.stopMarkerDiff = stopMarkerDiff;
        this.metadataPosition = metadataPosition;
    }


    SegmentedDocumentDescriptor(ObjectInputStream s) throws IOException {
        this.fileIndex = s.readInt();
        this.startMarker= s.readLong();
        this.stopMarkerDiff = s.readInt();
        this.metadataPosition = s.readLong();
    }


    @Override
    public Object clone() {
        return new SegmentedDocumentDescriptor(fileIndex, startMarker,
                stopMarkerDiff, metadataPosition);
    }

    public final long[] toSegments() {
        return new long[]{startMarker, stopMarkerDiff + startMarker};

    }

    public void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(this.fileIndex);
        s.writeLong(this.startMarker);
        s.writeInt(this.stopMarkerDiff);
        s.writeLong(this.metadataPosition);

    }
}

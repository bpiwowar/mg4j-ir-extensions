package net.bpiwowar.mg4j.extensions.utils;

/*		 
 * MG4J: Managing Gigabytes for Java
 *
 * Copyright (C) 2005-2007 Paolo Boldi and Sebastiano Vigna 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

import it.unimi.di.big.mg4j.document.AbstractDocument;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.di.big.mg4j.util.parser.callback.AnchorExtractor;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.HTMLFactory;
import it.unimi.dsi.parser.callback.ComposedCallbackBuilder;
import it.unimi.dsi.util.Properties;
import net.bpiwowar.mg4j.extensions.MarkedUpDocument;
import net.bpiwowar.mg4j.extensions.TagPointer;
import net.bpiwowar.mg4j.extensions.warc.WarcHTMLResponseRecord;
import net.bpiwowar.mg4j.extensions.warc.WarcRecord;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * A factory that provides fields for body and title of HTML documents. It uses
 * internally a {@link BulletParser}.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */

public class HTMLDocumentFactory extends PropertyBasedDocumentFactory {
    final static private Logger LOGGER = LoggerFactory.getLogger(HTMLDocumentFactory.class);

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;
    /**
     * A parser that will be used to extract text from HTML documents.
     */
    private transient BulletParser parser;
    /**
     * The callback recording text.
     */
    private transient StructuredTextExtractor textExtractor;
    /**
     * The callback for anchors.
     */
    private transient AnchorExtractor anchorExtractor;
    /**
     * The word reader used for all documents.
     */
    private transient WordReader wordReader;

    private transient char[] text;

    /**
     * The collection type (e.g. TREC, WARC)
     */
    private CollectionType collectionType = CollectionType.WARC018;

    @Override
    protected boolean parseProperty(final String key, final String[] values,
                                    final Reference2ObjectMap<Enum<?>, Object> metadata) throws ConfigurationException {
        if (sameKey(PropertyBasedDocumentFactory.MetadataKeys.MIMETYPE, key)) {
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.MIMETYPE,
                    ensureJustOne(key, values));
            return true;
        } else if (sameKey(PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                key)) {
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                    Charset.forName(ensureJustOne(key, values)).toString());
            return true;
        }

        return super.parseProperty(key, values, metadata);
    }


    private void init() {
        /** The HTML bullet parser */
        this.parser = new BulletParser(HTMLFactory.INSTANCE);

        ComposedCallbackBuilder composedBuilder = new ComposedCallbackBuilder();

        composedBuilder
                .add(this.textExtractor = new StructuredTextExtractor());
        parser.setCallback(composedBuilder.compose());

        this.wordReader = new FastBufferedReader();
        text = new char[DEFAULT_BUFFER_SIZE];

        if (collectionType == null)
            collectionType = CollectionType.WARC018;

    }

    private void initVars() {
    }

    /**
     * Returns a copy of this document factory. A new parser is allocated for
     * the copy.
     */
    @Override
    public HTMLDocumentFactory copy() {
        return new HTMLDocumentFactory(defaultMetadata);
    }

    public HTMLDocumentFactory(final Properties properties)
            throws ConfigurationException {
        super(properties);
        initVars();
        init();
    }

    public HTMLDocumentFactory(
            final Reference2ObjectMap<Enum<?>, Object> defaultMetadata) {
        super(defaultMetadata);
        initVars();
        init();
    }

    public HTMLDocumentFactory(final String[] property)
            throws ConfigurationException {
        super(property);
        initVars();
        init();
    }

    public HTMLDocumentFactory() {
        super();
        initVars();
        init();
    }

    @Override
    public int numberOfFields() {
        return Fields.values().length;
    }

    public static enum Fields {
        TEXT, TITLE
    }

    /**
     * Type of collection
     */
    public static enum CollectionType {
        // collections in WARC 0.18 format
        WARC018
    }

    @Override
    public String fieldName(final int field) {
        ensureFieldIndex(field);

        switch (field) {
            case 0:
                return "text";
            case 1:
                return "title";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int fieldIndex(final String fieldName) {
        for (int i = 0; i < numberOfFields(); i++)
            if (fieldName(i).equals(fieldName))
                return i;
        return -1;
    }

    @Override
    public FieldType fieldType(final int field) {
        ensureFieldIndex(field);
        switch (field) {
            case 0:
                return FieldType.TEXT;
            case 1:
                return FieldType.TEXT;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void readObject(final ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        s.defaultReadObject();
        init();
    }


    @Override
    public WARCSegmentedDocument getDocument(final InputStream rawContent,
                                             final Reference2ObjectMap<Enum<?>, Object> metadata)
            throws IOException {
        return new WARCSegmentedDocument(rawContent, metadata);
    }

    /**
     * A TREC document. If a <samp>TITLE</samp> element is available, it will be
     * used for {@link #title()} instead of the default value.
     * <p/>
     * <p/>
     * The document may be segmented.
     * <p/>
     * We delay the actual parsing until it is actually necessary, so operations
     * like getting the document URI will not require parsing.
     */
    public class WARCSegmentedDocument extends AbstractDocument implements
            MarkedUpDocument {
        private final Reference2ObjectMap<Enum<?>, Object> metadata;
        /**
         * Whether we already parsed the document.
         */
        private boolean parsed;
        /**
         * The cached raw content.
         */
        private final InputStream rawContent;


        /**
         * Here's where the actual document is parsed (using the BulletParser)
         *
         * @throws java.io.IOException
         */
        private void ensureParsed() throws IOException {
            if (parsed)
                return;

            switch (collectionType) {
                case WARC018:
                    parseWAR018CDocument();
                    break;
            }
        }

        /**
         * Parses a document from a WARC collection
         *
         * @throws java.io.IOException
         */
        private void parseWAR018CDocument() throws IOException {
            WarcRecord warcRecord = null;
            DataInputStream dis = new DataInputStream(rawContent);

            // Regardless of what the stream gives us, we read and return
            // the first entry which is a response.
            WarcHTMLResponseRecord warcResponse = null;
            while ((warcRecord = WarcRecord.readNextWarcRecord(dis)) != null) {
                // ignore if no WARC response type, otherwise read and finish
                if (warcRecord.getHeaderRecordType().equals("response")) {
                    warcResponse = new WarcHTMLResponseRecord(warcRecord);
                    break;
                }
            }

            if (warcResponse != null) {

                // parse the HTML content (skip HTTP header)
                Reader reader = warcResponse.getContentReader();
                int len = 0;
                while ((len = reader.read(text, 0, text.length)) >= 0) {
                    parser.parse(text, 0, len);
                }
                reader.close();


                // We use the TrecID for the URI
                metadata.put(MetadataKeys.URI, warcResponse.getTargetTrecID());
                // Set the title
                metadata.put(MetadataKeys.TITLE, textExtractor.title.trim());

                parsed = true;
            } else {
                LOGGER.warn("Couldn't find WARC response in stream!");
                if (warcRecord != null) {
                    LOGGER.warn("{}", warcRecord);
                } else LOGGER.warn("WARC record is null!");
                throw new IOException("Couldn't find WARC response in stream!");
            }
        }


        @Override
        public Iterator<TagPointer> tags(int field) {
            try {
                ensureParsed();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            switch (field) {
                case 0:
                    return textExtractor.tagPointer();
                case 1:
                    return null;
                case 2:
                    return null;
                default:
                    throw new IllegalArgumentException();
            }
        }

        public WARCSegmentedDocument(final InputStream rawContent,
                                     final Reference2ObjectMap<Enum<?>, Object> metadata) {
            this.metadata = metadata;
            this.rawContent = rawContent;
        }

        @Override
        public CharSequence title() {
            try {
                ensureParsed();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return (CharSequence) (textExtractor.title.length() == 0 ? resolve(
                    PropertyBasedDocumentFactory.MetadataKeys.TITLE, metadata)
                    : textExtractor.title);
        }

        @Override
        public String toString() {
            return title().toString();
        }

        @Override
        public CharSequence uri() {
            return (CharSequence) resolve(
                    PropertyBasedDocumentFactory.MetadataKeys.URI, metadata);
        }

        @Override
        public Object content(final int field) throws IOException {
            ensureFieldIndex(field);
            ensureParsed();
            switch (field) {
                case 0:
                    return new FastBufferedReader(textExtractor.text);
                case 1:
                    return new FastBufferedReader(textExtractor.title);
                case 2:
                    return anchorExtractor.anchors;
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * Returns the text of this document
         *
         * @return the text
         */
        public MutableString getText() throws IOException {
            ensureFieldIndex(0);
            ensureParsed();
            return textExtractor.getText();
        }

        @Override
        public WordReader wordReader(final int field) {
            ensureFieldIndex(field);
            return wordReader;
        }

    }

    public WordReader getWordReader() {
        return wordReader;
    }

    /**
     * Sets the type of the underlying collection (e.g. standard TREC
     * collection, WARC collection)
     *
     * @param t the collection type
     */
    public void setCollectionType(CollectionType t) {
        this.collectionType = t;
    }
}

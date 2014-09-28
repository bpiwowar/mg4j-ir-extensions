package net.bpiwowar.mg4j.extensions.trec;

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
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.callback.ComposedCallbackBuilder;
import it.unimi.dsi.util.Properties;
import net.bpiwowar.mg4j.extensions.MarkedUpDocument;
import net.bpiwowar.mg4j.extensions.TagPointer;
import net.bpiwowar.mg4j.extensions.utils.StructuredTextExtractor;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * A factory that provides fields for body and title of HTML documents. It uses
 * internally a {@link BulletParser}.
 */

public class TRECDocumentFactory extends PropertyBasedDocumentFactory {
    final static private Logger LOGGER = Logger.getLogger(TRECDocumentFactory.class);

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
        LOGGER.info("Initialising the TREC document factory");

        // The parser is a SGML BulletParser with TREC vocabulary
        this.parser = new BulletParser(TRECParsingFactory.INSTANCE);

        ComposedCallbackBuilder composedBuilder = new ComposedCallbackBuilder();

        composedBuilder.add(this.textExtractor = new StructuredTextExtractor());

        this.textExtractor.ignore(
                TRECParsingFactory.ELEMENT_DOCNO,
                TRECParsingFactory.ELEMENT_FILEID,
                TRECParsingFactory.ELEMENT_FIRST,
                TRECParsingFactory.ELEMENT_SECOND
        );
        parser.setCallback(composedBuilder.compose());

        this.wordReader = new FastBufferedReader();
        text = new char[DEFAULT_BUFFER_SIZE];
    }

    private void initVars() {
    }

    /**
     * Returns a copy of this document factory. A new parser is allocated for
     * the copy.
     */
    @Override
    public TRECDocumentFactory copy() {
        return new TRECDocumentFactory(defaultMetadata);
    }

    public TRECDocumentFactory(final Properties properties)
            throws ConfigurationException {
        super(properties);
        initVars();
        init();
    }

    public TRECDocumentFactory(
            final Reference2ObjectMap<Enum<?>, Object> defaultMetadata) {
        super(defaultMetadata);
        initVars();
        init();
    }

    public TRECDocumentFactory(final String[] property)
            throws ConfigurationException {
        super(property);
        initVars();
        init();
    }

    public TRECDocumentFactory() {
        super();
        initVars();
        init();
    }

    @Override
    public int numberOfFields() {
        return Fields.values().length;
    }

    /** Fields that can be returned */
    public static enum Fields {
        TEXT, TITLE
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

    final static char[] DOCNO_OPEN = "<DOCNO>".toCharArray();
    final static char[] DOCNO_CLOSE = "</DOCNO>".toCharArray();

    @Override
    public TRECSegmentedDocument getDocument(final InputStream rawContent,
                                             final Reference2ObjectMap<Enum<?>, Object> metadata)
            throws IOException {
        return new TRECSegmentedDocument(rawContent, metadata);
    }

    /**
     * A TREC document. If a <samp>TITLE</samp> element is available, it will be
     * used for {@link #title()} instead of the default value.
     *
     * The document may be segmented.
     *
     * We delay the actual parsing until it is actually necessary, so operations
     * like getting the document URI will not require parsing.
     */
    public class TRECSegmentedDocument extends AbstractDocument implements
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

            parseTRECDocument();
        }


        /**
         * Parses a TREC document
         *
         * @throws java.io.IOException
         */
        private void parseTRECDocument() throws IOException {
            // --- Read everything
            int offset = 0, l;

            InputStreamReader r = new InputStreamReader(rawContent,
                    (String) resolveNotNull(
                            PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                            metadata));

            while ((l = r.read(text, offset, text.length - offset)) > 0) {
                offset += l;
                text = CharArrays.grow(text, offset + 1);
            }

            // --- Search for DOCNO and use it for the URI metadata

            int docno_start = -1;
            for (int i = 0, n = 0; i < offset; i++) {
                if (docno_start < 0)
                    if (text[i] == DOCNO_OPEN[n]) {
                        n++;
                        if (n == DOCNO_OPEN.length) {
                            docno_start = i + 1;
                            n = 0;
                        }
                    } else
                        n = 0;
                else {
                    if (text[i] == DOCNO_CLOSE[n]) {
                        n++;
                        if (n == DOCNO_CLOSE.length) {
                            String docno = new String(text, docno_start, i
                                    - DOCNO_CLOSE.length - docno_start + 1)
                                    .trim();
                            metadata.put(MetadataKeys.URI, docno);
                            break;
                        }
                    } else
                        n = 0;
                }
            }

            // Parse the text
            parser.parse(text, 0, offset);
            textExtractor.title.trim();
            parsed = true;
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

        public TRECSegmentedDocument(final InputStream rawContent,
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

}

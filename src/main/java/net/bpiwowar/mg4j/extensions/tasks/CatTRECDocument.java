package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.Attribute;
import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.Element;
import it.unimi.dsi.parser.callback.Callback;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentCollection;
import net.bpiwowar.mg4j.extensions.trec.TRECParsingFactory;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * Wrapper for Index in MG4J
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(name = "cat-trec-document", project = {"ir", "mg4j"},
        description = "Outputs a TREC document in an easy to handle format")
public class CatTRECDocument extends AbstractTask {
    static final Logger logger = Logger.getLogger(CatTRECDocument.class);

    @Override
    public int execute() throws Throwable {
        FastBufferedInputStream in = new FastBufferedInputStream(System.in);

        byte[] buffer = new byte[8192];
        byte[] docnoBuffer = new byte[512];

        Charset charset = Charset.forName("ISO-8859-1");
        final CharsetDecoder decoder = charset.newDecoder();

        final BulletParser parser = new BulletParser(TRECParsingFactory.INSTANCE);


        final HashSet<Element> elements = new HashSet<>(Arrays.asList(TRECParsingFactory.ELEMENT_TEXT,
                TRECParsingFactory.ELEMENT_HEADLINE, TRECParsingFactory.ELEMENT_LEADPARA,
                TRECParsingFactory.ELEMENT_ABST, TRECParsingFactory.ELEMENT_SUMMARY,
                TRECParsingFactory.ELEMENT_HL));

        final PrintWriter writer = new PrintWriter(System.out);

        parser.setCallback(new Callback() {
            int inContent;
            boolean output;

            @Override
            public void configure(BulletParser parser) {
                parser.parseTags(true);
                parser.parseText(true);
            }

            @Override
            public void startDocument() {
                inContent = 0;
                output = false;
            }

            @Override
            public boolean startElement(Element element, Map<Attribute, MutableString> attributeMutableStringMap) {
                if (elements.contains(element)) {
                    inContent++;
                    if (output)
                       writer.println();
                }
                return true;
            }

            @Override
            public boolean endElement(Element element) {
                if (elements.contains(element)) {
                    inContent--;
                }
                return true;
            }

            @Override
            public boolean characters(char[] chars, int offset, int length, boolean b) {
                if (inContent > 0) {
                    for(int i = offset; i < offset+length; i++) {
                        if (Character.isSpaceChar(chars[i]))
                            writer.write(' ');
                        else
                            writer.write(chars[i]);
                    }
                }
                return true;
            }

            @Override
            public boolean cdata(Element element, char[] chars, int i, int i1) {
                return true;
            }

            @Override
            public void endDocument() {
                if (!output) writer.println();
            }
        });

        TRECDocumentCollection.EventHandler handler = new TRECDocumentCollection.EventHandler() {
            char[] text = new char[8192];
            int offset = 0;

            @Override
            public void startDocument() {
                offset = 0;
            }

            @Override
            public void endDocument(String docno, long currStart, long currStop) throws IOException {
                writer.format("%%%%%% DOCUMENT: %s%n", docno);
                parser.parse(text, 0, offset);
                writer.println();
            }

            @Override
            public void write(byte[] bytes, int offset, int length) {
                text = CharArrays.ensureCapacity(text, this.offset + length + 1);
                final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, offset, length);
                final CharBuffer charBuffer = CharBuffer.wrap(text, this.offset, length + 1);
                final CoderResult result = decoder.decode(byteBuffer, charBuffer, true);
                this.offset = charBuffer.position();
            }
        };

        TRECDocumentCollection.parseContent(in, buffer, docnoBuffer, handler);

        writer.flush();

        return 0;
    }
}

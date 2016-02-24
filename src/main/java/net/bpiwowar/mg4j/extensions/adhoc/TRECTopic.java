package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.Attribute;
import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.Element;
import it.unimi.dsi.parser.callback.DefaultCallback;
import net.bpiwowar.mg4j.extensions.query.*;
import net.bpiwowar.mg4j.extensions.trec.TRECParsingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * A TREC topic
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TRECTopic implements Topic {
    final static private Logger logger = LoggerFactory.getLogger(Topic.class);

    protected String id;

    protected String narrative;

    protected String description;

    protected String title;

    protected String concepts;

    protected String definitions;

    protected String summary;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getNarrative() {
        return narrative;
    }

    public String getSummary() {
        return summary;
    }

    public String getConcepts() {
        return concepts;
    }

    public String getDefinitions() {
        return definitions;
    }

    /**
     * Read a set of TREC topics
     *
     * @param reader
     * @param quoteCommas Should we quote word separated by commas?
     * @return
     * @throws IOException
     */
    static public DefaultQuerySet readTopics(BufferedReader reader,
                                             final boolean quoteCommas) throws IOException {
        logger.debug("Reading a topic file");
        final DefaultQuerySet querySet = new DefaultQuerySet();

        BulletParser bulletParser = new BulletParser(
                TRECParsingFactory.INSTANCE);

        bulletParser.setCallback(new DefaultCallback() {
            TRECTopic topic = null;
            MutableString curText = new MutableString();
            Element curElement;

            @Override
            public boolean characters(char[] text, int offset, int length,
                                      boolean flowBroken) {
                curText.append(text, offset, length);
                return true;
            }

            @Override
            public boolean startElement(Element element,
                                        Map<Attribute, MutableString> attrMapUnused) {

                // --- New tag
                if (topic != null)
                    process();

                // ---
                if (element == TRECParsingFactory.ELEMENT_TOP) {
                    topic = new TRECTopic();
                }
                curElement = element;
                return true;
            }

            void removePrefix(String prefix, MutableString text) {
                if (text.startsWith(prefix))
                    text.delete(0, prefix.length());

            }

            private void process() {
                curText.trim();
                curText.replace('\n', ' ');
                curText.squeezeSpaces(false);

                if (curElement == TRECParsingFactory.ELEMENT_TITLE) {
                    removePrefix("Topic: ", curText);
                    if (quoteCommas) {
                        StringBuilder builder = new StringBuilder();
                        boolean first = true;
                        for (String part : curText.toString()
                                .split("\\s*,\\s*")) {
                            if (first)
                                first = false;
                            else
                                builder.append(' ');

                            if (part.indexOf(' ') >= 0) {
                                builder.append('"');
                                builder.append(part);
                                builder.append('"');
                            } else
                                builder.append(part);
                        }

                        topic.title = builder.toString();
                    } else
                        topic.title = curText.toString();
                } else if (curElement == TRECParsingFactory.ELEMENT_NUM) {
                    removePrefix("Number: ", curText);
                    // Normalise the number
                    topic.id = new Integer(curText.toString()).toString();
                } else if (curElement == TRECParsingFactory.ELEMENT_DESC) {
                    removePrefix("Description: ", curText);
                    topic.description = curText.toString();
                } else if (curElement == TRECParsingFactory.ELEMENT_NARR) {
                    // TREC
                    removePrefix("Narrative: ", curText);
                    topic.narrative = curText.toString();
                } else if (curElement == TRECParsingFactory.ELEMENT_SMRY) {
                    removePrefix("Summary: ", curText);
                    topic.summary = curText.toString();
                } else if (curElement == TRECParsingFactory.ELEMENT_CON) {
                    // TREC 1
                    removePrefix("Concepts: ", curText);
                    topic.concepts = curText.toString();
                } else if (curElement == TRECParsingFactory.ELEMENT_DEF) {
                    // TREC 1
                    removePrefix("Definition(s): ", curText);
                    removePrefix("Definition: ", curText);
                    topic.definitions = curText.toString();
                }
                curElement = null;
                curText.delete(0, curText.length());
            }

            @Override
            public boolean endElement(Element element) {
                if (topic != null)
                    process();

                if (element == TRECParsingFactory.ELEMENT_TOP) {
                    if (topic.id == null) {
                        logger.warn("Topic had no identifier - skipping");
                    } else {
                        logger.debug("Adding topic {} with title [{}]", topic.id, topic.title);

                        querySet.put(topic.id, topic);
                    }
                    topic = null;
                }
                return true;
            }
        });

        // Read the file & parse
        char text[] = new char[8192];
        int offset = 0, l;
        while ((l = reader.read(text, offset, text.length - offset)) > 0) {
            offset += l;
            text = CharArrays.grow(text, offset + 1);
        }

        bulletParser.parseText(true);
        bulletParser.parseCDATA(true);
        bulletParser.parseTags(true);
        bulletParser.parse(text, 0, offset);

        return querySet;

    }

    @Override
    public String getId() {
        return id;
    }

    final static List<String> list = Arrays.asList("title", "desc");

    @Override
    public Query getTopicPart(QueryType type) {
        switch(type) {
            case TITLE:
                return new StringQuery(title);
            case DESCRIPTION:
                return new StringQuery(description);
        }
        return null;
    }

}

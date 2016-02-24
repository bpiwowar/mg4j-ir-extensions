package net.bpiwowar.mg4j.extensions.query;

import com.google.common.collect.ImmutableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * TREC topics for Web track (2009-14)
 */
public class TRECWebTopic implements Topic {

    private final String id;
    private final String type;
    private final ArrayList<SubTopic> subtopics = new ArrayList<>();
    private StringQuery query;
    private StringQuery description;

    static public class SubTopic {

        private final String number;
        private final String type;
        private final String text;

        public SubTopic(String number, String type, String text) {
            this.number = number;
            this.type = type;
            this.text = text;
        }
    }


    public TRECWebTopic(Element element) throws XMLStreamException {
        id = element.getAttribute("number");
        type = element.getAttribute("type");

        final NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            final Node item = nodes.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE) continue;
            Element child = (Element) item;
            switch (item.getLocalName()) {
                case "query":
                    query = new StringQuery(item.getTextContent());
                    break;
                case "description":
                    description = new StringQuery(item.getTextContent());
                    break;
                case "subtopic":
                    subtopics.add(new SubTopic(child.getAttribute("number"), child.getAttribute("type"), child.getTextContent()));
                    break;
            }
        }

    }

    private static String text(XMLStreamReader xmlr) {
        return null;
    }

    public static QuerySet readTopics(InputStream is) throws ParserConfigurationException, IOException, SAXException, XMLStreamException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(is);

        DefaultQuerySet set = new DefaultQuerySet();
        final Element root = document.getDocumentElement();
        if (!root.getLocalName().startsWith("webtrack"))
            throw new IOException("Root tag name should start with webtrack");

        final NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            if (nodes.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            if (nodes.item(i).getLocalName().equals("topic"))
                set.add(new TRECWebTopic((Element) nodes.item(i)));
        }

        return set;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Query getTopicPart(QueryType type) {
        switch (type) {
            case TITLE:
                return query;
            case DESCRIPTION:
                return description;
        }
        return null;
    }

}

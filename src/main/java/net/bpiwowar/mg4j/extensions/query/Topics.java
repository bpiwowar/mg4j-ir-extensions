package net.bpiwowar.mg4j.extensions.query;

import net.bpiwowar.mg4j.extensions.Utils;
import net.bpiwowar.mg4j.extensions.trec.TRECTopic;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.*;

import static java.lang.String.format;

/**
 *
 */
public class Topics {
    @JsonArgument(help = "Path to topic file")
    File path;

    @JsonArgument(help = "Format of the topics")
    String $format;

    @JsonArgument(help = "The ID of the topics")
    String id;

    public QuerySet getQuerySet() throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
        switch ($format) {
            case "trec":
                try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                    return TRECTopic.readTopics(reader, false);
                }
            case "trec.web.2009":
                try (FileInputStream is = new FileInputStream(path)) {
                    return TRECWebTopic.readTopics(is);
                }
            default:
                throw new RuntimeException(format("Cannot handle topics of type %s", $format));
        }
    }

    static public Topics fromJSON(String json) {
        return Utils.get(json, Topics.class);
    }
}

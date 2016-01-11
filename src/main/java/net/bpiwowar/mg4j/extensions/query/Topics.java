package net.bpiwowar.mg4j.extensions.query;

import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.trec.TRECTopic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

    public QuerySet getQuerySet() throws IOException {
        switch ($format) {
            case "trec":
                try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                    return TRECTopic.readTopics(reader, false);
                }
            default:
                throw new RuntimeException(format("Cannot handle topics of type %s", $format));
        }

    }
}

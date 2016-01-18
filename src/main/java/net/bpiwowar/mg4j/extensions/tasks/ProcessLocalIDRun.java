package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.bpiwowar.mg4j.extensions.adhoc.AdhocRun;
import net.bpiwowar.mg4j.extensions.adhoc.Run;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.trec.IdentifiableCollection;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.JsonPath;
import net.bpiwowar.xpm.manager.tasks.ProgressListener;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@TaskDescription(id = "mg4j:process-local-run",
        output = "irc:run",
        description = "Transform documents with a MG4J ID to the real document identifier (URI)",
        registry = Registry.class)
public class ProcessLocalIDRun extends AbstractTask {
    final private static Logger LOGGER = LoggerFactory.getLogger(ProcessLocalIDRun.class);

    @JsonArgument()
    Run run;

    @JsonPath(copy = "path")
    File outFile;

    @JsonArgument(required = true)
    IndexedCollection index;

    @Override
    public JsonElement execute(JsonObject r, ProgressListener progress) throws Throwable {
        final IdentifiableCollection collection = (IdentifiableCollection) index.getCollection().get();

        final AdhocRun adhocRun = run.load();
        for (List<AdhocRun.ScoreInfo> scoreInfos : adhocRun.runs().values()) {
            for (AdhocRun.ScoreInfo scoreInfo : scoreInfos) {
                scoreInfo.docno = collection.getDocumentURI(Long.parseLong(scoreInfo.docno));
            }
        }

        adhocRun.save(outFile, AdhocRun.RunType.TREC);
        return JsonNull.INSTANCE;
    }

}

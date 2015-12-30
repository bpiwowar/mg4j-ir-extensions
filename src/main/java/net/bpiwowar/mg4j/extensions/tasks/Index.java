package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.DocumentSequence;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.tool.IndexBuilder;
import it.unimi.di.big.mg4j.tool.Scan;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.net.experimaestro.tasks.Path;

import java.io.File;

/**
 * Wrapper for Index in MG4J
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(
        id = "mg4j:index",
        output = "mg4j:index",
        registry = Registry.class,
        description = "Index a series of documents"
)
public class Index extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(Index.class);

    @JsonArgument(name = "collection", type="mg4j:collection", help = "MG4J collection to index", required = true)
    Collection collection;

    @JsonArgument(name = "term_processor", type = "mg4j:text-toolchain", help = "The term processor used to process documents (default: no processor)")
    TermProcessor termProcessor = NullTermProcessor.getInstance();

    @JsonArgument(name = "$batch_size", help = "Maximum number of documents per batch")
    int documentsPerBatch = Scan.DEFAULT_BATCH_SIZE;

    @JsonArgument(name = "$temporary_directory", help = "Temporary directory for batch files")
    File tmpDirectory;

    @Path(copy = "path")
    File index;

    @Override
    public JsonElement execute(JsonObject r) throws Throwable {
        // Retrieve the collection
        DocumentSequence documentSequence = Scan.getSequence(collection.path.getAbsolutePath(),
                IdentityDocumentFactory.class, new String[]{},
                Scan.DEFAULT_DELIMITER, LOGGER);

        // Configure and start the indexation
        LOGGER.info(String.format("Term processor class is %s", termProcessor.getClass()));
        IndexBuilder indexBuilder = new IndexBuilder(index.getAbsolutePath(), documentSequence);
        indexBuilder.termProcessor(termProcessor)
                .documentsPerBatch(documentsPerBatch);

        if (tmpDirectory != null)
            indexBuilder.batchDirName(tmpDirectory.getAbsolutePath());

        indexBuilder.run();

        return JsonNull.INSTANCE;
    }
}

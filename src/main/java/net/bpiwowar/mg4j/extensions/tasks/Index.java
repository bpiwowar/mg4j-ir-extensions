package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.argparser.handlers.XStreamHandler;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import it.unimi.di.big.mg4j.document.DocumentSequence;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.tool.IndexBuilder;
import it.unimi.di.big.mg4j.tool.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Wrapper for Index in MG4J
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(name = "index", project = {"ir", "mg4j"}, description = "Index a series of documents")
public class Index extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(Index.class);

    @Argument(name = "index-dir", help = "Index directory", checkers = IOChecker.ValidDirectory.class, required = true)
    File directory;

    @Argument(name = "index-basename", help = "Index basename", required = true)
    String basename;

    @Argument(name = "collection-sequence", help = "Sequence to index", required = true)
    String sequence;

    @Argument(name = "documents-per-batch", help = "Maximum number of documents per batch", group = "Options")
    int documentsPerBatch = Scan.DEFAULT_BATCH_SIZE;

    @Argument(name = "tmp", help = "Temporary directory for batch files", checkers = IOChecker.ValidDirectory.class, group = "Options")
    File tmpDirectory;

    @Argument(name = "term-processor", help = "An XML serialized form of the term processor (default: no processor)", handler = XStreamHandler.class, group = "Options")
    TermProcessor termProcessor = NullTermProcessor.getInstance();

    @Override
    public int execute() throws Throwable {
        DocumentSequence documentSequence;
        documentSequence = Scan.getSequence(sequence,
                IdentityDocumentFactory.class, new String[] {},
                Scan.DEFAULT_DELIMITER, LOGGER);

        LOGGER.info(String.format("Term processor class is %s", termProcessor.getClass()));
        IndexBuilder indexBuilder = new IndexBuilder(new File(directory,basename).getAbsolutePath(), documentSequence);
        indexBuilder.termProcessor(termProcessor).documentsPerBatch(
                documentsPerBatch);

        if (tmpDirectory != null)
            indexBuilder.batchDirName(tmpDirectory.getAbsolutePath());

        indexBuilder.run();
        return 0;
    }
}

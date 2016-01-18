package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.bpiwowar.mg4j.extensions.trec.IdentifiableCollection;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Represents a run
 */
public class Run {
    /**
     * The path
     */
    @JsonArgument
    public File path;

    public AdhocRun load() throws IOException {
        return AdhocRun.load(path, AdhocRun.RunType.TREC);
    }

}

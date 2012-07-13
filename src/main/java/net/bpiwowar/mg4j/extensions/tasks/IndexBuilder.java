package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;

/**
 * Wrapper for IndexBuilder in MG4J
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(name = "index-builder", project = {"mg4j", "extensions"})
public class IndexBuilder extends AbstractTask {
    // Arguments to be given to index builder
    private String[] args;

    @Override
    public String[] processTrailingArguments(String[] args) throws Exception {
        this.args = args;
        return null;
    }

    @Override
    public int execute() throws Throwable {
        // Just run
        it.unimi.di.big.mg4j.tool.IndexBuilder.main(args);
        return 0;
    }
}

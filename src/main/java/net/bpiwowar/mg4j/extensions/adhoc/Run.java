package net.bpiwowar.mg4j.extensions.adhoc;

import net.bpiwowar.xpm.manager.tasks.JsonArgument;

import java.io.File;

/**
 * Represents a run
 */
public class Run {
    /** The path */
    @JsonArgument
    public File path;
}

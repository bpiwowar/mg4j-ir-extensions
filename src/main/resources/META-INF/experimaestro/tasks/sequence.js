/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

 /**
 * Builds a sequence object
 */
tasks("mg4j:collection") = {
	output: "mg4j:collection",

	description: "<p>Creates a <b>sequence</b> from a list of files, together with the adapted document builder</p>",
    
    inputs: {
        documents: { json: "irc:documents", copy:true, help: "<p>A document object as produced by the <code>get-task</code>" +
            "command of <a href=\"https://github.com/bpiwowar/ircollections\">IR collections</a>.</p>"}
    },
	
	run: function(p, r) {
		// Initialisation
		var log = logger.create("sequence");
		var outdir = $(p.outdir);
        var dir = this.unique_directory("collection", p.documents);

		r.path = dir.path("sequence");
		
		var command = get_command(["build-collection", "--out", r.path]);

		log.debug("Creating sequence in [%s] with command %s", r.path, command.toSource());
		log.debug("Documents: %s", p.documents.toSource());

		r.$$resource = xpm.command_line_job(r.path, command, { stdin: p.documents.toSource() });
		
		return r;
	}
	
}

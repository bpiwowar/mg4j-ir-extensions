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
var task_sequence = {
	id: qname(mg4jext, "sequence"),
	output: qname(mg4jext, "sequence"),

	description: 
		<><p>Creates a <b>sequence</b> from a list of files, 
		together with the adapted document builder
		</p></>,
    
    inputs: <inputs xmlns:irc={ns_irc}>
			<value id="outdir" value-type="xp:directory" help="The main directory for output"/>
			
			<xml id="documents" type="irc:documents">
				<p>A document object as produced by the <code>get-task</code> command of 
				<a href="https://github.com/bpiwowar/ircollections">IR collections</a>.</p>
			</xml>
		</inputs>,
		
	
	run: function(inputs) {
		// Initialisation
		var log = logger.create("sequence");
		var outdir = xpm.file(inputs.outdir.@value);
		var id = inputs.documents.@id;

		// Get the collection directory
		var colldir = outdir.path(id);
		colldir.mkdirs();

		var sequence = colldir.path("collection");
		
		var command = get_command(["build-collection", "--out", path(sequence)]);

		log.debug("Creating sequence in [%s] with command %s", sequence, command.toSource());
		log.debug("Documents: %s", inputs.documents.toXMLString());
		var rsrc = xpm.command_line_job(sequence, command, {
			stdin: inputs.documents.toXMLString()
		});
		
		var r =
			<sequence xmlns:xp={xp.uri} xmlns:mg4jext={mg4jext.uri} xmlns={mg4jext.uri} xp:resource={rsrc}>
			  <xp:path mg4jext:arg="collection-sequence">{sequence}</xp:path>
			  {inputs.documents}
			</sequence>;
		
		log.debug(r.toSource());
		return r;
	}
	
}

xpm.add_task_factory(task_sequence);

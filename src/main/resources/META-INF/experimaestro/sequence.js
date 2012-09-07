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
	id: qname(ns_mg4jext, "sequence"),
	
	description: 
	<><p>Creates a <b>sequence</b> from a list of files, 
	together with the adapted document builder
	</p></>,
    
    inputs: <inputs xmlns:irc={ns_irc}>
			<param id="outdir" type="xp:directory" help="The main directory for output"/>
			
			<param id="documents" type="irc:task" required="true">
				<p>A document object as produced by the <code>get-task</code> command of 
				<a href="https://github.com/bpiwowar/ircollections">IR collections</a>.</p>
			</param>
		</inputs>,
		
	outputs: <outputs xmlns:mg4jext={ns_mg4jext}>
			<value id="sequence" type="mg4jext:sequence"/>
		</outputs>,
	
	run: function(inputs) {
		// Initialisation
		var outdir = new java.io.File(inputs.outdir.@xp::value);
		var id = inputs.documents.ns_irc::documents.@id;

		// Get the collection directory
		var colldir = xpm.filepath(outdir, id);
		colldir.mkdirs();

		var sequence = xpm.filepath(colldir, "collection").getAbsolutePath();
		
		var command = get_command(["build-collection", "--out", sequence]);

		xpm.log("Creating sequence in [%s] with command [%s]", sequence, command.join(" "));
		scheduler.command_line_job(sequence, command, {
			stdin: inputs.documents.toString()
		});
		
		var r = <xpm:outputs xmlns:xpm={xpm.ns()}>
			<sequence xmlns:mg4jext={ns_mg4jext.uri} xmlns={ns_mg4jext.uri} xpm:resource={sequence}>
			  <path mg4jext:arg="collection-sequence">{sequence}</path>
			  {inputs.documents}
			</sequence>
		</xpm:outputs>;
		
		return r;
	}
	
}

xpm.addTaskFactory(task_sequence);

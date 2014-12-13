tasks("mg4j:index") = {
	description: "<p>Index the collection</p>",
	
    output: "mg4j:index",
	inputs: {
		basename: { value: "xp:string", default: "index", help="The base name of the index file"},
		sequence: { json: "mg4j:collection", help="The collection of files to index", copy: true },
		term_processor: { json: "mg4j:term-processor", copy: true }
	},
		

	run: function(p, r) {	
	   // --- Initialisations
	   var log = logger.create("index");	   
	   var seq_file = $(p.sequence.path);
	   var index_dir = this.get_unique_directory(seq_file, "index", r);

	   var commandId = index_dir.path(basename);

	   // Build the command
	   var command = get_command(["index"]).concat(
	   );
	   r.$$resource = xpm.command_line_job(commandId, command, { "lock": [$$(p.sequence).lock()] });

	   return r;
	   
	}
	
};

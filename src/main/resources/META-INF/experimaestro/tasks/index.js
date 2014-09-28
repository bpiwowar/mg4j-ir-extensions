var task_index = {
	id: qname(mg4jext, "index"),
	
	description: <><p>Index the collection</p></>,
	
    		// Used when parameters are fixed
	inputs: <inputs xmlns:mg4j={mg4jext.uri} xmlns={xp.uri}>
			<value id="indexdir" value-type="xp:directory" help="The main directory for output (by default, same as sequence)" optional="true"/>
			
			<value id="basename" value-type="xs:string" help="The name of the index" default="index"/> 
			
			<xml id="sequence" type="mg4j:sequence"></xml>
			<xml id="term-processor" type="mg4j:term-processor" optional="true"/>
		</inputs>,
		
    output: qname(mg4j, "index"),

	run: function(inputs) {	
	
	   // --- Initialisations
	   var log = logger.create("index");	   

	   // Get the sequence file
	   var seq_file = xpm.file(inputs.sequence.xp::path);
	   log.debug("seq_file [%s]", seq_file);
	   var index_dir = inputs.indexdir == undefined ? seq_file.get_parent().path("index") : inputs.indexdir.@value;
	   var basename = inputs.basename.@value;
	   var commandId = index_dir + "/" + basename;

	   var term_processor = inputs["term-processor"];
	   
	   log.debug("Index dir is [%s]", index_dir);

	   // Prepare output and resources
	   var output =
		   <mg4j:index xmlns:mg4j={mg4j.uri} xmlns:xp={xp.uri}>
			   <mg4j:directory mg4j:arg="index-dir"><xp:path>{index_dir}</xp:path></mg4j:directory>
			   <mg4j:name mg4j:arg="index-basename">{basename}</mg4j:name>

			   <!-- Dependent inputs -->
			   {inputs.sequence}
			   {inputs.term_processor}
		   </mg4j:index>;

	   var resources = get_resources(inputs.sequence, "READ_ACCESS");

	   // Build the command
   	   var command  = [ "index" ].concat(build_args("", output));

	   command = get_command(command);
	   log.debug("Command is %s", command.toSource());

	   // Make the directories
	   index_dir.mkdirs();
	   var rsrc = xpm.command_line_job(commandId, command, { "lock": resources });
	   output.@xp::resource = rsrc.toString();

	   log.debug("Output is %s", output.toSource());

	   return output;
	   
	}
	
};

xpm.add_task_factory(task_index);

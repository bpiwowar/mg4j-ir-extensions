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
	   

	   // Get the sequence file
	   var seq_file = inputs.sequence.mg4j::path;
	   var index_dir = inputs.indexdir == undefined ? xpm.filepath(new java.io.File(seq_file).getParentFile()) : index_dir = inputs.indexdir.@xp::value;
	   var basename = inputs.basename.@xp::value;
	   var commandId = index_dir + "/" + basename;
	   
	   // Prepare the output
	   var output =
		   <mg4j:index xmlns:mg4j={mg4j.uri} xmlns:xp={xp.uri} xp:resource={commandId}>
			   <mg4j:directory mg4j:arg="index-dir">{index_dir}</mg4j:directory>
			   <mg4j:name mg4j:arg="index-basename">{basename}</mg4j:name>

			   <!-- Dependent inputs -->
			   {inputs.sequence}
			   {inputs["term-processor"]}
		   </mg4j:index>;

	   
	   

		// Lock the resources	   
	   var resources = get_resources(inputs.sequence, "READ_ACCESS");

	   // Build the command
   	   var command  = [ "index" ].concat(build_args("", output));
	   if (inputs["term-processor"] != undefined)
	      command = command.concat("--term-processor", inputs["term-processor"].mg4j::path.text());

	   command = get_command(command);

	   // Make the directories
	   index_dir.mkdirs();
	   scheduler.command_line_job(commandId, command, { "lock": resources });

	   return output;
	   
	}
	
};

xpm.add_task_factory(task_index);

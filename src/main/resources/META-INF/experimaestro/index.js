var task_index = {
	module: module_adhoc.id,	
	id: xpm.qName(qirns(), "index"),
	
	description: <><p>Index the collection</p></>,
	
    		// Used when parameters are fixed
	inputs: <inputs xmlns:qia={qia.uri}>
			<input id="indexdir" type="xp:directory" help="The main directory for output (by default, same as sequence)"/>
			
			<input id="basename" type="xs:string" help="The name of the index" default="index"/> 
			
			<input id="term-processor" type="qia:term-processor"/>
			<input id="sequence" type="qia:sequence"/>
		</inputs>,
		
    outputs: <>
			<value id="index" type={qirns("index")} view="File" version="1" lock="read-only"/>
		</>,
                
	run: function(inputs) {	
	
	   // --- Initialisations
	   
	   // Get the sequence file
	   var seq_file = inputs.sequence.qia::path;
	   
	   // Set the index directory
	   if (inputs.indexdir == undefined) {
		   index_dir = xpm.filepath(new java.io.File(seq_file).getParentFile(), "index");
	   } else 
		   index_dir = inputs.indexdir.@xp::value;
	   
	   
	   // Prepare the output
	   var output = <xp:outputs xmlns:xp={xp.uri}>
		   <qia:index xmlns:qia={qirns()}  xp:resource={index_dir}>
			   <qia:directory qia:arg="index-dir">{index_dir}</qia:directory>
			   <qia:name qia:arg="index-basename">{inputs.basename.@xp::value}</qia:name>
			   <!-- Dependent inputs -->
			   {inputs.sequence}
			   {inputs["term-processor"]}
		   </qia:index>
	   </xp:outputs>;
	   
	   
	   // Make the directories
	   index_dir.mkdirs();

		// Lock the resources	   
	   var resources = getResources(inputs.sequence, "READ_ACCESS");

	   // Schedule the task

   	   var command  = qiaJarCmd.concat([ "index", 
		   build_args("", output), 
		   "--term-processor", inputs["term-processor"].qia::path.text(),
		   "--basename", inputs.basename.@xp::value]);

       xpm.log("Command is [%s]", command.toSource());
	   

	   scheduler.addCommandLineJob(index_dir, command, resources);

	   return output;
	   
	}
	
};

xpm.addTaskFactory(task_index);


// Declares an alternative
xpm.declare_alternative(qname(mg4j, "adhoc.model"));

var task_adhoc_run = {
	id: qname(mg4j, "adhoc.run"),

	description: <xp:documentation xmlns:xp={xp.uri} xmlns:irc={ns_irc.uri}><p>Run a configured model on an adhoc task.
	 This can be evaluated by <xp:task-link type="irc:evaluation">the IRC evaluation task</xp:task-link>.</p>
	 </xp:documentation>,

	inputs: <inputs xmlns={xp.uri} xmlns:irc={irc.uri} xmlns:mg4j={mg4j.uri}>
		<value id="run-path" type="xp:file" help="The file where results should be stored"/>

		<value id="top-k" type="xs:integer" default="1500" help="The maximum number of documents to retrieve"/>

		<xml id="index" type="mg4j:index" help="The MG4J index"/>
		<xml id="topics" type="irc:topics" help="The topics"/>

		<alternative id="model" type="mg4j:adhoc.model" help="The model to run"/>
	</inputs>,
		
	run: function(inputs) {			
		var path = inputs["run-path"].@xp::value;
		xpm.log("Model [%s]", inputs.model.toXMLString());
		var command = [ "adhoc" ].concat( 
			build_args("",inputs.index),
			["--collection-sequence", inputs.index.mg4j::sequence.mg4j::path, 
			"--top-k", inputs["top-k"].@xp::value, 
			"--run-id", inputs.model.@id]);
			
	   var resources = get_resources(inputs.index, "READ_ACCESS");
	   xpm.log(resources);

	   	// FIXME: should use FileObject
	   	xpm.filepath(path).getParentFile().mkdirs();

	   	xpm.log(command.toSource())
		scheduler.command_line_job(			
			path,
			get_command(command),
			{ 
				stdin: (<task>{inputs.topics}{inputs.model}</task>).toXMLString(),
				sdout: path,
				lock: resources
			}
		);
		
		return <outputs>
			<adhoc-run xmlns={mg4j.uri} xmlns:xp={xp.uri} xp:resource={path}>
				<path>{path}</path>
				{inputs.model}
				{inputs.topics}
				{inputs["top-k"]}
				{inputs.index}
			</adhoc-run>
		</outputs>;
	}
};


xpm.add_task_factory(task_adhoc_run);


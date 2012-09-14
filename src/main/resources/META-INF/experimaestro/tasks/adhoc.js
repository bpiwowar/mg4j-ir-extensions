
// Declares an alternative
xpm.declare_alternative(qname(mg4j, "adhoc.model"));

var task_adhoc_run = {
	id: qname(mg4j, "adhoc-run"),

	description: <xp:documentation xmlns:xp={xp.uri} xmlns:irc={ns_irc.uri}><p>Run a configured model on an adhoc task.
	 This can be evaluated by <xp:task-link type="irc:evaluation">the IRC evaluation task</xp:task-link>.</p></xp:documentation>,

	inputs: <inputs xmlns={xp.uri} xmlns:mg4j={mg4j.uri}>
		<value id="top-k" type="xs:integer" default="1500" help="The maximum number of documents to retrieve"/>
		<xml id="model" type="mg4j:adhoc-model" help="The model to run"/>
		<xml id="index" type="mg4j:index" help="The MG4J index"/>
		<xml id="topics" type="mg4j:topics" help="The topics"/>
		<value id="run-path" type="xp:file" help="The file where results should be stored"/>
	</inputs>,
	
	outputs: <></>,
	
	run: function(inputs) {						
		var path = inputs["run-path"].@xp::value;
		var command = [ "adhoc", 
			getIndexArgs(inputs.index), 
			"--collection-sequence", inputs.index.mg4j::sequence.mg4j::pathid, 
			"--top-k", inputs["top-k"], 
			"--run-id", inputs.model.@id,
			"--model", inputs.model.mg4j::path, 
			"--topics", inputs.topics.mg4j::path, 
			">", path];
			
		scheduler.addCommandLineJob(path, commmand, getResources(inputs.index, "READ_ACCESS") );
		
		return <outputs>
			<adhoc-run xmlns={mg4j.uri} xmlmns:xp={xp.uri} xp:resource={path}>
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


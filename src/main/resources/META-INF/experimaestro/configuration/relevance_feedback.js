/**
 * Relevance feedback configuration
 */
xpm.add_task_factory({
	id: qname(mg4j, "relevance-feedback"),
	
	inputs: <inputs>
		<xml id="method" help="The method used for determining the relevance of documents"/>
		<value id="top-k" help="Defines how many top ranked documents should be used"/>
	</inputs>,

	outputs: <outputs/>,
	
	run: function(inputs) {
		return <outputs>
			<pathid>{format("rf%s-k%s-t%s", rfMethod, rfTopK, Threshold)}</pathid>
		</outputs>;
	}
});

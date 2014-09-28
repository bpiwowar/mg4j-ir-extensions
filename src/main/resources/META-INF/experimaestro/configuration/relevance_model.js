xpm.add_task_factory({
	id: qname(mg4j, "relevance-model"),
	alternative: true,
	output: qname(mg4j, "adhoc.model"),

	inputs: 
		<inputs xmlns:mg4j={mg4j.uri}>
			<alternative id="source" type="mg4j:relevance-feedback.model" help="The relevance feedback model"/>

			<input id="lambda" type="xs:real" default="0.6" help="Smoothing parameter (eq. 15)"
			<input id="method" type="xs:enum">
				<choice value="iid" help="i.i.d. sampling"/>
				<choice value="conditional" help="Conditional sampling (eq. 9)"/>				
			</input>
		</inputs>,

	run: function(inputs) {	
		var k1 = inputs.k1.@value;
		var b = inputs.b.@value;

		inputs.model 
		r=<adhoc.model xmlns={mg4j.uri} id={"rm_" + inputs.source.@id }> 
			<relevance-model>
				{inputs.source}
			</relevance-model>
		</adhoc.model>;
		return r;
	}
});
xpm.add_task_factory({
	id: qname(mg4j, "bm25"),
	alternative: true,
	output: qname(mg4j, "adhoc.model"),

	inputs: 
		<inputs>
			<value id="k1" default="1.2" type="xs:float"/>
			<value id="b" default="0.75" type="xs:float"/>
		</inputs>,

	run: function(inputs) {	
		var k1 = inputs.k1.@xp::value;
		var b = inputs.b.@xp::value;
		r=<adhoc.model xmlns={mg4j.uri} id={"bm25-k1_" + k1 + "-b_" + b }> 
			<bm25 k1={k1} b={b}/>
		</adhoc.model>;
		return r;
	}
});
var composed_prepare_collection = {
	id: qname(mg4j, "prepare-collection"),
	
	description: <>
		<p>Prepare a collection:
		<ol>
		<li>parse document boundaries</li>
		<li>build the inverted index</li>
		<li>build the direct index</li>
		</ol>)
		</p>
	</>,
	
	inputs: 
		<inputs xmlns={xp.uri} xmlns:irc={ns_irc.uri} xmlns:mg4j={mg4j.uri}>
			<value id="outdir" type="xp:directory" help="The main directory for output"/>
			<xml id="term-processor" type="mg4j:term-processor" optional="true"/>

			<task id="collection" ref="irc:get-task" type="irc:task" required="true"/>

			<task id="sequence" ref="mg4j:sequence">
				<connect from="outdir" path="." to="outdir"/>
				<connect from="collection" path="irc:documents" to="documents"/> 
			</task>
			
			<task id="index" ref="mg4j:index">
				<connect from="sequence" path="." to="sequence"/>
				<connect from="term-processor" path="." to="term-processor"/>
			</task>
		</inputs>,
		
	
};
xpm.add_task_factory(composed_prepare_collection);

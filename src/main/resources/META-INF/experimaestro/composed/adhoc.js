/**
 * Ad-hoc run
 */


xpm.add_task_factory({
	id: qname(mg4j, "adhoc"),

	description: <>
		<p>Prepare a collection (sequence, index) and run an ad-hoc experiment</p>
	</>,
	
	inputs: 
		<inputs xmlns={xpm.ns()} xmlns:irc={ircns()} xmlns:qia={qia.uri}>
			<task id="prepare" type="qia:prepare-collection" named="false"/>
			
			<task id="topics" type="qia:get-topics">
			   <connect from="prepare.index" path="xp:parentPath(qia:index/qia:path)" to="outdir"/>
			   <connect from="prepare.collection" path="irc:task/irc:topics" to="topics"/>
			</task>

            <!-- Description of the model we use -->
			<task id="model" type="qia:adhoc-model"/>
						
			<task id="run" type="qia:adhoc-run">
			   <connect from="prepare.index" path="." to="index"/>
        	   <connect from="model" path="qia:adhoc-model" to="model"/>
			   <connect from="prepare.collection" path="qia:topics" to="topics"/>
			</task>

			<task id="evaluate" type="irc:evaluate">
			   <connect from="run" path="qia:run/qia:path" to="run"/>
			   <connect from="prepare" path="irc:qrels" to="qrels"/>
			   <connect from="run" path="xp:joinPaths(xp:parentPath(qia:run/qia:path), 'results.dat')" to="out"/>
			</task>
		</inputs>,
		
	outputs: <></>,
});


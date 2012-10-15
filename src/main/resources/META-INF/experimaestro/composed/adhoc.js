/**
 * Ad-hoc run
 */


xpm.add_task_factory({
	id: qname(mg4j, "adhoc"),

	description: <>
		<p>Prepare a collection (sequence, index) and run an ad-hoc experiment</p>
	</>,
	
	inputs: 
		<inputs xmlns={xp.uri} xmlns:irc={irc.uri} xmlns:mg4j={mg4j.uri}>
			<task id="prepare" ref="mg4j:prepare-collection" merge="true"/>
			
            <!-- Description of the model we use -->
			<alternative id="model" type="mg4j:adhoc.model"/>
						
			<task id="run" ref="mg4j:adhoc.run">
        	   <connect from="model" path="." to="model"/>
			   <connect from="prepare.index" path="." to="index"/>
			   <connect from="prepare.collection" path="irc:topics" to="topics"/>

			   <!-- Output will be in outdir/<id task>/<id model> -->
				<connect to="run-path" xmlns:irc={irc.uri}>
					<from var="o" ref="prepare.outdir"/>
					<from var="c" ref="prepare.collection"/>
					<from var="m" ref="model"/>
					<xquery>
						concat($o/@xp:value, "/","adhoc",
							  "/",$c//irc:documents/@id,
							  "/",$c//irc:topics/@id,
							  "/",$m/@id)
					</xquery>
			   </connect>
			</task>

			<!--
			 <task id="evaluate" ref="irc:evaluate">
			    <connect from="run" path="mg4j:run/mg4j:path" to="run"/>
			    <connect from="prepare" path="irc:qrels" to="qrels"/>
			    <connect from="run" path="xp:joinPaths(xp:parentPath(mg4j:run/mg4j:path), 'results.dat')" to="out"/>
			 </task>
			-->
		</inputs>,
		
});

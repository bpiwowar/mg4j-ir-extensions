/**
 * Builds a sequence object
 */
var task_sequence = {
	module: module_adhoc.id,	
	id: xpm.qName(qirns(), "sequence"),
	
	description: 
	<><p>Creates a <b>sequence</b> from a list of files, 
	together with the adapted document builder
	</p></>,
    
    inputs: <inputs xmlns:irc={irc.uri}>
			<param id="outdir" type="xp:directory" help="The main directory for output"/>
			
			<param id="documents" type="irc:task">
				<p>A document object as produced by the <code>get-task</code> command of 
				<a href="http://ircollections.sourceforge.net">IR collections</a>.</p>
			</param>
		</inputs>,
		
	outputs: <outputs xmlns:qia={qia.uri}>
			<value id="sequence" type="qia:sequence"/>
		</outputs>,
	
	run: function(inputs) {
		// Initialisation
		var outdir = new java.io.File(inputs.outdir.@xp::value);

		xpm.log("Outdir is [%s]", outdir);
		// Get the collection directory
		var colldir = xpm.filepath(outdir, inputs.documents.@id);
		var sequence = format("%s/collection", colldir);

		// Constructs the command line
		var dtype = inputs.documents.@type.toString();
		switch(dtype) {
			case "trec":
				command = ["trec-sequence", "--property", "encoding=ISO-8859-1"];
				break;
			default: 
				throw format("Unknown collection type %s", dtype);
		}
		
		var seqfile = format("%s.seq", sequence);
		var command = [].concat("cat", inputs.documents.@path.toString(), "|", 
			qiaJarCmd, command, "--collection", seqfile);
		

		colldir.mkdirs();
		scheduler.addCommandLineJob(sequence, command, []);
		
		var r = <xpm:outputs xmlns:xpm={xpm.ns()}>
			<sequence xmlns:qia={qirns()} xmlns={qirns()} xpm:resource={sequence}>
			  <path qia:arg="collection-sequence">{seqfile}</path>
			  {inputs.documents}
			</sequence>
		</xpm:outputs>;
		
		return r;
	}
	
}

xpm.addTaskFactory(task_sequence);

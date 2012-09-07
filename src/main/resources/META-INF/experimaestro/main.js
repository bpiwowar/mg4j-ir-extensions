
// The namespaces
var ns_mg4jext = new Namespace("net.bpiwowar.mg4j-ir-extensions");
var ns_irc = new Namespace("http://ircollections.sourceforge.net");

// Executable
var script_path = xpm.getScriptPath();

// FIXME: should not be using the 
var basedir = java.io.File(script_path).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
var pomfile = java.io.File(basedir, "pom.xml")
xpm.log(pomfile);


  
var get_command = function(args) {
	return ["mvn", "-q", "-f", pomfile, "compile", "exec:java", "-Dexec.mainClass=bpiwowar.experiments.Run", "-Dexec.args=" + args.join(" ")];	
} 

// Some useful functions
var format = java.lang.String.format;

include("sequence.js");
// include("index.js");

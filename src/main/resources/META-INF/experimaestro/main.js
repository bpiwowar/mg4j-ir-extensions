

// The namespaces
var mg4jext = new Namespace("net.bpiwowar.mg4j-ir-extensions");
var mg4j = mg4jext;
var ns_irc = new Namespace("http://ircollections.sourceforge.net");
var irc = ns_irc;

xpm.set_property("namespace", mg4jext);

// Check that IRC has been retrieved
// xpm.check_repository(ns_irc, );

// Executable
var script_path = xpm.get_script_path();

// FIXME: should not be using the file but a FileObject (or a proxy to it)
var basedir = java.io.File(script_path).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
var pomfile = java.io.File(basedir, "pom.xml")

/// Start mg4j-ir-ext jar file with given arguments  
var get_command = function(args) {
	// Case where we start with maven
	return ["mvn", "-q", "-f", pomfile, "compile", "exec:java", "-Dexec.mainClass=bpiwowar.experiments.Run", "-Dexec.args=" + args.join(" ")];	
} 

// Some useful functions
var format = java.lang.String.format;


function get_resources(xml, mode) {
        a = [];
        for each(var r in xml..@xp::resource) {
                a.push([r.text(), mode]);
        }
        return a;
}

/**
 * Builds a command line from an XML fragment
 * 
 * Picks up all the elements with a @mg4j::arg attribute
 */
var build_args = function(prefix, a) {
        array = [];
        //xpm.log("ARGUMENTS [%s]", a);
        for each(var x in a..*) 
                if (x.nodeKind() == "element") {
                        var arg = x.@mg4j::arg;
                        if (arg.length() > 0) {
                            var value = x.@xp::value;
                            if (value.length() == 0) value = x.text();
                            //xpm.log("ARG [%s]: %s=%s", x.toSource(), arg, value)
                                array = array.concat(prefix + (arg != "" ? (prefix ? "-" : "--") + arg : arg), value);
                        }
                }
        return array;
};


// --- Include the tasks

include("tasks/sequence.js");
include("tasks/index.js");
include("tasks/adhoc.js");

include("configuration/relevance_feedback.js");
include("configuration/bm25.js");

include("composed/prepare_collection.js");
include("composed/adhoc.js");

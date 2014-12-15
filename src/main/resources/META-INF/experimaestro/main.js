
// The namespaces
var mg4jext = new Namespace("net.bpiwowar.mg4j-ir-extensions");
var mg4j = mg4jext;
var ns_irc = new Namespace("http://ircollections.sourceforge.net");
var irc = ns_irc;

var logger = xpm.logger("net.bpiwowar.mg4jext");

xpm.set_property("namespace", mg4jext);

// Check that IRC has been retrieved
// xpm.check_repository(ns_irc, );

// Executable
var script_path = xpm.get_script_path();
var basedir = xpm.get_script_file().get_ancestor(6);
var pomfile = basedir.path("pom.xml")

/// Start mg4j-ir-ext jar file with given arguments  
var get_command = function(args) {
    // Case where we start with maven
    // xpm.log(a.toSource());
    return ["mvn-argj-exec", "--pom", path(pomfile)].concat(args);
} 


// Some useful functions
var format = java.lang.String.format;



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
                            // logger.debug("ARG [%s]: %s=%s", x.toSource(), arg, value);
                            // If xp:path, returns it as it is
                            if (x.localName() == "path" && x.namespace() == xp.uri)
                                array = array.concat(prefix + (arg != "" ? (prefix ? "-" : "--") + arg : arg), x);
                            else {
                                var value = x.@value;
                                if (value.length() == 0) value = x.*;
                                array = array.concat(prefix + (arg != "" ? (prefix ? "-" : "--") + arg : arg), value);
                            }
                        }
                }
        return array;
};


// --- Include the tasks

include("tasks/sequence.js");
include("tasks/index.js");
//include("tasks/adhoc.js");

// include("configuration/relevance_feedback.js");
// include("configuration/bm25.js");


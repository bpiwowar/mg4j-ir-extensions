// --- HANDLING TOPICS

var trec_topic = {

};

var topics_handlers = {
    "trec": trec_topic,
};






// --- HANDLING TASKS

// --- Ad-hoc task

var adhoc =  {
    // Default model is BM25
    model: 1,

    // Retrieves 1500 documents by default
    topK: 1500

    // Run the task given by the
    run: function(xml) {

    }
};


// --- List of handlers

var taskHandlers = {
    "adhoc": adhoc,
};


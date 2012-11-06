/**
 * Configuration: relevance feedback 
 * 
 * This configures what feedback should be given to a retrieval algorithm
 */

alt_rf = qname(mg4j, "relevance-feedback.model");
xpm.declare_alternative(alt_rf);

// Pseudo-relevance feedback
xpm.add_task_factory({
	id: qname(mg4j, "relevance-feedback.pseudo"),
	alternative: true,
	output: alt_rf,

	inputs: <inputs>
		<value id="top-k" help="Defines how many top ranked documents should be considered as relevant"/>
	</inputs>,
});

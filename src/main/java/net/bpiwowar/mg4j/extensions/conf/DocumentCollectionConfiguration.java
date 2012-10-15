/**
 * 
 */
package net.bpiwowar.mg4j.extensions.conf;

import bpiwowar.argparser.Argument;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.tool.Scan;
import org.apache.log4j.Logger;

public class DocumentCollectionConfiguration {
	final static private Logger logger = Logger.getLogger(DocumentCollectionConfiguration.class);
	
	@Argument(name = "sequence", help = "The document collection sequence", required = true)
	String sequence;

	@Argument(name = "encoding", help = "Default encoding")
	String encoding = "UTF-8";

	public DocumentCollection init() throws Throwable {
		return (DocumentCollection) Scan.getSequence(sequence,
				IdentityDocumentFactory.class, new String[] { "encoding="
						+ encoding }, Scan.DEFAULT_DELIMITER, logger);
	}
}
/**
 * $Author:$
 * $Id:$
 * $Rev:$
 */

package net.bpiwowar.mg4j.extensions.utils;

import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.query.COQuery;
import net.bpiwowar.mg4j.extensions.query.Phrase;
import net.bpiwowar.mg4j.extensions.query.Query;
import net.bpiwowar.mg4j.extensions.query.Requirement;
import net.bpiwowar.mg4j.extensions.query.SimpleQuery;
import net.bpiwowar.mg4j.extensions.query.StringQuery;
import net.bpiwowar.mg4j.extensions.query.Term;
import net.bpiwowar.mg4j.extensions.query.Text;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Code previously found in MG4JScorer but needed elsewhere as well
 *
 * @author <a href="mailto:benjamin@bpiwowar.net">Benjamin Piwowarski</a>
 */
public class TermUtil {
    final static Logger logger = Logger.getLogger(TermUtil.class);

    static public void getPositiveTerms(
            Query query,
            Map<String, MutableInt> terms, TermProcessor processor,
            IndexedField index) {
        if (query instanceof COQuery)
            getPositiveTerms((COQuery) query, terms, processor, index);
        else if (query instanceof StringQuery)
            getPositiveTerms((StringQuery) query, terms, processor, index);
        else
            throw new NotImplementedException(String.format(
                    "Cannot handle query of class %s", query.getClass()));
    }

    static public void getPositiveTerms(StringQuery query,
                                        Map<String, MutableInt> termMap, TermProcessor processor,
                                        IndexedField index) {
        // Transform into a CO query
        COQuery coQuery = new COQuery();
        final Requirement req = new Requirement(new SimpleQuery(query.getQuery()));
        coQuery.add(req);

        // ... and then perform!
        getPositiveTerms(coQuery, termMap, processor, index);
    }

    static public void getPositiveTerms(COQuery coQuery,
                                        Map<String, MutableInt> terms, TermProcessor processor,
                                        IndexedField index) {
        for (Requirement req : coQuery.requirements)
            getPositiveTerms(req, terms, processor, index);
    }

    static private void getPositiveTerms(Requirement req,
                                         Map<String, MutableInt> terms, TermProcessor processor,
                                         IndexedField index) {
        for (Text text : req.terms)
            getPositiveTerms(text, terms, processor, index);
    }

    /**
     * Get query terms which are not negative
     *
     * @param text
     * @param terms
     */
    static private void getPositiveTerms(Text text,
                                         Map<String, MutableInt> terms, TermProcessor processor,
                                         IndexedField index) {
        // Skip negative terms
        if (text.isNegative())
            return;

        // Add the terms from the query
        if (text instanceof Term) {
            addTerm(((Term) text).word, terms, processor, index);
        } else if (text instanceof Phrase) {
            for (Term term : ((Phrase) text).terms)
                addTerm(term.word, terms, processor, index);
        } else
            throw new RuntimeException(String.format("Unknown text type: %s", text.getClass()));
    }

//	static public void getPositiveTerms(TopicOperator operator,
//			final Map<String, MutableInt> terms, final TermProcessor processor,
//			final IndexConfiguration index) {
//		operator.apply(new OperatorTransformer() {
//			@Override
//			public Operator transform(Operator parent, Operator operator) {
//				if (operator instanceof Projection
//						&& ((Projection) operator).isOrthogonal()) {
//					((Projection) operator).getOperator().applyToSuboperators(
//							this);
//				} else if (operator instanceof TermOperator) {
//					addTerm(((TermOperator) operator).getTerm(), terms,
//							processor, index);
//				} else {
//					operator.applyToSuboperators(this);
//				}
//				return operator;
//			}
//		});
//	}
//

    /**
     * Add a term if present in the index Try to split a word containing dash
     * "-" if not present
     *
     * @param text
     * @param terms
     */
    static private void addTerm(String text, Map<String, MutableInt> terms,
                                TermProcessor processor, IndexedField index) {
        MutableString word = new MutableString(text);
        // TODO: Annalina - when the query transformer is implemented, remove
        // the following and just keep the "update" line
        if (processor.processTerm(word)) {
            if (index.getTermId(word) != -1) {
                update(terms, word.toString());
                return;
            }
        }

        String[] array = text.split("-");
        if (array.length > 1) {
            logger
                    .info(String.format(
                            "Split the dash separted word %s since it is not in the index",
                            text));
            for (String s : array)
                addTerm(s, terms, processor, index);
        }
    }


    // --- Get sets of terms

    static public void update(Map<String, MutableInt> terms, String word) {
        MutableInt v = terms.get(word);
        if (v == null)
            terms.put(word, new MutableInt(1));
        else
            v.add(1);

    }
}

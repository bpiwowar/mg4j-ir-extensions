package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.ClassChooser;
import bpiwowar.argparser.handlers.ClassChooser.Choice;
import bpiwowar.argparser.handlers.XStreamHandler;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.query.Query;
import it.unimi.di.big.mg4j.query.QueryEngine;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.query.parser.SimpleParser;
import it.unimi.di.big.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.di.big.mg4j.search.score.Scorer;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntBigList;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.conf.IndexConfiguration;
import net.bpiwowar.mg4j.extensions.query.Topic;
import net.bpiwowar.mg4j.extensions.utils.Output;
import net.bpiwowar.mg4j.extensions.utils.TermUtil;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static java.lang.Math.log;
import static java.lang.Math.min;

public class MG4JScorer implements RetrievalModel {
	final static Logger logger = Logger.getLogger(MG4JScorer.class);

	@Argument(name = "term-processor", help = "The file that contains the term processor description", handler = XStreamHandler.class)
    TermProcessor processor = NullTermProcessor.getInstance();

	@Argument(name = "rf", prefix = "pseudo-rf", help = "How to handle pseudo-relevance feedback", handler = ClassChooser.class)
	PseudoRF pseudoRF;

	@Argument(name = "query-types", help = "The parts of the query that we use", required = true)
	private Set<String> queryTypes = new TreeSet<>();

    {
        queryTypes.add("title");
    }

	Scorer scorer;

	/** 
	 * The MG4J topic. Made public to be reusable later. Invoke process()
	 * before using this! 
	 */
	public String mg4jTopic;
	
	transient private IndexConfiguration index;

    private QueryEngine queryEngine;

//	private uk.ac.gla.dcs.renaissance.mg4j.QueryEngine queryEngine;

	@Override
	public void init(DocumentCollection collection, IndexConfiguration index) throws Exception {
		this.index = index;

		final Object2ReferenceLinkedOpenHashMap<String, Index> indexMap = new Object2ReferenceLinkedOpenHashMap<>(
				Hash.DEFAULT_INITIAL_SIZE, .5f);
		indexMap.put(index.field, index.index);
		final Reference2DoubleOpenHashMap<Index> index2Weight = new Reference2DoubleOpenHashMap<>();
		index2Weight.put(index.index, 1.);

		final SimpleParser queryParser = new SimpleParser(indexMap.keySet(),
				indexMap.firstKey(), null);

		final Reference2ReferenceMap<Index, Object> index2Parser = new Reference2ReferenceOpenHashMap<>();

        queryEngine = new QueryEngine(queryParser, new DocumentIteratorBuilderVisitor(indexMap,
                index2Parser, indexMap.get(indexMap.firstKey()),
                Query.MAX_STEMMING), indexMap);

		queryEngine.score(new Scorer[]{scorer}, new double[]{1});

		queryEngine.multiplex = true;
		queryEngine.setWeights(index2Weight);

	}


    @Override
	public void close() {
	}

	@Override
    public void process(Topic topic, ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results, int capacity, TaskTimer timer) throws Exception {

        // --- Get the MG4J topic

		TreeMap<String, MutableInt> terms = new TreeMap<>();

		for (String queryType : queryTypes) {
            net.bpiwowar.mg4j.extensions.query.Query query = topic.getTopicPart(queryType);
			// Take all the words from the topic and construct the query for
			// MG4J
			// TODO Annalina: if (query instanceof MG4JQuery) {} else...
			TermUtil.getPositiveTerms(query, terms, processor, index);
		}

		this.mg4jTopic = Output.toString(" | ", terms.entrySet(),
                weightedWordFormatter);

		logger.info(String.format("Topic %s", mg4jTopic));
		queryEngine.process(mg4jTopic, 0, pseudoRF == null ? capacity
                : pseudoRF.k, results);

		// --- Handling relevance feedback

		if (pseudoRF != null) {
			for (CharSequence term : pseudoRF.process(results))
				update(terms, term.toString());

			mg4jTopic = Output.toString(" | ", terms.entrySet(),
                    weightedWordFormatter);
			logger.info(String.format("After blind RF, topic is %s", mg4jTopic));

			results.clear();
			queryEngine.process(mg4jTopic, 0, capacity, results);
		}
	}

	/**
	 * A formatter that adds a MG4J weight to each word
	 */
	public static Output.Formatter<Map.Entry<String, MutableInt>> weightedWordFormatter = new Output.Formatter<Map.Entry<String, MutableInt>>() {
		@Override
		public String format(Map.Entry<String, MutableInt> t) {
			return String.format("%s{%d}", t.getKey(), t.getValue().intValue());
		}
	};


	// --- Get sets of terms

	static public void update(Map<String, MutableInt> terms, String word) {
		MutableInt v = terms.get(word);
		if (v == null)
			v = terms.put(word, new MutableInt(1));
		else
			v.add(1);

	}



	// ---- Relevance feedback parameters ----
	// ---------------------------------------
	static public abstract class PseudoRF {
		@Argument(name = "top-k", help = "Top results to consider (default: top 10)")
		int k = 10;

		/**
		 * @param results
		 *            The set of results
		 * @throws java.io.IOException
		 */
		abstract Collection<CharSequence> process(
				ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results)
				throws IOException;

		abstract void init() throws IOException;

		@Choice(name = "trec-8")
		static public class TopRanked extends PseudoRF {

			@Argument(name = "threshold", help = "Threshold")
			double c = 0;

			// log of vocabularySize log(V)
			private double logV;

			private long vocabularySize;

			private IntBigList frequencies;

			private double logN;

			/**
			 * The ith entry stores log(Comb(k; i+1))
			 */
			double[] combinaisons;

			private IndexConfiguration index;

			private DocumentCollection collection;

			private int fieldIndex;

			private TermProcessor processor;

			@Override
			void init() throws IOException {
				vocabularySize = index.index.numberOfTerms;
				this.frequencies = index.getFrequencies();
				logN = log(index.index.numberOfDocuments);
				logV = log(vocabularySize);

				// TODO: Annalina - create a transformer that stem & stop using
				// processor
				// queryEngine.transformer(transformer);

				// Compute log(Comb(k; i+1)) for i = 0 to k-1
				combinaisons = new double[k];
				// log(Comb(k; 1)) is log(k)
				combinaisons[0] = log(k);
				for (int i = 1; i < k; i++) {
					// C(k; i+1) = C(k; i) * (k-i) / (i+1)
					// [don't forget that combinaisons[i] is C(k; i+1)
					combinaisons[i] = combinaisons[i - 1] + log(k - i)
							- log(i + 1);
				}

				fieldIndex = collection.factory().fieldIndex(index.field);
				if (fieldIndex < 0)
					throw new RuntimeException(String.format(
							"Could not find field %s in index", index.field));

			}

			@Override
			public Collection<CharSequence> process(
					ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results)
					throws IOException {
				// Creates a new collection of terms
				Collection<CharSequence> newTerms = new HashSet<>();

				// Get a subspace representation of the K first documents
				if (k > results.size()) {
					logger
							.warn(String.format(
									"The number of returned documents (%d) is inferior to the number of documents (%d) for blind RF",
									results.size(), k));
				}

				// --- Compute the frequencies of terms
				Long2LongRBTreeMap relFreq = computeTermFrequencies(results);

				// --- Select the candidates
				for (Long2LongMap.Entry entry : relFreq.long2LongEntrySet()) {
					long rt2 = entry.getLongValue();
					int nt = frequencies.getInt(entry.getLongKey());
					final double termScore = rt2 * (logN - log(nt))
							- combinaisons[(int)(rt2 - 1)] - logV;
					if (termScore > c) {
						// Add term to set
						final CharSequence term = index.getTerm(entry
								.getLongKey());
						newTerms.add(term);
						logger.info(String.format("Adding term %s", term));
					} else {
						if (logger.isDebugEnabled())
							logger.debug(String.format("Not adding term %s (%g <= %g)", index
									.getTerm(entry.getLongKey()), termScore, c));
					}

				}

				return newTerms;
			}

			/**
			 *
             * @param results
             * @return
			 * @throws java.io.IOException
			 */
			private Long2LongRBTreeMap computeTermFrequencies(
                    ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results)
					throws IOException {
				Long2LongRBTreeMap relFreq = new Long2LongRBTreeMap();
				relFreq.defaultReturnValue(0);

				for (int i = 0; i < min(k, results.size()); i++) {
					Document document = collection
							.document(results.get(i).document);
					Reader reader = (Reader) document.content(fieldIndex);
					WordReader wordReader = document.wordReader(fieldIndex);
					wordReader.setReader(reader);

					MutableString word = new MutableString();
					MutableString nonWord = new MutableString();
					final LongRBTreeSet set = new LongRBTreeSet();

					while (wordReader.next(word, nonWord)) {
						if (processor.processTerm(word)) {
							long termId = index.getTermId(word);
							if (termId >= 0)
								if (set.add(termId))
									relFreq
											.put(termId,
													relFreq.get(termId) + 1);
						}
					}

					document.close();

				}
				return relFreq;
			}
		}

	}

}

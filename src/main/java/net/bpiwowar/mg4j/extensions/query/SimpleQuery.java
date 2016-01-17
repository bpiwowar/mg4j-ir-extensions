package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.utils.Output;
import net.bpiwowar.mg4j.extensions.utils.*;
import org.apache.commons.lang.mutable.MutableInt;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * An abstract query representation that handles search engine like queries
 *
 * @author bpiwowar
 * @date 02/08/2007
 */
public class SimpleQuery {
    /**
     * A pattern to filter out non-words from queries
     */
    final public static Pattern wordPattern = Pattern
            .compile("^[^\\d#&;=(){}|/,<>_]+$");

    /**
     * The query string
     */
    String query;

    /**
     * The query representation (sequence of {@link Component}s)
     */
    ArrayList<Component> sequence = new ArrayList<>();

    private int numberOfWords;

    static final Pattern RE_B_SPACES = Pattern.compile("^\\s*");

    static final Pattern RE_E_SPACES = Pattern.compile("\\s*$");

    /**
     * An operator
     *
     * @author bpiwowar
     * @date Nov 19, 2007
     */
    public enum Operator {
        PLUS, NONE, MINUS
    }

    static abstract public class Component {
        Operator operator = Operator.NONE;

        public Operator getOperator() {
            return operator;
        }

        public Component() {
        }

        Component(Operator operator) {
            this.operator = operator;
        }

        public Component(char c) {
            if (c == '-')
                operator = Operator.MINUS;
            else if (c == '+')
                operator = Operator.PLUS;
            else
                operator = Operator.NONE;
        }

        public String toString() {
            switch (operator) {
                case MINUS:
                    return "-";
                case PLUS:
                    return "+";
                case NONE:
                    break;
            }
            return "";
        }

        public void printTree(PrintStream out) {
            switch (operator) {
                case MINUS:
                    out.print("[-]");
                    break;
                case PLUS:
                    out.print("[+]");
                    break;
                case NONE:
                    break;
            }
        }
    }

    /**
     * A phrase in the query
     *
     * @author bpiwowar
     */
    static public class Phrase extends Component {
        ArrayList<Term> terms = new ArrayList<>();

        /**
         * @param words
         */
        public Phrase(String[] words) {
            for (String w : words)
                terms.add(new Term(w));
        }


        public Phrase() {
        }


        void add(final String s) {
            terms.add(new Term(s));
        }

        public ArrayList<Term> getTerms() {
            return terms;
        }

        @Override
        public void printTree(PrintStream out) {
            super.printTree(out);
            out.print("<phrase: ");
            for (int i = 0, N = terms.size(); i < N; i++) {
                if (i > 0)
                    out.print(" ");
                terms.get(i).printTree(out);
            }
            out.print(">");
        }

        /*
         * (non-Javadoc)
         *
         * @see bpiwowar.sophistication.Query.Component#toString()
         */
        @Override
        public String toString() {
            String s = super.toString() + '"';
            for (int i = 0, N = terms.size(); i < N; i++) {
                if (i > 0)
                    s += " ";
                s += terms.get(i).toString();
            }
            return s + '"';
        }
    }

    /**
     * A query term
     *
     * @author bpiwowar
     * @date Nov 19, 2007
     */
    static public class Term extends Component {
        String term;

        Term(final String s) {
            super(s.charAt(0));
            if (s.charAt(0) == '-' || s.charAt(0) == '+') {
                term = s.substring(1);
            } else
                term = s;
        }

        public String getTerm() {
            return term;
        }

        @Override
        public void printTree(PrintStream out) {
            super.printTree(out);
            out.print(term);
        }

        @Override
        public String toString() {
            return super.toString() + term;
        }
    }

    public SimpleQuery(Tokenizer tokenizer, String query) {
        this.query = query;

        // Parse the query into a tree

        // Parse the query
        String q = RE_B_SPACES.matcher(query).replaceFirst("");
        q = RE_E_SPACES.matcher(q).replaceFirst("");
        String[] phrases = q.split("\"");
        boolean isPhrase = false;

        numberOfWords = 0;
        query = "";
        Operator operator = Operator.NONE;

        // Parse the phrase
        for (String s : phrases) {
            s = RE_B_SPACES.matcher(s).replaceFirst("");
            s = RE_E_SPACES.matcher(s).replaceFirst("");
            if (!s.equals("")) {

                List<String> words = tokenizer.tokenize(s);
                if (words.size() > 0) {
                    if (isPhrase && words.size() > 1) {
                        final Phrase phrase = new Phrase(words.toArray(new String[words.size()]));
                        numberOfWords += words.size();
                        phrase.operator = operator;
                        sequence.add(phrase);
                    } else {
                        for (String w : words) {
                            operator = Operator.NONE;

                            if (w.equals("+")) {
                                operator = Operator.PLUS;
                            } else if (w.equals("-")) {
                                operator = Operator.MINUS;
                            } else {
                                sequence.add(new Term(w));
                                numberOfWords++;
                            }
                        }
                    }
                }
            }
            isPhrase = !isPhrase;
        }

    }

    /**
     * Returns the (normalized) query
     */
    @Override
    public String toString() {
        String s = "";
        for (int i = 0, N = sequence.size(); i < N; i++) {
            if (i > 0)
                s += " ";
            s += sequence.get(i).toString();
        }
        return s;
    }

    public Iterable<String> terms() {
        return () -> new Iterator<String>() {
            // Iterator on components
            final Iterator<Component> componentIterator = sequence
                    .iterator();

            // Iterator on terms in a phrase
            Iterator<Term> termIterator;

            // Current term
            String currentTerm;

            {
                doNext();
            }

            private void doNext() {
                currentTerm = null;

                if (termIterator == null || !termIterator.hasNext()) {
                    if (componentIterator.hasNext()) {
                        Component c = componentIterator.next();
                        if (c instanceof Term)
                            currentTerm = ((Term) c).term;
                        else {
                            termIterator = ((Phrase) c).terms
                                    .iterator();
                            currentTerm = termIterator.next().term;
                        }
                    }
                } else
                    currentTerm = termIterator.next().term;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public String next() {
                String next = currentTerm;
                doNext();
                return next;
            }

            public boolean hasNext() {
                return currentTerm != null;
            }

        };
    }

    public ArrayList<Component> getSequence() {
        return sequence;
    }

    public <T extends Set<String>> T getTerms(T set) {
        for (String term : terms())
            set.add(term);
        return set;
    }

    public Set<String> getTerms() {
        HashSet<String> set = new HashSet<String>();
        for (String term : terms()) {
            set.add(term);
        }
        return set;
    }

    public Set<String> getTerms(Pattern pattern) {
        HashSet<String> set = new HashSet<String>();
        for (String term : terms()) {
            if (pattern.matcher(term).matches())
                set.add(term);
        }
        return set;
    }

    /**
     * Print the query tree
     *
     * @param out
     */
    public void printTree(PrintStream out) {
        for (int i = 0, N = sequence.size(); i < N; i++) {
            if (i > 0)
                out.print(" ");
            sequence.get(i).printTree(out);
        }
        out.println();
    }

    /**
     * Get the query words, without all + - and quotes
     *
     * @return
     */
    public String getWordsQuery() {
        String s = null;
        for (String term : terms()) {
            if (s == null)
                s = term;
            else
                s += " " + term;
        }
        return s;
    }

    public static void main(String[] args) {

        SimpleQuery query = new SimpleQuery(new Tokenizer(), args[0]);
        java.lang.System.out.println("Normalised query: " + query);
        query.printTree(System.out);

        System.out.format("%n=== Query terms%n");
        for (String term : query.terms()) {
            System.out.format("\t%s%n", term);
        }

        if (args.length >= 2) {
            SimpleQuery q2 = new SimpleQuery(new Tokenizer(), args[1]);
            System.out.format("stats(%s,%s)=%s%n", query, q2, ngramSimilarties(
                    3, query, q2));

            WordDifferences wd = computeWordDifferences(query, q2);
            System.out.format(
                    "Only in q1 = {%s}, only in q2 = {%s}, in both = {%s}%n",
                    Output.toString(", ", wd.onlyQ1), Output.toString(", ",
                            wd.onlyQ2), Output.toString(", ", wd.both));
        }
    }

    static public class Similarities {
        public double cosine = 0;

        public static final int INCLUDED_SIMILARITY = 0;
        public static final int INCLUDE_SIMILARITY = 1;

        public double inclusion[] = {0, 0};

        @Override
        public String toString() {
            return String.format("[cosine=%.3f,inclusion=(%.3f,%.3f)]", cosine,
                    inclusion[0], inclusion[1]);
        }
    }

    /**
     * Compute the cosine similarity using a n-gram representation
     *
     * @param n the n-gram parameter
     */
    public static Similarities ngramSimilarties(int n, SimpleQuery q1, SimpleQuery q2) {
        String s[] = {q1.getWordsQuery(), q2.getWordsQuery()};
        Similarities similarities = new Similarities();

        if (s[0] == null || s[1] == null)
            return similarities;

        if (s[0].length() < n || s[1].length() < n)
            return similarities;


        @SuppressWarnings("unchecked")
        DefaultMap<String, MutableInt> maps[] = new DefaultMap[2];

        double[] l2 = new double[2];
        int[] l = {0, 0};

        for (int k = 0; k < 2; k++) {
            maps[k] = new DefaultMap<>(TreeMap.class,
                    MutableInt.class);
            for (int i = 0; i <= s[k].length() - n; i++) {
                maps[k].get(s[k].substring(i, i + n)).increment();
                l[k]++;
            }

            for (MutableInt m : maps[k].values())
                l2[k] += m.intValue() * m.intValue();

            l2[k] = Math.sqrt(l2[k]);
        }

        // And then compute
        MapIntersectionIterator iterator = MapIntersectionIterator.create(new Aggregator.MapValueArray(Integer.class, 2), maps);
        while (iterator.hasNext()) {
            Pair<String, MutableInt[]> pair = iterator.next();
            MutableInt[] values = pair.getSecond();
            similarities.cosine += values[0].intValue() * values[1].intValue();
            similarities.inclusion[0] += Math.min(values[0].intValue(),
                    values[1].intValue());
            similarities.inclusion[1] += Math.min(values[0].intValue(),
                    values[1].intValue());
        }

        similarities.cosine /= (l2[0] * l2[1]);
        similarities.inclusion[0] /= l[0];
        similarities.inclusion[1] /= l[1];
        return similarities;
    }

    static final public class WordDifferences {
        TreeSet<String> onlyQ1 = new TreeSet<>(),
                onlyQ2 = new TreeSet<>(), both = new TreeSet<>();
    }

    /**
     * Computes which words are present in which queries
     *
     * @param q1 The first query
     * @param q2 The second query
     * @return A structure containing words belonging to the three possible sets
     * (only Q1, only Q2, or both queries)
     */
    static final WordDifferences computeWordDifferences(SimpleQuery q1, SimpleQuery q2) {
        // Get the query terms
        TreeSet<String> q1set = q1.getTerms(new TreeSet<String>()), q2set = q2
                .getTerms(new TreeSet<String>());
        WordDifferences wd = new WordDifferences();

        final Aggregator<String, Integer> aggregator = new Aggregator<String, Integer>() {
            int r = 0;

            @Override
            public boolean accept(int n, int size) {
                return true;
            }

            @Override
            public Integer aggregate() {
                return r;
            }

            @Override
            public void reset() {
                r = 0;
            }

            @Override
            public void set(int index, String k) {
                System.err.format("%d -> %s%n", index, k);
                if (k != null)
                    r += 2 * index - 1;
            }
        };

        JoinIterator<String, Integer> join = new JoinIterator<String, Integer>(
                aggregator, q1set, q2set);
        while (join.hasNext()) {
            Pair<String, Integer> next = join.next();
            System.err.println(next.toString());
            final int v = next.getSecond();
            final String word = next.getFirst();
            if (v == -1)
                wd.onlyQ1.add(word);
            else if (v == 1)
                wd.onlyQ2.add(word);
            else
                wd.both.add(word);
        }

        return wd;
    }

    /**
     * @return
     */
    public int numberOfWords() {
        return numberOfWords;
    }

}

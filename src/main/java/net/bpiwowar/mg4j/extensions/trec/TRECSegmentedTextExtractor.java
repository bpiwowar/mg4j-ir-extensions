/**
 * 
 */
package net.bpiwowar.mg4j.extensions.trec;

import net.bpiwowar.mg4j.extensions.TagPointer;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.Attribute;
import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.Element;
import it.unimi.dsi.parser.callback.Callback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A callback extracting text and titles for TREC documents. This implementation
 * also keeps track of the different segments (paragraphs) within the text. It
 * also stores the positions of elements and tags.
 * 
 * <P>
 * This callbacks extracts all text in the page, and the title. The resulting
 * text is available through {@link #text}, and the title through {@link #title}
 * . Furthermore, the segments of the resulting text are preserved.
 * <p>
 *
 * @author Ingo Frommholz &lt;ingo@dcs.gla.ac.uk&gt
 */

public class TRECSegmentedTextExtractor implements Callback {
	private static final Log LOG = LogFactory.getLog(TRECSegmentedTextExtractor.class);

	/** The text resulting from the parsing process. */
	public final MutableString text = new MutableString();
	/** The title resulting from the parsing process. */
	public final MutableString title = new MutableString();

	/** Tag pointers */
	private List<TagPointer> pointers = new ArrayList<TagPointer>();

	/**
	 * The current position in the stream
	 */
	private int currentPointer = 0;

	/**
	 * Flag that tells us if we are within a <script> element
	 */
	private boolean inScript = false;
	
	public TRECSegmentedTextExtractor() {
	}

	/**
	 * Returns the tag pointers
	 * 
	 * @return Iterator over tag pointers
	 */
	public Iterator<TagPointer> tagPointer() {
		return pointers.iterator();
	}

	@Override
	public void startDocument() {
		LOG.trace("Starting a new document");
		text.length(0);
		currentPointer = 0;
		pointers = new ArrayList<TagPointer>();

	}
	
	public List<TagPointer> getPointers() {
		return pointers;
	}
	

	@Override
	public boolean characters(final char[] characters, final int offset,
			final int length, final boolean flowBroken) {
		if (!inScript) {
			text.append(characters, offset, length);
			currentPointer += length;
		}
		return true;
	}

	@Override
	public boolean endElement(Element element) {
		if (!element.name.toString().trim().equals("script")) {
			pointers
			.add(new TagPointer(currentPointer, element, TagPointer.TagType.END));
			// put blank at element position if the element breaks the flow
			if (element.breaksFlow) {
				text.append(' ');
				currentPointer++;
			} 
		}
		else inScript = false;

		return true;
	}

	@Override
	public boolean startElement(Element element,
			Map<Attribute, MutableString> attrMapUnused) {

		// we ignore (java)script
		if (!element.name.toString().trim().equals("script")) {
			pointers.add(new TagPointer(currentPointer, element,
					TagPointer.TagType.START));

			// put blank at element position if the element breaks the flow
			if (element.breaksFlow) {
				text.append(' ');
				currentPointer++;
			}
		}
		else inScript = true;
		return true;
	}

	/**
	 * Returns the text. Note that the text may not be trimmed.
	 * 
	 * @return the text
	 */
	public MutableString getText() {
		return this.text;
	}

	@Override
	public boolean cdata(Element element, char[] text, int offset, int length) {
		return true;
	}

	@Override
	public void endDocument() {

	}

	@Override
	public void configure(final BulletParser parser) {
		parser.parseText(true);
		parser.parseTags(true);
		parser.parseCDATA(false);
		parser.parseAttributes(false);
	}

}

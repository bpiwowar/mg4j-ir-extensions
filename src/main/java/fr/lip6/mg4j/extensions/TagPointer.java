package fr.lip6.mg4j.extensions;

import it.unimi.dsi.parser.Element;

/**
 * Some convenience methods for element pointers
 * 
 * @author <a href="mailto:ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 * 
 */
public class TagPointer extends Pointer {
	/** The tag type (start or end tag) */
	public enum TagType {
		START, END
	};

	private Element element;
	private final TagType tagtype;

	/**
	 * @param position
	 *            the position in the text
	 * @param element
	 *            The element
	 * @param tagtype
	 *            tag type (start or end tag)
	 */
	public TagPointer(int position, Element element, TagType tagtype) {
		super(position);
		this.element = element;
		this.tagtype = tagtype;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ingo.iqir.trec.ElementPointer#getElement()
	 */
	public Element getElement() {
		return this.element;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ingo.iqir.trec.ElementPointer#getTagType()
	 */
	public TagType getTagType() {
		return this.tagtype;
	}
	
	@Override
	public String toString() {
		return String.format("[%s: %s]@%d", tagtype, element, position);
	}
}
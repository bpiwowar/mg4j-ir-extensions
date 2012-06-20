package fr.lip6.mg4j.extensions;

import java.util.Iterator;

/**
 * Marked up documents should provide a set of tag pointers 
 * @author <a href="mailto:ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 *
 */
public interface MarkedUpDocument {
	
	/**
	 * Returns the tag pointers for the given field as an iterator. Should
	 * return <code>null</code> if the field doesn't have any markup.
	 *
     *
     *
     *
     * @param field the field
     * @return iterator over tag pointers
	 */
	public Iterator<TagPointer> tags(int field);
}

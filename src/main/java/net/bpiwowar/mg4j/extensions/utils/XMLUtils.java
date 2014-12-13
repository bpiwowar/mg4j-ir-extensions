package net.bpiwowar.mg4j.extensions.utils;

import bpiwowar.argparser.utils.AbstractIterator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/10/12
 */
public class XMLUtils {
    /**
     * Iterates over elements in a node list
     */
    public static Iterable<? extends Element> elements(final NodeList nodes) {
        return new Iterable<Element>() {
            @Override
            public Iterator<Element> iterator() {
                return new AbstractIterator<Element>() {
                    int i = -1;

                    @Override
                    protected boolean storeNext() {
                        while (++i < nodes.getLength()) {
                            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                value = (Element) nodes.item(i);
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
        };
    }

    public static boolean is(Element element, QName qName) {
        final boolean equals = qName.equals(new QName(element.getNamespaceURI(), element.getTagName()));
        return equals;
    }
}

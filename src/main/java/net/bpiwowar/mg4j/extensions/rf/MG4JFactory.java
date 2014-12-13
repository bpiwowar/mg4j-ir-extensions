package net.bpiwowar.mg4j.extensions.rf;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

/**
 * MG4J factory
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/11/12
 */
public class MG4JFactory extends DocumentFactory<MG4JRelevanceFeedback.MG4JDocument> {
    final DocumentCollection collection;
    Object2IntRBTreeMap<String> map = null;

    public MG4JFactory(DocumentCollection collection) {
        this.collection = collection;
    }

    public long getDocId(MG4JRelevanceFeedback.MG4JDocument document) {
        if (document.docid == -1) {
            if (document.docno == null)
                throw new RuntimeException();
//            map = AbstractDocumentCollection.getReverseIDMap(collection);
            if (map == null) throw new NotImplementedException();
            document.docid = map.get(document.docno);
        }
        return document.docid;
    }

    @Override
    public String getDocNo(MG4JRelevanceFeedback.MG4JDocument document) {
        if (document.docno == null) {
            try {
                if (document.docid == -1)
                    throw new RuntimeException();
//                document.docno = (String) collection.metadata(
//                        document.docid).get(Metadata);
                throw new IOException("not implemented");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return document.docno;
    }

    @Override
    public MG4JRelevanceFeedback.MG4JDocument getDocument(String docno) {
        return new MG4JRelevanceFeedback.MG4JDocument(-1, docno);
    }


}

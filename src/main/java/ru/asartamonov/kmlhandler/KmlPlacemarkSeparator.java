package ru.asartamonov.kmlhandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Alexander Artamonov (asartamonov@gmail.com) 2016.
 */
class KmlPlacemarkSeparator implements KmlTransformer<Document> {
    private final InputStream archetypeKml = getClass().getResourceAsStream("/kml_archetype.kml");

    /**
     * Method takes a Collection of Documents, takes Placemrks from it and makes new
     * Collection of Documents separates each Placemark into one Document and returns this
     * new Collection.
     *
     * @param collection a Collection of kml Documents to be converted by the method.
     * @see KmlTransformer interface
     * @see Document
     */
    public Collection<Document> transform(Collection<Document> collection
    ) {
        Collection<Document> outKmlCollection = new ArrayList<>();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document kmlArchetype = docBuilder.parse(archetypeKml);
            Node kmlRootNode = kmlArchetype.getDocumentElement();
            docBuilder.reset();
            for (Document doc : collection) {
                NodeList placemarks = doc.getElementsByTagName("Placemark");
                if (placemarks.getLength() > 0) {
                    for (int i = 0; i < placemarks.getLength(); i++) {
                        Document newDocument = docBuilder.newDocument();
                        Node newKmlrootNode = newDocument.importNode(kmlRootNode, true);
                        newDocument.appendChild(newKmlrootNode);
                        Node newPlacemark = newDocument.importNode(placemarks.item(i), true);
                        Node placemarksFolder = (newDocument.getElementsByTagName("Folder").item(0));
                        placemarksFolder.appendChild(newPlacemark);
                        outKmlCollection.add(newDocument);
                        docBuilder.reset();
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("Check your kml files and archetype kml " +
                    "(kml_archetype.kml), error in " +
                    "KmlPlacemarkSeparator.transform\n" +
                    e.toString());
            System.exit(1);
        }
        return outKmlCollection;
    }
}

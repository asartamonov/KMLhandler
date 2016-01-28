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
class KmlPlacemarkUniter implements KmlTransformer<Document> {
    private final InputStream archetypeKml = getClass().getResourceAsStream("/kml_archetype.kml");

    /**
     * Method takes a Collection of Documents, takes Placemrks from it and makes new
     * Collection of Documents adding each Placemark into one Document and returns this
     * new Collection.
     *
     * @param collection a Collection of kml Documents to be converted by the method.
     * @see KmlTransformer
     * @see Document
     */
    public Collection<Document> transform(Collection<Document> collection) {
        Collection<Document> outKmlCollection = new ArrayList<>();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document kmlArchetype = docBuilder.parse(archetypeKml);
            Node kmlRootNode = kmlArchetype.getDocumentElement();
            docBuilder.reset();
            Document newDocument = docBuilder.newDocument();
            Node newKmlrootNode = newDocument.importNode(kmlRootNode, true);
            newDocument.appendChild(newKmlrootNode);
            Node placemarks = (newDocument.getElementsByTagName("Folder").item(0));
            for (Document document : collection) {
                NodeList pmarkNodeList = document.getElementsByTagName("Placemark");
                if (pmarkNodeList.getLength() > 0) {
                    for (int i = 0; i < pmarkNodeList.getLength(); i++) {
                        Node newPlacemark = newDocument.importNode(pmarkNodeList.item(i), true);
                        newDocument.adoptNode(newPlacemark);
                        placemarks.appendChild(newPlacemark);
                    }
                }
            }
            outKmlCollection.add(newDocument);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("Check your kml files and archetype kml " +
                    "(kml_archetype.kml), error in " +
                    "KmlPlacemarkUniter.transform\n" + e.toString());
            System.exit(1);
        }
        return outKmlCollection;
    }
}

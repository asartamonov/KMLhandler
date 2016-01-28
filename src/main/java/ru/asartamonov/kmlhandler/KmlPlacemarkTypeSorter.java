package ru.asartamonov.kmlhandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Alexander Artamonov (asartamonov@gmail.com) 2016.
 */
class KmlPlacemarkTypeSorter implements KmlTransformer<Document> {
    public PlacemarkType getPlacemarkType(Node node) {
        NodeList childNodes = node.getChildNodes();
        if (childNodes.getLength() > 0) {
            String nodeName;
            for (int i = 0; i < childNodes.getLength(); i++) {
                nodeName = childNodes.item(i).getNodeName();
                if (nodeName.equals(PlacemarkType.POINT.type))
                    return PlacemarkType.POINT;
                else if (nodeName.equals(PlacemarkType.POLYGON.type))
                    return PlacemarkType.POLYGON;
                else if (nodeName.equals(PlacemarkType.LINE_STRING.type))
                    return PlacemarkType.LINE_STRING;
            }
        }
        return PlacemarkType.UNSORTABLE;
    }

    private void createShapeFolders(Document doc) {

        Document document = doc;
        Node documentNode = document.getElementsByTagName("Document").item(0);

        //everything for "Points"
        Element pointsFolder = document.createElement("Folder");
        documentNode.appendChild(pointsFolder);
        Node pointsFolderName = document.createElement("name");
        pointsFolderName.appendChild(document.createTextNode("Points"));
        pointsFolder.appendChild(pointsFolderName);
        Node pointsFolderOpen = document.createElement("open");
        pointsFolderOpen.appendChild(document.createTextNode("1"));
        pointsFolder.appendChild(pointsFolderOpen);

        //everything for "Polygons"
        Element polygonsFolder = document.createElement("Folder");
        documentNode.appendChild(polygonsFolder);
        Node polygonsFolderName = document.createElement("name");
        polygonsFolderName.appendChild(document.createTextNode("Polygons"));
        polygonsFolder.appendChild(polygonsFolderName);
        Node polygonsFolderOpen = document.createElement("open");
        polygonsFolderOpen.appendChild(document.createTextNode("1"));
        polygonsFolder.appendChild(polygonsFolderOpen);

        //everything for "LineStrings"
        Element linesFolder = document.createElement("Folder");
        documentNode.appendChild(linesFolder);
        Node linestringsFolderName = document.createElement("name");
        linestringsFolderName.appendChild(document.createTextNode("LineStrings"));
        linesFolder.appendChild(linestringsFolderName);
        Node linestringsFolderOpen = document.createElement("open");
        linestringsFolderOpen.appendChild(document.createTextNode("1"));
        linesFolder.appendChild(linestringsFolderOpen);

        //everything for "Unsorted"
        Element unsortedFolder = document.createElement("Folder");
        documentNode.appendChild(unsortedFolder);
        Node unsortedFolderName = document.createElement("name");
        unsortedFolderName.appendChild(document.createTextNode("Unsorted"));
        unsortedFolder.appendChild(unsortedFolderName);
        Node unsortedFolderOpen = document.createElement("open");
        unsortedFolderOpen.appendChild(document.createTextNode("1"));
        unsortedFolder.appendChild(unsortedFolderOpen);
    }

    @Override
    public Collection<Document> transform(Collection<Document> collection) {
        Collection<Document> out = new ArrayList<>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            for (Document inDocument : collection) {
                Document document = documentBuilder.newDocument();
                Node root = document.importNode(inDocument.getDocumentElement(), true);
                document.appendChild(root);
                NodeList oldFolders = document.getElementsByTagName("Folder");
                for (int k = 0; k < oldFolders.getLength(); k++) {
                    oldFolders.item(k).getParentNode().removeChild(oldFolders.item(k));
                }
                createShapeFolders(document);
                NodeList allFolders = document.getElementsByTagName("Folder");
                Node defaultNode = allFolders.item(3);
                Node pointsNode = allFolders.item(0);
                Node polygonsNode = allFolders.item(1);
                Node lineStringsNode = allFolders.item(2);
                NodeList allPlacemarks = inDocument.getElementsByTagName("Placemark");
                if (allPlacemarks.getLength() > 0) {
                    for (int i = 0; i < allPlacemarks.getLength(); i++) {
                        Node placemark = allPlacemarks.item(i);
                        PlacemarkType placemarkType = getPlacemarkType(placemark);
                        switch (placemarkType) {
                            case POINT:
                                pointsNode.appendChild(document.importNode(placemark, true));
                                break;
                            case POLYGON:
                                polygonsNode.appendChild(document.importNode(placemark, true));
                                break;
                            case LINE_STRING:
                                lineStringsNode.appendChild(document.importNode(placemark, true));
                                break;
                            default:
                                defaultNode.appendChild(document.importNode(placemark, true));
                        }
                    }
                }
                out.add(document);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return out;
    }

    public enum PlacemarkType {
        POINT("Point"),
        POLYGON("Polygon"),
        LINE_STRING("LineString"),
        UNSORTABLE("Unsortable");
        public String type;

        PlacemarkType(String type) {
            this.type = type;
        }
    }
}

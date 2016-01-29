package ru.asartamonov.kmlhandler;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alexander Artamonov (asartamonov@gmail.com) 2016.
 */

class KmlFileReader {
    public Collection<Document> read(File folder) {
        File[] KMLFiles = folder.listFiles((FilenameFilter) new SuffixFileFilter(".kml"));
        Collection<Document> inputKmlDocs = new ArrayList<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            for (File kmlFile : KMLFiles) {
                inputKmlDocs.add(dBuilder.parse(kmlFile));
                dBuilder.reset();
            }
        } catch (IllegalArgumentException ia) {
            System.out.println("KmlFileReader.read" + " null as argument for parser");
            System.exit(1);
        } catch (ParserConfigurationException | IOException | SAXException pe) {
            System.out.println("KmlFileReader.read\n" + pe.toString());
            System.exit(1);
        } catch (NullPointerException nul) {
            System.out.println("no .kml files found at " + folder.toString());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return inputKmlDocs;
    }
}

class KmlFileWriter {

    private File outputFolder;

    KmlFileWriter(File folder) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        StringBuilder sb = new StringBuilder();
        sb.append(folder.getAbsolutePath());
        sb.append(File.separator);
        sb.append("KmlHandler_results_");
        sb.append(sdf.format(Calendar.getInstance().getTime()));
        outputFolder = new File(sb.toString());
        while (outputFolder.exists()) {
            sb.append("_");
        }
        boolean isCreated = outputFolder.mkdir();
        if (!isCreated) {
            System.out.println("Failed to initiate kmlFileWriter:" +
                    "\nfailed to create output directory at " + sb.toString() + " quiting");
            System.exit(1);
        }
    }

    private String getSinglePlacemarkDocumentType(Document placemark) {
        boolean isPoint, isPolygon, isTrack;
        isPoint = placemark.getElementsByTagName("Point").getLength() > 0;
        isPolygon = placemark.getElementsByTagName("Polygon").getLength() > 0;
        String placemarkType = isPoint ? "Point_" : isPolygon ? "Polygon_" : "Linestring_";
        return placemarkType;
    }

    private String getPlacemarkName(Document placemark) {
        String placemarkName = placemark.getElementsByTagName("name").item(2).getTextContent();
        placemarkName = placemarkName.replaceAll("[;\\\\/:*?\"|]", "_");
        return placemarkName;
    }

    private String getDocumentName(Document document) {
        String documentName = document.getElementsByTagName("name").item(0).getTextContent();
        documentName = documentName.replaceAll("[;\\\\/:*?\"|]", "_");
        documentName = documentName.substring(0, documentName.length()-3);
        return documentName;
    }

    boolean write(Collection<Document> collection, boolean addShapeTypePrefix, boolean addDocumentName) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            System.out.println("KmlFileWriter.write error");
            e.printStackTrace();
            System.exit(1);
        }
        StringBuilder fileNameBuilder = new StringBuilder(outputFolder.getAbsolutePath());
        fileNameBuilder.append(File.separator);
        int folderPathLength = fileNameBuilder.length();
        for (Document document : collection) {
            DOMSource source = new DOMSource(document);
            fileNameBuilder.append(addShapeTypePrefix == true ? getSinglePlacemarkDocumentType(document) : "");
            fileNameBuilder.append(addDocumentName == true ? getDocumentName(document) : getPlacemarkName(document));
            fileNameBuilder.append(".kml");
            File outputFile;
            //check for files with the same name to prevent rewriting and data lost
            while (new File(fileNameBuilder.toString()).exists()) {
                fileNameBuilder.insert(fileNameBuilder.length() - 4, "_");
            }
            if (collection.size() == 1 && addDocumentName == false)
                fileNameBuilder.insert(fileNameBuilder.length() - 4, " and " +
                        (document.getElementsByTagName("Placemark").getLength() - 1) + " more placemarks");
            outputFile = new File(fileNameBuilder.toString());
            try {
                outputFile.createNewFile();
                StreamResult result = new StreamResult(outputFile);
                transformer.transform(source, result);
            } catch (IOException e) {
                System.out.println("Failed to create file " + outputFile.toString() + "\n");
                e.printStackTrace();
                System.exit(1);
            } catch (TransformerException e) {
                System.out.println("Error while parsing file " + outputFile.toString() + "\n");
                e.printStackTrace();
                System.exit(1);
            }
            fileNameBuilder.delete(folderPathLength, fileNameBuilder.length());
        }
        File[] kmlFile = outputFolder.listFiles((FilenameFilter) new SuffixFileFilter(".kml"));
        System.out.println("processed files " + collection.size() +
                "\nresult saved at " + outputFolder.toString());
        return kmlFile.length == collection.size();
    }
}

/**
 * Main class of KmlHandler app, calls other parts of the app and communicates with a user.
 * Contains main method only and String static fileds with usage rules and user input tips.
 */
public class KmlHandler {
    private static final String HELLO;
    private static final String USAGE;

    static {
        USAGE = "This application is to handle placemarks from .kml files.\n" +
                "Type mode and full path to a folder with your .kmls then application\n" +
                "will do the rest.\n" +
                "Modes:\n " +
                "mode section must start with \"-\" sign" +
                "(mode \"u\") [unite] Placemarks from all files in one\n" +
                "(mode \"s\" [separate] all placemarks from all .kml files in folder\n" +
                "to different .kml files" +
                "(mode \"x\" [extract] different shapes (point, polygon, linestring) to different folders" +
                "in case of first flag \"-u\" this means extracting inside one .kml, in case of " +
                "\"-s\" this will add shape-prefix to .kml filename of separated placemarks" +
                "\nFolder:\n" +
                "address of folder with your .kml file(s)\n" +
                "Example: \n-sx C:\\myfolder\n" +
                "application will read all .kml files in myfolder and create new outputFolder in it\n" +
                "where results of placemarks handling will be stored.\n";
        HELLO = "Type mode and full path to outputFolder with .kml files or \"help\" for more info. \n" +
                "Available modes: -s, -u, -sx, -ux, x.\n" +
                "Example: \n-sx C:\\myfolder\n";
    }


    /**
     * Main method calls KmlFileReader class to read files in user defined folder, creates KmlTransformer accordingly
     * to user defined mode of application working, writes result to disk.
     *
     * @param args app is to be used with userinput after the launching, so it neglects args from command
     *             line before it is launched.
     * @throws Throwable IO Exception if problem with BufferedReader (user input stream)
     *                   or (theoretically) with user defined outputFolder, actually user defined outputFolder is to be read by KmlFileReader
     *                   which handles IOexceptions and shall catch it.
     * @see KmlFileReader
     * @see KmlFileWriter
     * @see KmlTransformer
     */
    public static void main(String[] args) throws Throwable {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        KmlFileReader kmlReader = new KmlFileReader();
        //use regex "^-(u|s|ux|sx) ([A-Za-z]:[\\|\/].+\S$)" to check user input fits "-flag(s) address" form
        //old (^(-u |-s )([A-Za-z]:(\\|\/)(.| )+)(\S$))
        Pattern pattern = Pattern.compile("^-(u|s|ux|sx|x) ([A-Za-z]:[\\\\|\\/].+\\S$)");
        do {
            System.out.println(HELLO);
            userInput = console.readLine();
            if (userInput.equalsIgnoreCase("help")) {
                System.out.println(USAGE);
            }
        }
        while (!pattern.matcher(userInput).matches());
        Matcher matcher = pattern.matcher(userInput);
        matcher.find();
        String selectedMode = matcher.group(1);
        String folder = matcher.group(2);
        File inputFolder = new File(folder);
        Mode mode = Mode.getMode(selectedMode);
        mode.process(inputFolder);
    }

    enum Mode {

        UNITER("u") {
            @Override
            public void process(File folder) {
                System.out.println("\"Uniter\" mode selected\n");
                KmlFileReader reader = new KmlFileReader();
                Collection<Document> input = reader.read(folder);
                KmlFileWriter writer = new KmlFileWriter(folder);
                KmlTransformer<Document> transformer = new KmlPlacemarkUniter();
                Collection<Document> output = transformer.transform(input);
                boolean isOK = writer.write(output, false, false);
                System.out.println("All .kml files processed normally -- " + isOK);
            }
        },
        SEPARATOR("s") {
            @Override
            public void process(File folder) {
                System.out.println("\"Separator\" mode selected\n");
                KmlFileReader reader = new KmlFileReader();
                Collection<Document> input = reader.read(folder);
                KmlFileWriter writer = new KmlFileWriter(folder);
                KmlTransformer<Document> transformer = new KmlPlacemarkSeparator();
                Collection<Document> output = transformer.transform(input);
                boolean isOK = writer.write(output, false, false);
                System.out.println("All .kml files processed normally -- " + isOK);
            }
        },
        EXTRACTOR("x") {
            @Override
            public void process(File folder) {
                System.out.println("\"Extractor\" mode selected\n");
                KmlFileReader reader = new KmlFileReader();
                Collection<Document> input = reader.read(folder);
                KmlFileWriter writer = new KmlFileWriter(folder);
                KmlTransformer<Document> transformer = new KmlPlacemarkTypeSorter();
                Collection<Document> output = transformer.transform(input);
                boolean isOK = writer.write(output, false, true);
                System.out.println("All .kml files processed normally -- " + isOK);
            }
        },
        UNITER_EXTRACTOR("ux") {
            @Override
            public void process(File folder) {
                System.out.println("\"Uniter-Extractor\" mode selected\n");
                KmlFileReader reader = new KmlFileReader();
                Collection<Document> input = reader.read(folder);
                KmlFileWriter writer = new KmlFileWriter(folder);
                KmlTransformer<Document> separator = new KmlPlacemarkUniter();
                KmlTransformer<Document> sorter = new KmlPlacemarkTypeSorter();
                Collection<Document> output = sorter.transform(separator.transform(input));
                boolean isOK = writer.write(output, false, false);
                System.out.println("All .kml files processed normally -- " + isOK);
            }
        },
        SEPARATOR_EXTRACTOR("sx") {
            @Override
            public void process(File folder) {
                System.out.println("\"Separator-Extractor\" mode selected\n");
                KmlFileReader reader = new KmlFileReader();
                Collection<Document> input = reader.read(folder);
                KmlFileWriter writer = new KmlFileWriter(folder);
                KmlTransformer<Document> separator = new KmlPlacemarkSeparator();
                Collection<Document> output = separator.transform(input);
                boolean isOK = writer.write(output, true, false);
                System.out.println("All .kml files processed normally -- " + isOK);
            }
        };

        private static final Map<String, Mode> modes;

        static {
            modes = new HashMap<>();
            modes.put("u", UNITER);
            modes.put("s", SEPARATOR);
            modes.put("x", EXTRACTOR);
            modes.put("ux", UNITER_EXTRACTOR);
            modes.put("sx", SEPARATOR_EXTRACTOR);
        }

        public String mode;

        Mode(String mode) {
            this.mode = mode;
        }

        public static Mode getMode(String key) {
            return modes.get(key);
        }

        public abstract void process(File folder);
    }
}
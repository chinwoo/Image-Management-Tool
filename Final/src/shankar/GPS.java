package shankar;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

//******************************************************************************
//******************************************************************************

/**
 *   Used to open, resize, rotate, crop and save images.
 *
 ******************************************************************************/

public class GPS {

    private BufferedImage bufferedImage = null;
    private java.util.ArrayList corners = null;

    private float outputQuality = 1f; //0.9f; //0.5f;

    private static final boolean useSunCodec = getSunCodec();
    private static Class JPEGCodec;
    private static Class JPEGEncodeParam;


    private Graphics2D g2d = null;

    public static String[] InputFormats = getFormats(ImageIO.getReaderFormatNames());
    public static String[] OutputFormats = getFormats(ImageIO.getWriterFormatNames());


    private IIOMetadata metadata;
    private HashMap<Integer, Object> exif;
    private HashMap<Integer, Object> iptc;
    private HashMap<Integer, Object> gps;
    private boolean saveMetadata = false;


    //**************************************************************************
    //** Constructor
    //**************************************************************************
    /**  Creates a new instance of this class using an existing image */

    public GPS(String PathToImageFile){
        this(new File(PathToImageFile));
    }

    public GPS(File file){
        if (!file.exists()) throw new IllegalArgumentException("Input file not found.");
        try{ createBufferedImage(new FileInputStream(file)); }
        catch(Exception e){}
    }

    public GPS(InputStream InputStream){
        createBufferedImage(InputStream);
    }

    public GPS(byte[] byteArray){
        this(new ByteArrayInputStream(byteArray));
    }

    public GPS(int width, int height){
        this.bufferedImage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        this.g2d = getGraphics();
    }

    public GPS(BufferedImage bufferedImage){
        this.bufferedImage = bufferedImage;
    }


    public GPS(RenderedImage img) {
        if (img instanceof BufferedImage) {
            this.bufferedImage = (BufferedImage) img;
        }
        else{
            ColorModel cm = img.getColorModel();
            WritableRaster raster =
                    cm.createCompatibleWritableRaster(img.getWidth(), img.getHeight());
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            java.util.Hashtable properties = new java.util.Hashtable();
            String[] keys = img.getPropertyNames();
            if (keys!=null) {
                for (int i = 0; i < keys.length; i++) {
                    properties.put(keys[i], img.getProperty(keys[i]));
                }
            }
            BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
            img.copyData(raster);
            this.bufferedImage = result;
        }
    }


    //**************************************************************************
    //** Constructor
    //**************************************************************************
    /** Creates a new instance of this class using a block of text.
     *  @param fontName Name of the font you with to use. Note that you can get
     *  a list of available fonts like this:
    <pre>
    for (String fontName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()){
    System.out.println(fontName);
    }
    </pre>
     */
    public GPS(String text, String fontName, int fontSize, int r, int g, int b){
        this(text, new Font(fontName, Font.TRUETYPE_FONT, fontSize), r,g,b);
    }


    public GPS(String text, Font font, int r, int g, int b){

        //Get Font Metrics
        Graphics2D t = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        t.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fm = t.getFontMetrics(font);
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        int descent = fm.getDescent();

        t.dispose();


        //Create Image
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        //Add Text
        float alpha = 1.0f; //Set alpha.  0.0f is 100% transparent and 1.0f is 100% opaque.
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(new Color(r, g, b));
        g2d.setFont(font);
        g2d.drawString(text, 0, height-descent);
    }


    //**************************************************************************
    //** setBackgroundColor
    //**************************************************************************


    //**************************************************************************
    //** getInputFormats
    //**************************************************************************


    //**************************************************************************
    //** getOutputFormats
    //**************************************************************************


    //**************************************************************************
    //** getFormats
    //**************************************************************************
    /**  Used to trim the list of formats. */

    private static String[] getFormats(String[] inputFormats){

        //Build a unique list of file formats
        HashSet<String> formats = new HashSet<String> ();
        for (int i=0; i<inputFormats.length; i++){
            String format = inputFormats[i].toUpperCase();
            if (format.contains("JPEG") && format.contains("2000")){
                formats.add("JP2");
                formats.add("J2C");
                formats.add("J2K");
                formats.add("JPX");
            }
            else if (format.equals("JPEG") || format.equals("JPG")){
                formats.add("JPE");
                formats.add("JFF");
                formats.add(format);
            }
            else{
                formats.add(format);
            }
        }

        //Sort and return the hashset as an array
        inputFormats = formats.toArray(new String[formats.size()]);
        java.util.Collections.sort(java.util.Arrays.asList(inputFormats));
        return inputFormats;
    }


    //**************************************************************************
    //** getSunCodec
    //**************************************************************************
    /** Attempts to load classes from the com.sun.image.codec.jpeg package used
     *  to compress jpeg images. These classes are marked as deprecated in Java
     *  1.7 and several distributions of Java no longer include these classes
     *  (e.g.  "IcedTea" OpenJDK 7). Returns true of the classes are available.
     */
    private static boolean getSunCodec(){
        try{
            JPEGCodec = Class.forName("com.sun.image.codec.jpeg.JPEGCodec");
            JPEGEncodeParam = Class.forName("com.sun.image.codec.jpeg.JPEGEncodeParam");
            return true;
        }
        catch(Exception e){
            return false;
        }
    }


    public int getWidth(){
        return bufferedImage.getWidth();
    }


    public int getHeight(){
        return bufferedImage.getHeight();
    }

    private Graphics2D getGraphics(){
        if (g2d==null){
            g2d = this.bufferedImage.createGraphics();

            //Enable anti-alias
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        return g2d;
    }



    public Color getColor(int x, int y){
        int pixel = bufferedImage.getRGB(x, y);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new Color(red, green, blue, alpha);
    }


    private void createBufferedImage(InputStream input) {
        try{
            //bufferedImage = ImageIO.read(input);

            javax.imageio.stream.ImageInputStream stream = ImageIO.createImageInputStream(input);

            Iterator iter = ImageIO.getImageReaders(stream);
            if (!iter.hasNext()) {
                return;
            }

            ImageReader reader = (ImageReader)iter.next();
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(stream, true, true);

            try {
                bufferedImage = reader.read(0, param);
                metadata = reader.getImageMetadata(0);
            }
            finally {
                reader.dispose();
                stream.close();
            }


            input.close();
        }
        catch(Exception e){
            //e.printStackTrace();
        }
    }


    //**************************************************************************
    //** Rotate
    //**************************************************************************


    public BufferedImage getBufferedImage(){
        return bufferedImage;
    }



    public byte[] getByteArray(){
        return getByteArray("jpeg");
    }


    public byte[] getByteArray(String format){
        byte[] rgb = null;

        format = format.toLowerCase();
        if (format.startsWith("image/")){
            format = format.substring(format.indexOf("/")+1);
        }

        try{
            if (isJPEG(format)){
                rgb = getJPEGByteArray(outputQuality);
            }
            else{
                ByteArrayOutputStream bas = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, format.toLowerCase(), bas);
                rgb = bas.toByteArray();
            }
        }
        catch(Exception e){}
        return rgb;
    }




    private boolean isJPEG(String FileExtension){
        FileExtension = FileExtension.trim().toLowerCase();
        if (FileExtension.equals("jpg") ||
                FileExtension.equals("jpeg") ||
                FileExtension.equals("jpe") ||
                FileExtension.equals("jff") ){
            return true;
        }
        return false;
    }

    private byte[] getJPEGByteArray(float outputQuality) throws IOException {
        if (outputQuality>=0f && outputQuality<=1.2f) {
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            BufferedImage bi = bufferedImage;
            int t = bufferedImage.getTransparency();if (t==BufferedImage.TRANSLUCENT){
                bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D biContext = bi.createGraphics();
                biContext.drawImage ( bufferedImage, 0, 0, null );
            }
            if (useSunCodec){


                try{

                    //For Java 1.7 users, we will try to invoke the Sun JPEG Codec using reflection
                    Object encoder = JPEGCodec.getMethod("createJPEGEncoder", OutputStream.class).invoke(JPEGCodec, bas);
                    Object params = JPEGCodec.getMethod("getDefaultJPEGEncodeParam", BufferedImage.class).invoke(JPEGCodec, bi);
                    params.getClass().getMethod("setQuality", float.class, boolean.class).invoke(params, outputQuality, true);
                    params.getClass().getMethod("setHorizontalSubsampling", int.class, int.class).invoke(params, 0, 2);
                    params.getClass().getMethod("setVerticalSubsampling", int.class, int.class).invoke(params, 0, 2);


                    //Save metadata as needed
                    if (saveMetadata && metadata!=null){
                        java.lang.reflect.Method setMarkerData = params.getClass().getMethod("setMarkerData", int.class, byte[][].class);

                        //Parse unknown markers (similar logic to the getUnknownTags method)
                        HashSet<Integer> markers = new HashSet<Integer>();
                        for (String name : metadata.getMetadataFormatNames()) {
                            IIOMetadataNode node = (IIOMetadataNode) metadata.getAsTree(name);
                            for (Node unknownNode : getElementsByTagName("unknown", node)){
                                String markerTag = getAttributeValue(unknownNode.getAttributes(), "MarkerTag");

                                try{
                                    int marker = Integer.parseInt(markerTag);
                                    if (!markers.contains(marker)){
                                        markers.add(marker);

                                        byte[] data = (byte[]) ((IIOMetadataNode) unknownNode).getUserObject();
                                        if (data!=null){
                                            byte[][] app = new byte[1][data.length];
                                            app[0] = data;
                                            setMarkerData.invoke(params, marker, app);
                                        }
                                    }
                                }
                                catch(Exception e){
                                    //e.printStackTrace();
                                }
                            }
                        }
                    }

                    encoder.getClass().getMethod("encode", BufferedImage.class, JPEGEncodeParam).invoke(encoder, bi, params);
                }
                catch(Exception e){
                    bas.reset();
                }
            }



            if (bas.size()==0){

                if (outputQuality>1f) outputQuality = 1f;

                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                JPEGImageWriteParam params = (JPEGImageWriteParam) writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(outputQuality);
                writer.setOutput(ImageIO.createImageOutputStream(bas));
                if (saveMetadata){
                    writer.write(metadata, new IIOImage(bi, null, metadata), params);
                }
                else{
                    writer.write(null, new IIOImage(bi, null, null), params);
                }
            }


            bas.flush();
            return bas.toByteArray();
        }
        else{
            return getByteArray();
        }
    }

    private int getImageType(){
        return getImageType(this.bufferedImage);
    }

    private int getImageType(BufferedImage bufferedImage){
        int imageType = bufferedImage.getType();
        if (imageType <= 0 || imageType == 12) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        return imageType;
    }

    public boolean equals(Object obj){
        if (obj!=null){
            if (obj instanceof GPS){
                GPS GPS = (GPS) obj;
                if (GPS.getWidth()==this.getWidth() &&
                        GPS.getHeight()==this.getHeight())
                {

                    //Iterate through all the pixels in the image and compare RGB values
                    for (int i = 0; i< GPS.getWidth(); i++){
                        for (int j = 0; j< GPS.getHeight(); j++){

                            if (!GPS.getColor(i,j).equals(this.getColor(i,j))){
                                return false;
                            }
                        }

                    }

                    return true;
                }

            }
        }
        return false;
    }

    public IIOMetadata getIIOMetadata(){
        return metadata;
    }

    public HashMap<Integer, Object> getIptcTags(){

        if (iptc==null){
            iptc = new HashMap<Integer, Object>();
            for (IIOMetadataNode marker : getUnknownTags(0xED)){
                byte[] iptcData = (byte[]) marker.getUserObject();
                HashMap<Integer, Object> tags = new MetadataParser(iptcData, 0xED).getTags("IPTC");
                iptc.putAll(tags);
            }
        }
        return iptc;
    }

    public byte[] getExifData(){
        IIOMetadataNode[] tags = getUnknownTags(0xE1);
        if (tags.length==0) return null;
        return (byte[]) tags[0].getUserObject();
    }
    public HashMap<Integer, Object> getExifTags(){
        if (exif==null) parseExif();
        return exif;
    }

    public HashMap<Integer, Object> getGpsTags(){
        if (gps==null) parseExif();
        return gps;
    }

    private void parseExif(){

        exif = new HashMap<Integer, Object>();
        gps = new HashMap<Integer, Object>();
        for (IIOMetadataNode marker : getUnknownTags(0xE1)){
            byte[] exifData = (byte[]) marker.getUserObject();

            MetadataParser metadataParser = new MetadataParser(exifData, 0xE1);
            HashMap<Integer, Object> exif = metadataParser.getTags("EXIF");
            HashMap<Integer, Object> gps = metadataParser.getTags("GPS");

            if (exif!=null) this.exif.putAll(exif);
            if (gps!=null) this.gps.putAll(gps);

            metadataParser = null;
        }
    }

    public double[] getGPSCoordinate(){
        getExifTags();
        try{
            Double lat = getCoordinate((String) gps.get(0x0002));
            Double lon = getCoordinate((String) gps.get(0x0004));
            String latRef = (String) gps.get(0x0001); //N
            String lonRef = (String) gps.get(0x0003); //W

            if (!latRef.equalsIgnoreCase("N")) lat = -lat;
            if (!lonRef.equalsIgnoreCase("E")) lon = -lon;

            return new double[]{lon, lat};
        }
        catch(Exception e){
            return null;
        }
    }


    private double getCoordinate(String RationalArray) {

        //num + "/" + den
        String[] arr = RationalArray.substring(1, RationalArray.length()-1).split(",");
        String[] deg = arr[0].trim().split("/");
        String[] min = arr[1].trim().split("/");
        String[] sec = arr[2].trim().split("/");

        double degNumerator = Double.parseDouble(deg[0]);
        double degDenominator = 1D; try{degDenominator = Double.parseDouble(deg[1]);} catch(Exception e){}
        double minNumerator = Double.parseDouble(min[0]);
        double minDenominator = 1D; try{minDenominator = Double.parseDouble(min[1]);} catch(Exception e){}
        double secNumerator = Double.parseDouble(sec[0]);
        double secDenominator = 1D; try{secDenominator = Double.parseDouble(sec[1]);} catch(Exception e){}

        double m = 0;
        if (degDenominator != 0 || degNumerator != 0){
            m = (degNumerator / degDenominator);
        }

        if (minDenominator != 0 || minNumerator != 0){
            m += (minNumerator / minDenominator) / 60D;
        }

        if (secDenominator != 0 || secNumerator != 0){
            m += (secNumerator / secDenominator / 3600D);
        }

        return m;
    }

    public String getGPSDatum(){
        getExifTags();
        return (String) gps.get(0x0012);
    }


    public IIOMetadataNode[] getUnknownTags(int MarkerTag){
        java.util.ArrayList<IIOMetadataNode> markers = new java.util.ArrayList<IIOMetadataNode>();
        if (metadata!=null)
            for (String name : metadata.getMetadataFormatNames()) {
                IIOMetadataNode node=(IIOMetadataNode) metadata.getAsTree(name);
                Node[] unknownNodes = getElementsByTagName("unknown", node);
                for (Node unknownNode : unknownNodes){
                    try{
                        int marker = Integer.parseInt(getAttributeValue(unknownNode.getAttributes(), "MarkerTag"));
                        if (marker==MarkerTag) markers.add((IIOMetadataNode) unknownNode);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        return markers.toArray(new IIOMetadataNode[markers.size()]);
    }

    public IIOMetadataNode[] getMetadataByTagName(String tagName){
        java.util.ArrayList<IIOMetadataNode> tags = new java.util.ArrayList<IIOMetadataNode>();
        if (metadata!=null)
            for (String name : metadata.getMetadataFormatNames()) {
                IIOMetadataNode node=(IIOMetadataNode) metadata.getAsTree(name);
                Node[] unknownNodes = getElementsByTagName(tagName, node);
                for (Node unknownNode : unknownNodes){
                    tags.add((IIOMetadataNode) unknownNode);
                }
            }
        return tags.toArray(new IIOMetadataNode[tags.size()]);
    }

    private static Node[] getElementsByTagName(String tagName, Node node){
        java.util.ArrayList<Node> nodes = new java.util.ArrayList<Node>();
        getElementsByTagName(tagName, node, nodes);
        return nodes.toArray(new Node[nodes.size()]);
    }

    private static void getElementsByTagName(String tagName, Node node, java.util.ArrayList<Node> nodes){
        if (node!=null && node.getNodeType()==1){

            String nodeName = node.getNodeName().trim();
            if (nodeName.contains(":") && !tagName.contains(":")){
                nodeName = nodeName.substring(nodeName.indexOf(":")+1);
            }

            if (nodeName.equalsIgnoreCase(tagName)){
                nodes.add(node);
            }

            NodeList childNodes = node.getChildNodes();
            for (int i=0; i<childNodes.getLength(); i++){
                getElementsByTagName(tagName, childNodes.item(i), nodes);
            }
        }
    }

    public static String getAttributeValue(NamedNodeMap attrCollection, String attrName){

        if (attrCollection!=null){
            for (int i=0; i < attrCollection.getLength(); i++ ) {
                Node node = attrCollection.item(i);
                if (node.getNodeName().equalsIgnoreCase(attrName)) {
                    return node.getNodeValue();
                }
            }
        }
        return "";
    }


    @SuppressWarnings("FieldCanBeLocal")
    private class MetadataParser {

// Implementation notes:
// (1) Merged Version 1.1 of the "Exif.java" and "ExifData.java" classes.
// (2) Added new IPTC metadata parser.
// (3) All unsigned integers are treated as signed ints (should be longs).
// (4) Added logic to parse GPS Info using the GPS IFD offset value (tag 34853,
//     hex 0x8825).
// (5) Added logic to parse an array of rational numbers (e.g. GPS metadata).
// (6) Improved performance in the parseExif() method by serializing only the
//     first 8 characters into a string (vs the entire EXIF byte array).
// (7) TODO: Need to come up with a clever scheme to parse MakerNotes.

        private final int bytesPerFormat[] = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
        private final int NUM_FORMATS = 12;
        private final int FMT_BYTE = 1;
        private final int FMT_STRING = 2;
        private final int FMT_USHORT = 3;
        private final int FMT_ULONG = 4;
        private final int FMT_URATIONAL = 5;
        private final int FMT_SBYTE = 6;
        //private final int FMT_UNDEFINED = 7;
        private final int FMT_SSHORT = 8;
        private final int FMT_SLONG = 9;
        private final int FMT_SRATIONAL = 10;
        //private final int FMT_SINGLE = 11;
        //private final int FMT_DOUBLE = 12;

        private byte[] data = null;
        private boolean intelOrder = false;

        private final int TAG_EXIF_OFFSET = 0x8769;
        private final int TAG_INTEROP_OFFSET = 0xa005;
        private final int TAG_GPS_OFFSET = 0x8825;
        private final int TAG_USERCOMMENT = 0x9286;

        private HashMap<String, HashMap<Integer, Object>> tags =
                new HashMap<String, HashMap<Integer, Object>>();


        public MetadataParser(byte[] data, int marker) {
            switch (marker) {
                case 0xED: parseIptc(data); break;
                case 0xE1: parseExif(data); break;
            }

            data = null;
        }

        private void parseIptc(byte[] iptcData) {

            HashMap<Integer, Object> tags = new HashMap<Integer, Object>();
            this.tags.put("IPTC", tags);

            data = iptcData;

            int offset = 0;
            while (offset < data.length) {
                if (data[offset] == 0x1c) {

                    offset++;

                    int directoryType;
                    int tagType;
                    int tagByteCount;
                    try {
                        directoryType = data[offset++];
                        tagType = data[offset++];
                        tagByteCount = get16u(offset);
                        offset += 2;
                    }
                    catch (Exception e) {
                        return;
                    }


                    int tagIdentifier = tagType | (directoryType << 8);

                    String str = "";
                    if (tagByteCount < 1 || tagByteCount>(data.length-offset)) {
                    }
                    else {
                        try {
                            str = new String(data, offset, tagByteCount, "UTF-8");
                            offset += tagByteCount;
                        }
                        catch (Exception e) {
                        }
                    }
                    tags.put(tagIdentifier, str);
                }
                else{
                    offset++;
                }
            }
        }

        public void parseExif(byte[] exifData) {

            HashMap<Integer, Object> tags = new HashMap<Integer, Object>();
            this.tags.put("EXIF", tags);


            try{
                String dataStr = new String(exifData, 0, 8, "UTF-8"); //new String(exifData);
                if (exifData.length <= 4 || !"Exif".equals(dataStr.substring(0, 4))) {
                    //System.err.println("Not really EXIF data");
                    return;
                }

                String byteOrderMarker = dataStr.substring(6, 8);
                if ("II".equals(byteOrderMarker)) {
                    intelOrder = true;
                } else if ("MM".equals(byteOrderMarker)) {
                    intelOrder = false;
                } else {
                    //System.err.println("Incorrect byte order in EXIF data.");
                    return;
                }
            }
            catch(Exception e){
                return;
            }


            data = exifData;

            int checkValue = get16u(8);
            if (checkValue != 0x2a) {
                data = null;
                //System.err.println("Check value fails: 0x"+ Integer.toHexString(checkValue));
                return;
            }


            if (data==null) return;


            int firstOffset = get32u(10);
            processExifDir(6 + firstOffset, 6, tags);
        }

        public HashMap<Integer, Object> getTags(String dir) {
            return tags.get(dir);
        }


        private void processExifDir(int dirStart, int offsetBase, HashMap<Integer, Object> tags) {
            if (dirStart>=data.length) return;


            int numEntries = get16u(dirStart);
            for (int de = 0; de < numEntries; de++) {
                int dirOffset = dirStart + 2 + (12 * de);

                int tag = get16u(dirOffset);
                int format = get16u(dirOffset + 2);
                int components = get32u(dirOffset + 4);

                //System.err.println("EXIF: entry: 0x" + Integer.toHexString(tag)
                //		 + " " + format
                //		 + " " + components);

                if (format < 0 || format > NUM_FORMATS) {
                    //System.err.println("Bad number of formats in EXIF dir: " + format);
                    return;
                }

                int byteCount = components * bytesPerFormat[format];
                int valueOffset = dirOffset + 8;

                if (byteCount > 4) {
                    int offsetVal = get32u(dirOffset + 8);
                    valueOffset = offsetBase + offsetVal;
                }

                if (tag == TAG_EXIF_OFFSET || tag == TAG_INTEROP_OFFSET || tag == TAG_GPS_OFFSET) {

                    String dirName = "";
                    switch (tag) {
                        case TAG_EXIF_OFFSET:
                            dirName = "EXIF";
                            break;
                        case TAG_INTEROP_OFFSET:
                            dirName = "EXIF";
                            break;
                        case TAG_GPS_OFFSET:
                            dirName = "GPS";
                            break;
                    }

                    tags = this.tags.get(dirName);
                    if (tags==null){
                        tags = new HashMap<Integer, Object>();
                        this.tags.put(dirName, tags);
                    }

                    int subdirOffset = get32u(valueOffset);
                    processExifDir(offsetBase + subdirOffset, offsetBase, tags);

                }

                else {

                    switch (format) {
                        case FMT_STRING:
                            String value = getString(valueOffset, byteCount);
                            if (value!=null) tags.put(tag, value);
                            break;
                        case FMT_SBYTE:
                        case FMT_BYTE:
                        case FMT_USHORT:
                        case FMT_SSHORT:
                        case FMT_ULONG:
                        case FMT_SLONG:
                            tags.put(tag, (int) getDouble(format, valueOffset));
                            break;
                        case FMT_URATIONAL:
                        case FMT_SRATIONAL:

                            if (components>1) {

                                //Create a string representing an array of rational numbers
                                StringBuffer str = new StringBuffer();
                                str.append("[");
                                for (int i=0; i<components; i++){
                                    str.append( getRational(valueOffset + (8 * i)) );
                                    if (i<components-1) str.append(",");
                                }
                                str.append("]");
                                tags.put(tag, str.toString());
                            }
                            else{
                                tags.put(tag, getRational(valueOffset));
                            }
                            break;


                        default: //including FMT_UNDEFINED
                            byte[] result = getUndefined(valueOffset, byteCount);
                            if (result!=null) tags.put(tag, result);
                            break;
                    }

                }
            }
        }

        private String getRational(int offset) {
            int num = get32s(offset);
            int den = get32s(offset + 4);
            String result = "";

            // This is a bit silly, I really ought to find a real GCD algorithm
            if (num % 10 == 0 && den % 10 == 0) {
                num = num / 10;
                den = den / 10;
            }

            if (num % 5 == 0 && den % 5 == 0) {
                num = num / 5;
                den = den / 5;
            }

            if (num % 3 == 0 && den % 3 == 0) {
                num = num / 3;
                den = den / 3;
            }

            if (num % 2 == 0 && den % 2 == 0) {
                num = num / 2;
                den = den / 2;
            }

            if (den == 0) {
                result = "0";
            } else if (den == 1) {
                result = "" + num; // "" + int sure looks ugly...
            } else {
                result = "" + num + "/" + den;
            }
            return result;
        }

        private int get16s(int offset) {
            int hi, lo;

            if (intelOrder) {
                hi = data[offset + 1];
                lo = data[offset];
            } else {
                hi = data[offset];
                lo = data[offset + 1];
            }

            lo = lo & 0xFF;
            hi = hi & 0xFF;

            return (hi << 8) + lo;
        }

        private int get16u(int offset) {
            int value = get16s(offset);
            return value & 0xFFFF;
        }

        private int get32s(int offset) {
            int n1, n2, n3, n4;

            if (intelOrder) {
                n1 = data[offset + 3] & 0xFF;
                n2 = data[offset + 2] & 0xFF;
                n3 = data[offset + 1] & 0xFF;
                n4 = data[offset] & 0xFF;
            } else {
                n1 = data[offset] & 0xFF;
                n2 = data[offset + 1] & 0xFF;
                n3 = data[offset + 2] & 0xFF;
                n4 = data[offset + 3] & 0xFF;
            }

            return (n1 << 24) + (n2 << 16) + (n3 << 8) + n4;
        }

        private int get32u(int offset) {
            return get32s(offset); //Should probably return a long instead...
        }

        private byte[] getUndefined(int offset, int length) {
            return java.util.Arrays.copyOfRange(data, offset, offset+length);
        }

        private String getString(int offset, int length) {
            try{
                return new String(data, offset, length, "UTF-8").trim();
            }
            catch(Exception e){
                return null;
            }
        }

        private double getDouble(int format, int offset) {
            switch (format) {
                case FMT_SBYTE:
                    return data[offset];
                case FMT_BYTE:
                    int iValue = data[offset];
                    return iValue & 0xFF;
                case FMT_USHORT:
                    return get16u(offset);
                case FMT_ULONG:
                    return get32u(offset);
                case FMT_URATIONAL:
                case FMT_SRATIONAL:
                    int num = get32s(offset);
                    int den = get32s(offset + 4);
                    if (den == 0) return 0;
                    else return (double) num / (double) den;
                case FMT_SSHORT:
                    return get16s(offset);
                case FMT_SLONG:
                    return get32s(offset);
                default:
                    return 0.0;
            }
        }
    }


    private class Skew {

        public final static int ZERO = 0;
        public final static int CLAMP = 1;
        public final static int WRAP = 2;

        public final static int NEAREST_NEIGHBOUR = 0;
        public final static int BILINEAR = 1;

        protected int edgeAction = ZERO;
        protected int interpolation = BILINEAR;

        protected Rectangle transformedSpace;
        protected Rectangle originalSpace;

        private float x0, y0, x1, y1, x2, y2, x3, y3;
        private float dx1, dy1, dx2, dy2, dx3, dy3;
        private float A, B, C, D, E, F, G, H, I;


        private BufferedImage src;
        private BufferedImage dst;


        public Skew(BufferedImage src) {
            this.src = src;
            this.dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        }


        public BufferedImage setCorners(float x0, float y0,
                                        float x1, float y1,
                                        float x2, float y2,
                                        float x3, float y3)
        {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;

            dx1 = x1-x2;
            dy1 = y1-y2;
            dx2 = x3-x2;
            dy2 = y3-y2;
            dx3 = x0-x1+x2-x3;
            dy3 = y0-y1+y2-y3;

            float a11, a12, a13, a21, a22, a23, a31, a32;

            if (dx3 == 0 && dy3 == 0) {
                a11 = x1-x0;
                a21 = x2-x1;
                a31 = x0;
                a12 = y1-y0;
                a22 = y2-y1;
                a32 = y0;
                a13 = a23 = 0;
            } else {
                a13 = (dx3*dy2-dx2*dy3)/(dx1*dy2-dy1*dx2);
                a23 = (dx1*dy3-dy1*dx3)/(dx1*dy2-dy1*dx2);
                a11 = x1-x0+a13*x1;
                a21 = x3-x0+a23*x3;
                a31 = x0;
                a12 = y1-y0+a13*y1;
                a22 = y3-y0+a23*y3;
                a32 = y0;
            }

            A = a22 - a32*a23;
            B = a31*a23 - a21;
            C = a21*a32 - a31*a22;
            D = a32*a13 - a12;
            E = a11 - a31*a13;
            F = a31*a12 - a11*a32;
            G = a12*a23 - a22*a13;
            H = a21*a13 - a11*a23;
            I = a11*a22 - a21*a12;


            return filter(src,dst);
        }



        protected void transformSpace(Rectangle rect) {
            rect.x = (int)Math.min( Math.min( x0, x1 ), Math.min( x2, x3 ) );
            rect.y = (int)Math.min( Math.min( y0, y1 ), Math.min( y2, y3 ) );
            rect.width = (int)Math.max( Math.max( x0, x1 ), Math.max( x2, x3 ) ) - rect.x;
            rect.height = (int)Math.max( Math.max( y0, y1 ), Math.max( y2, y3 ) ) - rect.y;
        }


        public float getOriginX() {
            return x0 - (int)Math.min( Math.min( x0, x1 ), Math.min( x2, x3 ) );
        }

        public float getOriginY() {
            return y0 - (int)Math.min( Math.min( y0, y1 ), Math.min( y2, y3 ) );
        }


        private BufferedImage filter( BufferedImage src, BufferedImage dst ) {
            int width = src.getWidth();
            int height = src.getHeight();

            originalSpace = new Rectangle(0, 0, width, height);
            transformedSpace = new Rectangle(0, 0, width, height);
            transformSpace(transformedSpace);

            if ( dst == null ) {
                ColorModel dstCM = src.getColorModel();
                dst = new BufferedImage(
                        dstCM,
                        dstCM.createCompatibleWritableRaster(transformedSpace.width, transformedSpace.height),
                        dstCM.isAlphaPremultiplied(),
                        null
                );
            }

            int[] inPixels = getRGB( src, 0, 0, width, height, null );

            if ( interpolation == NEAREST_NEIGHBOUR )
                return filterPixelsNN( dst, width, height, inPixels, transformedSpace );

            int srcWidth = width;
            int srcHeight = height;
            int srcWidth1 = width-1;
            int srcHeight1 = height-1;
            int outWidth = transformedSpace.width;
            int outHeight = transformedSpace.height;
            int outX, outY;
            //int index = 0;
            int[] outPixels = new int[outWidth];

            outX = transformedSpace.x;
            outY = transformedSpace.y;
            float[] out = new float[2];

            for (int y = 0; y < outHeight; y++) {
                for (int x = 0; x < outWidth; x++) {
                    transformInverse(outX+x, outY+y, out);
                    int srcX = (int)Math.floor( out[0] );
                    int srcY = (int)Math.floor( out[1] );
                    float xWeight = out[0]-srcX;
                    float yWeight = out[1]-srcY;
                    int nw, ne, sw, se;

                    if ( srcX >= 0 && srcX < srcWidth1 && srcY >= 0 && srcY < srcHeight1) {
                        // Easy case, all corners are in the image
                        int i = srcWidth*srcY + srcX;
                        nw = inPixels[i];
                        ne = inPixels[i+1];
                        sw = inPixels[i+srcWidth];
                        se = inPixels[i+srcWidth+1];
                    } else {
                        // Some of the corners are off the image
                        nw = getPixel( inPixels, srcX, srcY, srcWidth, srcHeight );
                        ne = getPixel( inPixels, srcX+1, srcY, srcWidth, srcHeight );
                        sw = getPixel( inPixels, srcX, srcY+1, srcWidth, srcHeight );
                        se = getPixel( inPixels, srcX+1, srcY+1, srcWidth, srcHeight );
                    }
                    outPixels[x] = bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
                }
                setRGB( dst, 0, y, transformedSpace.width, 1, outPixels );
            }
            return dst;
        }

        final private int getPixel( int[] pixels, int x, int y, int width, int height ) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                switch (edgeAction) {
                    case ZERO:
                    default:
                        return 0;
                    case WRAP:
                        return pixels[(mod(y, height) * width) + mod(x, width)];
                    case CLAMP:
                        return pixels[(clamp(y, 0, height-1) * width) + clamp(x, 0, width-1)];
                }
            }
            return pixels[ y*width+x ];
        }


        protected BufferedImage filterPixelsNN( BufferedImage dst, int width,
                                                int height, int[] inPixels, Rectangle transformedSpace )
        {
            int srcWidth = width;
            int srcHeight = height;
            int outWidth = transformedSpace.width;
            int outHeight = transformedSpace.height;
            int outX, outY, srcX, srcY;
            int[] outPixels = new int[outWidth];

            outX = transformedSpace.x;
            outY = transformedSpace.y;
            int[] rgb = new int[4];
            float[] out = new float[2];

            for (int y = 0; y < outHeight; y++) {
                for (int x = 0; x < outWidth; x++) {
                    transformInverse(outX+x, outY+y, out);
                    srcX = (int)out[0];
                    srcY = (int)out[1];
                    // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                    if (out[0] < 0 || srcX >= srcWidth || out[1] < 0 || srcY >= srcHeight) {
                        int p;
                        switch (edgeAction) {
                            case ZERO:
                            default:
                                p = 0;
                                break;
                            case WRAP:
                                p = inPixels[(mod(srcY, srcHeight) * srcWidth) + mod(srcX, srcWidth)];
                                break;
                            case CLAMP:
                                p = inPixels[(clamp(srcY, 0, srcHeight-1) * srcWidth) + clamp(srcX, 0, srcWidth-1)];
                                break;
                        }
                        outPixels[x] = p;
                    } else {
                        int i = srcWidth*srcY + srcX;
                        rgb[0] = inPixels[i];
                        outPixels[x] = inPixels[i];
                    }
                }
                setRGB( dst, 0, y, transformedSpace.width, 1, outPixels );
            }
            return dst;
        }


        protected void transformInverse(int x, int y, float[] out) {
            out[0] = originalSpace.width * (A*x+B*y+C)/(G*x+H*y+I);
            out[1] = originalSpace.height * (D*x+E*y+F)/(G*x+H*y+I);
        }


        public int[] getRGB( BufferedImage image, int x, int y, int width, int height, int[] pixels ) {
            int type = image.getType();
            if ( type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB )
                return (int [])image.getRaster().getDataElements( x, y, width, height, pixels );
            return image.getRGB( x, y, width, height, pixels, 0, width );
        }

        public void setRGB( BufferedImage image, int x, int y, int width, int height, int[] pixels ) {
            int type = image.getType();
            if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
                image.getRaster().setDataElements( x, y, width, height, pixels );
            else
                image.setRGB( x, y, width, height, pixels, 0, width );
        }


        private int clamp(int x, int a, int b) {
            return (x < a) ? a : (x > b) ? b : x;
        }

        private double mod(double a, double b) {
            int n = (int)(a/b);

            a -= n*b;
            if (a < 0)
                return a + b;
            return a;
        }

        private float mod(float a, float b) {
            int n = (int)(a/b);

            a -= n*b;
            if (a < 0)
                return a + b;
            return a;
        }

        private int mod(int a, int b) {
            int n = a/b;

            a -= n*b;
            if (a < 0)
                return a + b;
            return a;
        }

        private int bilinearInterpolate(float x, float y, int nw, int ne, int sw, int se) {
            float m0, m1;
            int a0 = (nw >> 24) & 0xff;
            int r0 = (nw >> 16) & 0xff;
            int g0 = (nw >> 8) & 0xff;
            int b0 = nw & 0xff;
            int a1 = (ne >> 24) & 0xff;
            int r1 = (ne >> 16) & 0xff;
            int g1 = (ne >> 8) & 0xff;
            int b1 = ne & 0xff;
            int a2 = (sw >> 24) & 0xff;
            int r2 = (sw >> 16) & 0xff;
            int g2 = (sw >> 8) & 0xff;
            int b2 = sw & 0xff;
            int a3 = (se >> 24) & 0xff;
            int r3 = (se >> 16) & 0xff;
            int g3 = (se >> 8) & 0xff;
            int b3 = se & 0xff;

            float cx = 1.0f-x;
            float cy = 1.0f-y;

            m0 = cx * a0 + x * a1;
            m1 = cx * a2 + x * a3;
            int a = (int)(cy * m0 + y * m1);

            m0 = cx * r0 + x * r1;
            m1 = cx * r2 + x * r3;
            int r = (int)(cy * m0 + y * m1);

            m0 = cx * g0 + x * g1;
            m1 = cx * g2 + x * g3;
            int g = (int)(cy * m0 + y * m1);

            m0 = cx * b0 + x * b1;
            m1 = cx * b2 + x * b3;
            int b = (int)(cy * m0 + y * m1);

            return (a << 24) | (r << 16) | (g << 8) | b;
        }


    }
}
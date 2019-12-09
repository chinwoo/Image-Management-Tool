package shankar;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.ProcessStarter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class Controller {
    @FXML
    private BorderPane borderPane;
    @FXML
    private ChoiceBox choiceBox;
    @FXML
    private TextField textWidth;
    @FXML
    private TextField textHeight;

    @FXML
    private GridPane gridpane;
    @FXML
    private Label content_header;
    @FXML
    private Label notChoose;
    @FXML
    private Button convertBtn;
    @FXML
    private Button uploadBtn;

    private Image image;
    private List<File> files;
    private boolean didUpload;
    private boolean uploadCheck;

    @FXML
    public void uploadHandle() {
        String text;
        FileInputStream input;
        ImageView imageView;
        String sourcePath;
        String imageName;
        gridpane.getChildren().clear();
        content_header.setVisible(true);
        int count = 0;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpeg", "*.png", "*.gif" ,"*.jpg","*.mp4")
        );
        files = chooser.showOpenMultipleDialog(borderPane.getScene().getWindow());

        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                try {
                    Label textArea = new Label();
                    File file = files.get(i);
                    int indexX = i % 5;
                    int indexY = i / 5;
                    count++;

                    sourcePath = files.get(i).getPath();
                    GPS GPSInfo = new GPS(sourcePath);
                    java.util.HashMap<Integer, Object> exif = GPSInfo.getExifTags();
                    double[] coord = GPSInfo.getGPSCoordinate();

                    imageName = file.getName();
                    image = new Image(file.toURI().toString());

                    input = new FileInputStream(file);
                    image = new Image(input);
//                    System.out.println("Image Width: "+image.getWidth());
//                    System.out.println("Image Height: "+image.getHeight());

                    imageView = new ImageView(image);
                        imageView.setFitHeight(100);
                        imageView.setFitWidth(100);

                    System.out.println("\nImage Chosen Successfully from --> "+sourcePath);
                    System.out.println("Image Properties are \n\tName: "+imageName);
                    System.out.println("\tHeight: " + image.getHeight());
                    System.out.println("\tWidth: " + image.getWidth());
                    System.out.println("\tCamera: " + exif.get(0x0110));
                    System.out.println("\tLatitude: " + coord[0]);
                    System.out.println("\tLongitude: " + coord[1]);


                    if (coord != null) {
                        text =  "                 Name: " + imageName + "\n" +"" +
                                "                 Height: " + image.getHeight() + "\n" +
                                "                 Width: " + image.getWidth() + "\n" +
                                "                 Camera: " + exif.get(0x0110) + "\n" +
                                "                 Latitude: " + coord[0] + "\n" +
                                "                 Longitude: " + coord[1] + "\n";
                    } else {
                         text = "                 Name: " + imageName + "\n" +
                                "                 Height: " + image.getHeight() + "\n" +
                                "                 width: " + image.getWidth();
                    }
                    textArea.setText(text);
                    if (count > 5) {
                        indexY += 4;
                    }
                    if (count > 10) {
                        break;
                    }
                    gridpane.add(imageView, indexX, indexY);
                    gridpane.add(textArea, indexX, indexY + 1);
                    uploadCheck = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Chooser was cancelled");
        }
    }

    @FXML
    public void handleConvert() throws IOException {
        if(uploadCheck) {
            int width = 0;
            int height = 0;
            int convertWidth;
            int convertHeight;
            FileInputStream input;

            String filePath;
            String sourcePath;
            FileChooser chooser = new FileChooser();
            File file = chooser.showSaveDialog(borderPane.getScene().getWindow());
            for (int i = 0; i < files.size(); i++) {
                chooser.setInitialFileName(file.getName());
                chooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*." + choiceBox.getValue())
                );
                String destinationPath = "";
                File imageFile = files.get(i);
                input = new FileInputStream(imageFile);
                image = new Image(input);
                sourcePath = files.get(i).getPath();
                try {
                    filePath = file.getPath();
                    if (files.size() > 1) {
                        destinationPath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
                        destinationPath += imageFile.getName().substring(0, imageFile.getName().lastIndexOf("."));
                        destinationPath += ("."+choiceBox.getValue().toString());
                    } else {
                        destinationPath = filePath;
                        destinationPath += ("." + choiceBox.getValue());
                    }

                    if (textHeight.getText().equals("") && textWidth.getText().equals("")) {
                        width = (int) image.getWidth();
                        height = (int) image.getWidth();
                        System.out.println("width: "+width);
                        System.out.println("height: "+height);
                        System.out.println("sourcePath: "+sourcePath);
                        System.out.println("destinationPath: "+destinationPath);

                        convertImages(sourcePath,width,height,destinationPath);
                        //System.out.println("convert done");
                    } else if ((textHeight.getText().equals("") && !textWidth.getText().equals(""))
                            ||(!textHeight.getText().equals("") && textWidth.getText().equals("")) ){
                        didUpload = true;
                        break;
                    } else if (Integer.parseInt(textHeight.getText()) > 10000 || Integer.parseInt(textWidth.getText()) > 10000) {
                        didUpload = true;
                        break;
                    } else {
                        convertWidth = Integer.parseInt(textWidth.getText());
                        convertHeight = Integer.parseInt(textHeight.getText());

                        width = convertWidth;
                        height = convertHeight;
                        convertImages(sourcePath,width,height,destinationPath);
                    }
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            }
            uploadCheck = false;

            textWidth.clear();
            textHeight.clear();

        }else {
            notChoose.setText("Please select an Image to convert");
            notChoose.setVisible(true);
        }
    }

    private void convertImages(String sourcePath, int width, int height, String destinationPath){
        //ProcessStarter.setGlobalSearchPath("C:\\Program Files\\ImageMagick-7.0.9-Q16-HDRI");
        ProcessStarter.setGlobalSearchPath("C:\\Program Files (x86)\\ImageMagick-6.7.5-Q8");
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.addImage();
        op.resize(width, height);
        System.out.println("Resize Properties: \n \tPrefered Width: "+width+"\n \tPrefered Height: "+height);
        op.addImage();
        System.out.println("Image Saved Successfully to ---> "+destinationPath);
        Object[] listOfFiles = {sourcePath,destinationPath};
        try {
            cmd.run(op, listOfFiles);
            displayContent();

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void displayContent() {
        content_header.setVisible(false);
        gridpane.getChildren().clear();

        Label con2 = new Label("                 CONVERSION DONE! ");
        gridpane.add(con2, 0, 3);
    }
}

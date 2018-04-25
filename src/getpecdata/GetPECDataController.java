/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getpecdata;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

/**
 *
 * @author r
 */
public class GetPECDataController implements Initializable {
    
    @FXML
    TextField folder;
    @FXML
    Button getFolder;
    @FXML
    Button extract;
    @FXML
    CheckBox rev;
    @FXML
    Label result;
    @FXML
    Label probl;
    
    @FXML
    private void getFolderFired(ActionEvent event) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Some Directories");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = directoryChooser.showDialog(null);
        if (dir != null) {
            folder.setText(dir.getAbsolutePath());
        } else {
            folder.setText("");
        }
    }
    
    @FXML
    private void extractFired(ActionEvent event) throws IOException {
        if (folder.getText().isEmpty()) {
            result.setText("Indicar el directorio de las PECs!");
            result.setTextFill(Color.RED);
        } else {
            result.setText("Extrayendo PECs...");
            result.setTextFill(Color.BLACK);
            probl.setText("");
            
            startTask(folder.getText(), System.currentTimeMillis());
        }
    }
    
    public void startTask(String dir, double time) throws IOException {
        // Create a Runnable
        Runnable task = () -> {
            try {
                runTask(dir, time);
            } catch (IOException ex) {
                Logger.getLogger(GetPECDataController.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        
        // Run the task in a background thread
        Thread backgroundThread = new Thread(task);
        // Terminate the running thread if the application exits
        backgroundThread.setDaemon(true);
        // Start the thread
        backgroundThread.start();
    }

    public void runTask(String dir, double time) throws IOException {
        //Get all the pdf files of dir
        File PECfolder = new File(dir);
        FilenameFilter pdfFilter;
        pdfFilter = (File dir1, String name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".pdf");
        };
        File[] listOfFiles = PECfolder.listFiles(pdfFilter);

        boolean lProblems = false;
        boolean lComments = false;
        boolean lfirst = true;
        List<String> lines = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf("."));

                // Update the Label on the JavaFx Application Thread
                final String status = "Extrayendo PEC " + dni;
                Platform.runLater(() -> {
                    result.setText(status);
                });

                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                String producer = reader.getInfo().get("Producer");
                
                if (form.getFields().size()>0 & 
                        producer.substring(0,Math.min(11,producer.length())).equalsIgnoreCase("LibreOffice")) {
                    if (lfirst) {
                        //Get form fields names and sort alphabetically
                        for (String key : form.getFields().keySet()) {
                            if (key.substring(0, 1).equalsIgnoreCase("P")) names.add(key);
                        }
                        Collections.sort(names);
                        lfirst = false;
                    }
                    
                    //Build COMMENTS section
                    if (!form.getField("COMENT").isEmpty()) {
                        lComments = true;
                        comments.add(dni + ":" + form.getField("COMENT") + "\n");
                    }
                    //Header with identification data
                    String c = "'" + form.getField("APE1") + "','" + form.getField("APE2") + "','" + 
                            form.getField("NOMBRE") + "','" + dni + "'";
                    if (!rev.isSelected()) {
                        c = c + ((form.getField("HONOR").equalsIgnoreCase("Yes")) ? ",1" : ",0");
                    }

                    //Loop through the sorted fields and get the contents
                    for (String name : names) {
                        c = c + ",'" + form.getField(name).replace(".", ",") + "'";
                    }
                    lines.add(c);
                } else {
                    // If there are no fields on the form or the producer is not LibreOffice, the PDF file may be corrupted
                    lProblems = true;
                    if (form.getFields().isEmpty()) {
                        problems.add(dni + "; no hay campos");
                    }
                    else {
                        problems.add(dni + "; " + producer);
                    }
                }                
            }
        }
        
        //Save data
        Files.write(Paths.get(dir + "/datos_pecs.txt"), lines, Charset.forName("UTF-8"));
        //Save comments, if any
        if (lComments) Files.write(Paths.get(dir + "/comentarios.txt"), comments, Charset.forName("UTF-8"));
        //Save problems, if any
        if (lProblems) Files.write(Paths.get(dir + "/errores.txt"), problems, Charset.forName("UTF-8"));

        double t = (System.currentTimeMillis() - time)/1000;
        final String message = "Proceso finalizado (" +  String.format("%.1f", t) + " segs)." +
                (lComments ? " Hay comentarios." : "");
        final String c = (lProblems ? "Hay errores." : "");
        Platform.runLater(() -> {
            result.setText(message);
            probl.setText(c);
        });
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}

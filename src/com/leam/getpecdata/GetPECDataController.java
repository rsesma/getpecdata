package com.leam.getpecdata;

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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import javafx.application.Application.Parameters;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

public class GetPECDataController implements Initializable {
    
    @FXML
    TextField folder;
    @FXML
    Button getFolder;
    @FXML
    Button extract;
    @FXML
    Label result;
    @FXML
    Label probl;
    
    public void setParameters(Parameters p) {
    	List<String> args = p.getRaw();
    	if (!args.isEmpty()) {
    		folder.setText(args.get(0));
    		
            result.setText("Extrayendo PECs...");
            result.setTextFill(Color.BLACK);
            probl.setText("");
            
            try {
            	startTask(folder.getText(), System.currentTimeMillis());
            } catch (Exception e) {
            	System.out.println(e.getMessage());
            }
    	}
    }
    
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
        // get all the pdf files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter;
        pdfFilter = (File dir1, String name) -> { return name.toLowerCase().endsWith(".pdf"); };
        File[] PECs = folder.listFiles(pdfFilter);

        boolean lProblems = false;
        boolean lComments = false;
        boolean lfirst = true;
        boolean lhonor = false;
        List<String> lines = new ArrayList<>();
        List<String> mlines = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> memos = new ArrayList<>();
        for (File file : PECs) {
            if (file.isFile()) {
            	// get dni from filename
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf("."));

                // update the Label on the JavaFx Application Thread
                final String status = "Extrayendo PEC " + dni;
                Platform.runLater(() -> {
                    result.setText(status);
                });

                // open pdf form
				PDDocument pdf = PDDocument.load(file);
			    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
                
			    String producer = pdf.getDocumentInformation().getProducer();		// get form producer                
                if (form.getFields().size()>0) {
                    if (!producer.substring(0,Math.min(11,producer.length())).equalsIgnoreCase("LibreOffice")) {
                    	// if the producer is not LibreOffice, the PDF file may be corrupted
                    	lProblems = true;
                        problems.add(dni + "; " + producer);
                    }
                    
                    if (lfirst) {
                        // get form fields names and sort alphabetically
        				for (PDField f : form.getFields()) {
        					String name = f.getFullyQualifiedName();
        					if (name.substring(0, 1).equalsIgnoreCase("P")) names.add(name);		// answers
        					if (name.substring(0, 1).equalsIgnoreCase("M")) memos.add(name);		// memo fields
        					if (name.equalsIgnoreCase("HONOR")) lhonor = true;
        				}
                        Collections.sort(names);
                        Collections.sort(memos);
                        lfirst = false;
                    }
                    
                    // build COMMENTS section
                    if (!form.getField("COMENT").getValueAsString().isEmpty()) {
                        lComments = true;
                        comments.add(dni + ":" + form.getField("COMENT").getValueAsString() + "\n");
                    }
                    // header with identification data
                    String c = "'" + form.getField("APE1").getValueAsString() + "','" +
                    		form.getField("APE2").getValueAsString() + "','" + 
                            form.getField("NOMBRE").getValueAsString() + "','" + dni + "'";
                    if (lhonor) {
                    	PDCheckBox honor = (PDCheckBox) form.getField("HONOR");
                        c = c + (honor.isChecked() ? ",1" : ",0");
                    }

                    // loop through the sorted answers and get the contents
                    for (String name : names) {
                    	PDField f = form.getField(name);
                		if (f instanceof PDTextField) {
                			PDTextField ed = (PDTextField) f;				// text field: numeric or memo
                			c = c + ",'" + ed.getValue().replace(".", ",") + "'";
                		}
                		if (f instanceof PDComboBox) {
                			PDComboBox co = (PDComboBox) f;					// combobox field: closed answer
                			c = c + ",'" + co.getValue().get(0) + "'";
                		}
                    }
                    lines.add(c);
                    
                    if (!memos.isEmpty()) {
                    	// loop through the sorted memos and get the contents
                        String m = "'" + dni + "'";
	                    for (String name : memos) {
	                    	PDTextField ed = (PDTextField) form.getField(name);
	                		m = m + ",'" + ed.getValue().replace("'", "''") + "'";
                		}
	                    mlines.add(m);
                	}
                } else {
                	// if there are no fields on the form the PDF file may be corrupted
                    lProblems = true;
                    if (form.getFields().isEmpty()) {
                        problems.add(dni + "; no fields");
                    }
                }
                
                // close pdf form
                pdf.close();
                pdf = null;
                form = null; 
            }
        }

        // save data
        Files.write(Paths.get(dir + "/datos_pecs.txt"), lines, Charset.forName("UTF-8"));
        // save comments, if any
        if (lComments) Files.write(Paths.get(dir + "/comentarios.txt"), comments, Charset.forName("UTF-8"));
        // save problems, if any
        if (lProblems) Files.write(Paths.get(dir + "/errores.txt"), problems, Charset.forName("UTF-8"));
        // save memos, if any
        if (!memos.isEmpty()) Files.write(Paths.get(dir + "/memos.txt"), mlines, Charset.forName("UTF-8"));

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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getpecdata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
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
        double time = System.currentTimeMillis();           //timer
        
        String d = folder.getText();
        if (d.isEmpty()) {
            result.setText("Indicar el directorio de las PECs!");
            result.setTextFill(Color.RED);
        } else {
            result.setText("Extrayendo PECs...");
            result.setTextFill(Color.BLACK);
            probl.setText("");
            
            startTask(folder.getText());
        }
    }
    
    public void startTask(String dir)
    {
        // Create a Runnable
        Runnable task = () -> {
            runTask(dir);
        };
        
        // Run the task in a background thread
        Thread backgroundThread = new Thread(task);
        // Terminate the running thread if the application exits
        backgroundThread.setDaemon(true);
        // Start the thread
        backgroundThread.start();
    }

    public void runTask(String dir)
    {
        for(int i = 1; i <= 10; i++) {
            try {
                // Get the Status
                final String status = "Processing " + i + " of " + 10;
                // Update the Label on the JavaFx Application Thread       
                Platform.runLater(() -> {
                    result.setText(status);
                });
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}

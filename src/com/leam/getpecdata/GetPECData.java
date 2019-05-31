package com.leam.getpecdata;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GetPECData extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
    	
        FXMLLoader loader= new FXMLLoader(getClass().getResource("/fxml/GetPECData.fxml"));
        Parent root = loader.load();
        GetPECDataController c = loader.getController();
        
        c.setParameters(getParameters());
        
        stage.setScene(new Scene(root));
        stage.setTitle("Extraer datos PECs");
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}

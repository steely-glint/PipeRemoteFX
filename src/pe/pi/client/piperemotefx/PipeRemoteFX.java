/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.client.piperemotefx;

import com.phono.srtplight.Log;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author thp
 */
public class PipeRemoteFX extends Application {

    static String friend;

    @Override
    public void start(Stage stage) throws Exception {
        Parameters para = this.getParameters();
        friend = para.getNamed().getOrDefault("friend", "cca8b24bacc2e09ca27f6bcc45ac7f65539df4f94772e75ac25a8df1fe56e0bb");
        Parent root = FXMLLoader.load(getClass().getResource("LightSwitch.fxml"));

        Scene scene = new Scene(root);

        stage.setScene(scene);

        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Log.setLevel(Log.DEBUG);
        System.setProperty("java.net.preferIPv4Stack", "true");
        launch(args);
    }

}

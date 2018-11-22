package mapedit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class MapEdit extends Application {
  public static void main (String[] args) {
    launch(args);
  }
  
  @Override
  public void start (Stage primaryStage) {
    primaryStage.setTitle("MapEdit");
    UserInterface ui = new UserInterface(primaryStage);
    primaryStage.setScene(new Scene(ui.getPane(), 850, 550));
    primaryStage.show();
  }
}

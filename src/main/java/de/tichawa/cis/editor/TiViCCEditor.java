package de.tichawa.cis.editor;

import de.tichawa.util.MechaRow;
import de.tichawa.util.ElectRow;
import de.tichawa.util.CSVRow;
import de.tichawa.util.PriceRow;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.Stage;

public class TiViCCEditor extends Application
{
  private static String MODE = "Price";
  
  @Override
  public void start(Stage stage) throws Exception
  {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CCEditor.fxml"));
    switch(MODE)
    {
      case "Price":
      {
        loader.setController(new CCEditorController(PriceRow.getSample()));
        break;
      }
      case "Elect":
      {
        loader.setController(new CCEditorController(ElectRow.getSample(6)));
        break;
      }
      case "Mecha":
      {
        loader.setController(new CCEditorController(MechaRow.getSample(6, 5)));
        break;
      }
      default:
      {
        System.exit(0);
      }
    }
    stage.setScene(new Scene(loader.load()));
    stage.setTitle("TiViCC " + MODE + " Editor");
    stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/TiViCC.png")));
    stage.setOnCloseRequest(w ->
    {
      if(CSVRow.hasChanged())
      {
        Alert al = new Alert(Alert.AlertType.WARNING, "Es gibt ungespeicherte Änderungen. Wirklich verwerfen?", ButtonType.YES, ButtonType.NO);
        ((Stage) al.getDialogPane().getScene().getWindow()).getIcons().add(new Image(getClass().getResourceAsStream("/img/TiViCC.png")));
        if(al.showAndWait().orElse(ButtonType.NO) == ButtonType.NO)
        {
          w.consume();
        }
      }
    });
    stage.show();
  }

  public static void main(String[] args)
  {
    if(args.length > 0)
    {
      MODE = args[0];
    }
    launch(args);
  }
}

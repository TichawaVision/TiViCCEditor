package de.tichawa.cis.editor;

import de.tichawa.util.ArtNoProperty;
import de.tichawa.util.CSVRow;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;

import static de.tichawa.util.CSVRow.SEPARATOR;

public class CCEditorController implements Initializable
{

  protected final CSVRow sample;
  protected static final DecimalFormat FORMATTER = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
  protected File lastFile;

  @FXML
  private MenuItem open;
  @FXML
  private Menu search;
  @FXML
  private MenuItem save;
  @FXML
  private MenuItem close;
  @FXML
  private HBox addBox;
  @FXML
  private VBox root;
  @FXML
  private TableView<CSVRow> table;

  protected CCEditorController(CSVRow sample)
  {
    lastFile = new File(System.getProperty("user.dir"));
    this.sample = sample;
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    FORMATTER.setMaximumFractionDigits(340);
    root.widthProperty().addListener((observable, oldVal, newVal) ->
    {
      table.getColumns().forEach(column -> column.setPrefWidth((column.getMinWidth() / root.getMinWidth()) * newVal.doubleValue()));
      addBox.getChildren().stream()
              .filter(TextField.class::isInstance)
              .map(TextField.class::cast)
              .forEach(field -> field.setPrefWidth((field.getMinWidth() / root.getMinWidth()) * newVal.doubleValue()));
    });
    open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
    open.setOnAction(this::handleOpen);
    MenuItem pseudoSearch = new MenuItem("Suchen");
    pseudoSearch.setOnAction(this::handleSearch);
    pseudoSearch.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
    search.setOnShown(e ->
    {
      search.hide();
      pseudoSearch.fire();
    });
    search.getItems().add(pseudoSearch);
    save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
    save.setOnAction(this::handleSave);
    close.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
    close.setOnAction(this::handleClose);

    sample.getPropNames().forEach(name ->
    {
      Property p = sample.getProperty(name);
      if(p.getValue() instanceof String)
      {
        TableColumn<CSVRow, String> stringColumn = new TableColumn<>();
        stringColumn.setText(name);
        stringColumn.setMinWidth(sample.getPropWidth(name));
        stringColumn.setCellValueFactory(row -> row.getValue().getProperty(name));
        stringColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        table.getColumns().add(stringColumn);

        TextField stringField = new TextField();
        stringField.setPromptText(name);
        stringField.setMinWidth(sample.getPropWidth(name));
        addBox.getChildren().add(stringField);
      }
      else if(p.getValue() instanceof Double)
      {
        TableColumn<CSVRow, Double> doubleColumn = new TableColumn<>();
        doubleColumn.setText(name);
        doubleColumn.setMinWidth(sample.getPropWidth(name));
        doubleColumn.setCellValueFactory(row -> row.getValue().getProperty(name));
        doubleColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleConverter()));
        table.getColumns().add(doubleColumn);

        TextField doubleField = new TextField();
        doubleField.setPromptText(name);
        doubleField.setMinWidth(sample.getPropWidth(name));
        addBox.getChildren().add(doubleField);
      }
      else if(p.getValue() instanceof Integer)
      {
        TableColumn<CSVRow, Integer> intColumn = new TableColumn<>();
        intColumn.setText(name);
        intColumn.setMinWidth(sample.getPropWidth(name));
        intColumn.setCellValueFactory(row -> row.getValue().getProperty(name));
        if(p instanceof ArtNoProperty)
        {
          intColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>()
          {
            @Override
            public Integer fromString(String string)
            {
              return Integer.parseInt(string);
            }

            @Override
            public String toString(Integer object)
            {
              return object == 0 ? "" : String.format("%05d", object);
            }
          }));
        }
        else
        {
          intColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>()
          {
            @Override
            public Integer fromString(String string)
            {
              return Integer.parseInt(string);
            }

            @Override
            public String toString(Integer object)
            {
              return object.toString();
            }
          }));
        }
        table.getColumns().add(intColumn);

        TextField intField = new TextField();
        intField.setPromptText(name);
        intField.setMinWidth(sample.getPropWidth(name));
        addBox.getChildren().add(intField);
      }
    });
    table.setEditable(true);
  }

  protected List<TextField> getAddFields()
  {
    return addBox.getChildren().stream()
            .filter(TextField.class::isInstance)
            .map(TextField.class::cast)
            .collect(Collectors.toList());
  }

  protected TableView<CSVRow> getTable()
  {
    return table;
  }

  protected Window getWindow()
  {
    return root.getScene().getWindow();
  }

  @FXML
  protected void handleAdd(ActionEvent a)
  {
    CSVRow row = sample.getNewInstance(getAddFields().stream()
            .map(TextInputControl::getText)
            .collect(Collectors.joining(SEPARATOR)));
    if(row != null)
    {
      getTable().getItems().add(row);
      CSVRow.setChanged(true);
    }
    else
    {
      showAlert("Ungültige Eingabe.");
    }
  }

  @FXML
  private void handleClose(ActionEvent a)
  {
    if(table.getItems().isEmpty())
    {
      System.exit(0);
    }
    Alert al = new Alert(Alert.AlertType.WARNING, "Es gibt ungespeicherte Änderungen. Wirklich verwerfen?", ButtonType.YES, ButtonType.NO);
    ((Stage) al.getDialogPane().getScene().getWindow()).getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/editor/TiViCC.png")));
    if(!CSVRow.hasChanged() || al.showAndWait().orElse(ButtonType.NO) == ButtonType.YES)
    {
      CSVRow.setChanged(false);
      table.getItems().clear();
    }
  }

  @FXML
  protected void handleOpen(ActionEvent a)
  {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Preistabelle laden");
    if(lastFile != null)
    {
      chooser.setInitialDirectory(lastFile.isDirectory() ? lastFile : lastFile.getParentFile());
    }
    if((lastFile = chooser.showOpenDialog(getWindow())) != null)
    {
      Path source = lastFile.toPath();
      try
      {
        Files.lines(source)
                .map(line -> sample.getNewInstance(line))
                .filter(row -> row != null)
                .forEach(row -> getTable().getItems().add(row));
      }
      catch(IOException ex)
      {
      }
    }
  }

  @FXML
  private void handleSave(ActionEvent a)
  {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Preistabelle speichern");
    if(lastFile != null)
    {
      chooser.setInitialDirectory(lastFile.isDirectory() ? lastFile : lastFile.getParentFile());
    }
    if((lastFile = chooser.showSaveDialog(root.getScene().getWindow())) != null)
    {
      Path source = lastFile.toPath();
      try
      {
        List<String> data = table.getItems().stream()
                .map(CSVRow::toString)
                .collect(Collectors.toList());
        data.add(0, table.getColumns().stream()
                .map(TableColumnBase::getText)
                .collect(Collectors.joining("\t")));
        Files.write(source, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        CSVRow.setChanged(false);
      }
      catch(IOException ex)
      {
        ex.printStackTrace();
      }
    }
  }

  @FXML
  private void handleSearch(ActionEvent a)
  {
    TextInputDialog searchDiag = new TextInputDialog();
    searchDiag.setTitle("Schnellsuche");
    searchDiag.setContentText("Stichwort:");
    searchDiag.setHeaderText("");
    ((Stage) searchDiag.getDialogPane().getScene().getWindow()).getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/editor/TiViCC.png")));
    searchDiag.getEditor().textProperty().addListener((observable, oldVal, newVal) ->
    {
      table.getSelectionModel().clearSelection();
      table.getItems().stream()
              .filter(row -> row.getProperties().stream().anyMatch(p -> p.getValue().toString().toLowerCase().contains(newVal.toLowerCase())))
              .findFirst()
              .ifPresent(row -> table.getSelectionModel().select(row));
      table.scrollTo(table.getSelectionModel().getSelectedIndex());
      if(table.getSelectionModel().isEmpty())
      {
        searchDiag.getEditor().setStyle("-fx-text-fill: red;");
      }
      else
      {
        searchDiag.getEditor().setStyle("");
      }
    });
    searchDiag.show();
  }

  private class DoubleConverter extends StringConverter<Double>
  {

    @Override
    public Double fromString(String string)
    {
      return Double.parseDouble(string);
    }

    @Override
    public String toString(Double object)
    {
      return object.equals(Double.NaN) ? "NaN" : FORMATTER.format(object);
    }
  }

  private void showAlert(String text)
  {
    Alert al = new Alert(Alert.AlertType.ERROR, text, ButtonType.CLOSE);
    ((Stage) al.getDialogPane().getScene().getWindow()).getIcons().add(new Image(getClass().getResourceAsStream("/tivi/cis/editor/TiViCC.png")));
    al.showAndWait();
  }
}

package utilities;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.function.Function;

public class GlobalStatics  {
  private static String errorColor = "-fx-border-color: red";
  private static String defaultColor = "-fx-border-color: transparent";
  public static Function<WindowEvent, Void> onClose = e -> {
    System.exit(0);
    return null;
  };//default exit clause


  /**
   * highlight the given textFields and add an error message to them
   *
   * @param errorMessage the messaage tooltip that will be added to the textField
   * @param values the textfields where an error occurred
   */
  public static void highlightErrorsV(String errorMessage, TextField... values) {
    for(TextField s : values) {
      s.setStyle(errorColor);
      s.setTooltip(new Tooltip(errorMessage));
    }
  }

  /**
   * Dehighlight any textfields passed in
   * @param values the textfields that we want to reset the style of
   */
  public static void deHighlightErrors(TextField... values){
    for(TextField s : values) {
      s.setStyle(defaultColor);
    }
  }

  /**
   * Show a certain scene
   *
   * @param resource the FXML source behind the scene we want to show
   * @param title the title of the scene
   * @param w the width of the scene
   * @param h the height of the scene
   * @param modality the modality of the scene
   * @param opacity the opacity of the scene
   * @param resize whether the user can resize the scene
   * @param exitClause what to do on exit of the scene
   * @param tClass the parent class
   */
  public static void showX(String resource, String title, int w, int h, Modality modality, double opacity, boolean resize, Function<WindowEvent, Void> exitClause, Class tClass){
    try {
      FXMLLoader fxmlLoader = new FXMLLoader(tClass.getResource(resource));
      Parent root = fxmlLoader.load();
      Stage primaryStage = new Stage();
      primaryStage.setTitle(title);
      root.getStylesheets().add(tClass.getResource("/cssFiles/DarkTheme.bss").toExternalForm());
      primaryStage.setScene(new Scene(root, w, h));
      primaryStage.setResizable(resize);
      primaryStage.setOnCloseRequest(exitClause::apply);
      primaryStage.initModality(modality);
      primaryStage.setOpacity(opacity);
      primaryStage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the index of the given character
   * @param letter the letter we will find the index of
   * @return the index of the letter relative to alphabet, 0 if not found
   */
  public static int getIndex(char letter) {
    final char[] alphabet = {'A', 'B', 'C', 'D',
        'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L',
        'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X',
        'Y', 'Z'};
    for (int i = 0; i < alphabet.length; i++) {
      if (alphabet[i] == letter) {
        return i;
      }
    }
    return 0;
  }

  /**
   * populate the search field with results that match the initial typed symbol. (aa) would populate aapl
   * @param typedSymbol the currently typed querey in a search text field, e.g. "goo" for "google"
   * @param data the data table we can lookup from
   * @param searchTV the tableView we will populate with results
   */
  public static void populateSearchField(String typedSymbol, String[] data, TableView<String> searchTV) {
    if (typedSymbol.equals("")){
      return;
    }
    searchTV.getItems().clear();

    for (int i = data.length - 1; i >= 0; i--) {
      try {
        if (data[i].substring(0, typedSymbol.length()).equals(typedSymbol.toUpperCase())) {
          searchTV.getItems().add(data[i]);
        }
      } catch (IndexOutOfBoundsException e) {
        e.getMessage();
      }
    }
  }
}

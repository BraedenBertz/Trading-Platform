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
  };




  public static void highlightErrorsV(String errorMessage, TextField... values) {
    for(TextField s : values) {
      s.setStyle(errorColor);
      s.setTooltip(new Tooltip(errorMessage));
    }
  }

  public static void deHighlightErrors(TextField... values){
    for(TextField s : values) {
      s.setStyle(defaultColor);
    }
  }

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

  public static int getMaxHTTPSizeForRequest(String webserverURL) {
    //First we identify the webserver, microsoft IIS, nginx, apache, tomcat, etc
    /*

using System;
using System.Net;

public class TestApp {
    public static void Main( string[] args ) {
        HttpWebRequest request = (HttpWebRequest)WebRequest.Create("https://finance.yahoo.com/");
        WebResponse response = request.GetResponse();
        Console.Out.WriteLine( response.Headers.Get("Server") );
    }
}

     */
    //quick c# to get the type of webserver, yhf is ATS which is 8k header limit
    //Obviously, need to programmatically make this
    return 8000;
  }
}

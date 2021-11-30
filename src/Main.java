import controllers.SearchController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utilities.GlobalStatics;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.net.ftp.*;

import static utilities.GlobalStatics.onClose;

/**
 * Logic to start the application:
 * First it gets the market data, then it populates search table's data (i.e., makes the data available to each class)
 * Next it allows the user to login or create a new user
 * Once a login is successful, then we show the main application window, and close this window to preserve resources
 * */
public final class Main extends Application {
    //Container of paths to the preloaders (.gifs that are displayed while we are reading or writing
    //   data during the startup phase of the application)
    private final String[] preloaders = {
        "loadingIcons/load0.gif",
        "loadingIcons/load1.gif",
        "loadingIcons/load2.gif",
        "loadingIcons/load3.gif",
        "loadingIcons/load4.gif",
        "loadingIcons/load5.gif",
        "loadingIcons/load6.gif",
    };

    //Text that will ALWAYS be displayed on the tooltip
    private final String tooltipText = "It shouldn't take much longer";
    //Tooltip that will be installed on the .gif loading screen to tell the user what we are doing
    private final Tooltip tooltip = new Tooltip(tooltipText);

    /**
     * Launch the stage that will either start the universe refresh or allow the user to login
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Setup a window that provides a .gif preloader to entertain the user while they wait for the files,
     * paths from filePaths to be validated (exist, have data).
     * The validation will happen on another thread and go through each of the indices of filePaths
     *
     * @param primaryStage The stage that will be shown to the user (JavaFX UI Primary Thread)
     */
    @Override
    public void start(Stage primaryStage) {
        //Primary stage is the window that has a loading screen and tells the user that we are working on something
        VBox box = new VBox();
        Image load = new Image(getClass().getResource(preloaders[new Random().nextInt(preloaders.length)]).toExternalForm());
        ImageView loadingIV = new ImageView(load);
        Tooltip.install(loadingIV, tooltip);
        box.getChildren().add(loadingIV);
        primaryStage.setTitle("Initializing. . . ");
        Scene primaryScene = new Scene(box, 400, 300);
        primaryStage.setScene(primaryScene);
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();

        //Wait until validation flag is true
        Task<Void> getData = new Task<Void>() {
            @Override
            protected Void call() {
                long startTime = System.currentTimeMillis();
                final String[] filePaths = {
                    "src/log/stocks.txt",
                    "src/log/currencies.txt",
                    "src/log/commodities.txt",
                    "src/log/bonds.txt",
                    "src/log/options.txt",
                    "src/log/indices.txt",
                    "src/log/ETFs.txt",
                };
                final String[] nasdaqFilePaths = {
                    "/Symboldirectory/nasdaqlisted.txt",
                };
                ArrayList<String> stock = new ArrayList<>();
                FTPClient ftpClient = new FTPClient();
                Scanner scanner;
                try{
                    ftpClient.connect("ftp.nasdaqtrader.com");
                    boolean login = ftpClient.login("Anonymous", "guest");
                    for(int i = 0; i < nasdaqFilePaths.length; i++) {
                        File f = new File(filePaths[i]);
                        OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(f));
                        boolean filee = ftpClient.retrieveFile(nasdaqFilePaths[i], outputStream1);
                        outputStream1.close();
                        scanner = new Scanner(f);
                        scanner.useDelimiter("\\|");
                        scanner.nextLine();
                        while(scanner.hasNext()) {
                            stock.add(scanner.next());
                            scanner.nextLine();
                        }
                        scanner.close();
                        stock.remove(stock.size() - 1);
                        SearchController.addTable(stock.toArray(new String[0]), 0);

                        stock.clear();
                    }
                } catch(IOException e){
                    e.printStackTrace();
                    failed();
                } finally {
                    try {
                        ftpClient.disconnect();
                    } catch(IOException e) {
                        e.printStackTrace();
                        failed();
                    }
                }
                System.out.println("time taken to get to login Screen: "+(System.currentTimeMillis()-startTime)*0.001+" seconds");
                succeeded();
                return null;
            }
        };

        //Success means that all data is here; we are ready to continue the program
        getData.setOnSucceeded(v -> {
                primaryStage.close();
                showLoginStage(primaryStage);
        });

        //Something went wrong with getting the data
        getData.setOnFailed(v -> System.exit(-1));

        //Start getting data for program
        Platform.runLater(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.submit(getData);
        });
    }

    /**
     * Show the user the login window that allows them to login to their account
     * This stage will be handled by controllers.LoginController
     *
     * @param primaryStage The stage that will be shown to the user (JavaFX UI Primary Thread)
     */
    private void showLoginStage(Stage primaryStage) {

        primaryStage.close();
        GlobalStatics.showX("/fxmlFiles/PrimaryLogin.fxml",
            "Login",
            600,
            327,
            Modality.APPLICATION_MODAL,
            1.0,
            true,
            onClose,
            getClass());
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

/**
 *
 * @author bho
 */
public class IX_RighCleaner extends Application {
    private TextField folderField, userField,groupField, itemField, depthField, partitionField,dataIdField,folderPermField;
    private PasswordField passField;
    private LogView logView;
    private Logger logger;
    private CheckBox exportField, exportParentField;
    private final TabPane tPane = new TabPane();
    private TaskKeeper tKeeper;
    private boolean checkGlobalFields() {
        if(userField.getText() == null || userField.getText().isEmpty()) {
            return false;
        }
        if(passField.getText() == null ||passField.getText().isEmpty()) {
            return false;
        }
        return groupField.getText() != null;
    }
    
    private boolean checkLeftTab() {
        if(groupField.getText().isEmpty()) {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Wenn die Gruppe leer gelassen wird, werden alle Berechtigungen gelöscht am Objekt");
            alert.setContentText("Ist das so in Ordnung?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK
            } else {
                // ... user chose CANCEL or closed the dialog
                return false;
            }
        }
        if(depthField.getText() == null || depthField.getText().isEmpty()) {
            return false;
        }
        if(partitionField.getText() == null || partitionField.getText().isEmpty()) {
            return false;
        }
        if(itemField.getText() == null || itemField.getText().isEmpty()) {
            return false;
        }
        return !(folderField.getText() == null ||folderField.getText().isEmpty());
    }
    
    private boolean checkMiddleTab() {
        
        return !(folderPermField.getText() == null || folderPermField.getText().isEmpty());
    }
    
    public boolean checkRightTab() {
        if(groupField.getText().isEmpty()) {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Wenn die Gruppe leer gelassen wird, werden alle Berechtigungen gelöscht am Objekt");
            alert.setContentText("Ist das so in Ordnung?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK
            } else {
                // ... user chose CANCEL or closed the dialog
                return false;
            }
        }
        return !(dataIdField.getText() == null ||dataIdField.getText().isEmpty());
    }
    @Override
    public void start(Stage primaryStage) {
        Button btn = new Button();
        btn.setText("Aktualisier!");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(!checkGlobalFields()) {
                    return;
                }
                
                String[] split;
                List<String> stringList;
                ArrayList<Long> folderIds;
                switch(tPane.getSelectionModel().getSelectedItem().getText()) {
                    case "Update Items with folder id":
                        if(!checkLeftTab()) {
                            return;
                        }
                        split = folderField.getText().split(",");
                        stringList = new ArrayList<>(Arrays.asList(split)); //new ArrayList is only needed if you absolutely need an ArrayList
                        //Start update
                        folderIds = new ArrayList<>();
                        for(String string : stringList ) {
                            folderIds.add(Long.valueOf(string));                    
                        }
                        Updater updater_1 = new Updater(logger, userField.getText(),passField.getText(), Integer.valueOf(itemField.getText()), Integer.valueOf(depthField.getText()), Integer.valueOf(partitionField.getText()) ,groupField.getText(), folderIds, exportField.isSelected());
                        tKeeper.addNewTask(updater_1);
                        break;
                    case "Update with Item ID":
                        if(!checkRightTab()) {
                            return;
                        }
                        split = dataIdField.getText().split(",");
                        stringList = new ArrayList<>(Arrays.asList(split)); //new ArrayList is only needed if you absolutely need an ArrayList
                        //Start update
                        folderIds = new ArrayList<>();
                        for(String string : stringList ) {
                            folderIds.add(Long.valueOf(string));                    
                        }
                        UpdaterDataIds updaterids_1 = new UpdaterDataIds(logger,userField.getText(),passField.getText(),groupField.getText(),folderIds,exportField.isSelected(),exportParentField.isSelected());
                        tKeeper.addNewTask(updaterids_1);
                        break;
                    case "Update Permissions from folder":
                        if(!checkMiddleTab()) {
                            return;
                        }
                        split = folderPermField.getText().split(",");
                        stringList = new ArrayList<>(Arrays.asList(split)); //new ArrayList is only needed if you absolutely need an ArrayList
                        //Start update
                        folderIds = new ArrayList<>();
                        for(String string : stringList ) {
                            folderIds.add(Long.valueOf(string));                    
                        }
                        RightVererber vererber_1 = new RightVererber(logger, userField.getText(), passField.getText(), folderIds, exportField.isSelected());
                        tKeeper.addNewTask(vererber_1);
                        break;
                    default:
                        logger.warn("Something went wrong");
                }
                //Clear input fields
                folderField.clear();
                itemField.clear();
                depthField.clear();
                partitionField.clear();
                dataIdField.clear();
                folderPermField.clear();
            }
        });
        Lorem  lorem  = new Lorem();
        Log    log    = new Log();
        logger = new Logger(log, "main");
       
        logView = new LogView(logger);
        logView.setPrefWidth(400);
        ChoiceBox<Level> filterLevel = new ChoiceBox<>(
                FXCollections.observableArrayList(
                        Level.values()
                )
        );
        filterLevel.getSelectionModel().select(Level.DEBUG);
        logView.filterLevelProperty().bind(
                filterLevel.getSelectionModel().selectedItemProperty()
        );
        ToggleButton showTimestamp = new ToggleButton("Show Timestamp");
        logView.showTimeStampProperty().bind(showTimestamp.selectedProperty());

        ToggleButton tail = new ToggleButton("Tail");
        logView.tailProperty().bind(tail.selectedProperty());

        ToggleButton pause = new ToggleButton("Pause");
        logView.pausedProperty().bind(pause.selectedProperty());

        Slider rate = new Slider(0.1, 60, 60);
        logView.refreshRateProperty().bind(rate.valueProperty());
        Label rateLabel = new Label();
        rateLabel.textProperty().bind(Bindings.format("Update: %.2f fps", rate.valueProperty()));
        rateLabel.setStyle("-fx-font-family: monospace;");
        VBox rateLayout = new VBox(rate, rateLabel);
        rateLayout.setAlignment(Pos.CENTER);

        HBox controls = new HBox(
                10,
                filterLevel,
                showTimestamp,
                tail,
                pause,
                rateLayout
        );
        controls.setMinHeight(HBox.USE_PREF_SIZE);

        VBox layout = new VBox(
                10,
                controls,
                logView
        );
        VBox.setVgrow(logView, Priority.ALWAYS);
        folderField = new TextField();
        userField = new TextField();
        passField = new PasswordField();
        groupField = new TextField();
        itemField = new TextField();
        depthField = new TextField();
        partitionField = new TextField();
        dataIdField = new TextField();
        exportField = new CheckBox();
        exportParentField = new CheckBox();
        folderPermField = new TextField();
        partitionField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        partitionField.setTooltip(new Tooltip("Kill laptop with < 50, stay safe with 200 or more"));
        depthField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        itemField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        HBox userBox = new HBox(10,new Label("CS Nutzer"),userField);
        userBox.setPadding(new Insets(5,5,5,5));
        HBox passBox = new HBox(10,new Label("Passwort"), passField);
        passBox.setPadding(new Insets(5,5,5,5));
        HBox itemBox = new HBox(10,new Label("How many Items to update"), itemField);
        itemBox.setPadding(new Insets(5,5,5,5));
        itemField.setTooltip(new Tooltip("Has to be greater than the items in the folder"));
        HBox depthBox = new HBox(10,new Label("Folder depth") ,depthField);
        depthBox.setPadding(new Insets(5,5,5,5));
        depthField.setTooltip(new Tooltip("1 if only in the current folder"));
        HBox sizeBox = new HBox(10,new Label("Folder Ids"), folderField);
        sizeBox.setPadding(new Insets(5,5,5,5));
        HBox dataIdBox = new HBox(10,new Label("Data Ids"), dataIdField);
        dataIdBox.setPadding(new Insets(5,5,5,5));
        HBox groupBox = new HBox(10,new Label("Group to Remove") ,groupField);
        groupBox.setPadding(new Insets(5,5,5,5));
        HBox partitionBox = new HBox(10,new Label("Partition size") ,partitionField);
        partitionBox.setPadding(new Insets(5,5,5,5));
        HBox exportBox = new HBox(10,new Label("Export Processed Id") ,exportField);
        exportBox.setPadding(new Insets(5,5,5,5));
        HBox exportParentBox = new HBox(10, new Label("Export Parent Ids"), exportParentField);
        exportParentBox.setPadding(new Insets(5,5,5,5));
        HBox runBox = new HBox(10,btn);
        runBox.setPadding(new Insets(5,5,5,5));
        HBox folderPermBox = new HBox(10, new Label("Folder from which perm to inherit"), folderPermField);
        folderPermBox.setPadding(new Insets(5,5,5,5));
        
        Container a = new Container("Update Items with folder id");
        Container b = new Container("Update Permissions from folder");
        Container c = new Container("Update with Item ID");
        a.addNode(depthBox);
        a.addNode(itemBox);
        a.addNode(partitionBox);
        a.addNode(sizeBox);
        b.addNode(folderPermBox);
        c.addNode(dataIdBox);
        c.addNode(exportParentBox);
        VBox bottom = new VBox(userBox, passBox, groupBox ,exportBox,runBox);
        tPane.getTabs().add(a);
        tPane.getTabs().add(b);
        tPane.getTabs().add(c);
        SplitPane leftPane = new SplitPane(tPane,bottom);
        leftPane.setOrientation(Orientation.VERTICAL);
        SplitPane root = new SplitPane(leftPane,layout);
        Scene scene = new Scene(root, 300, 250);
        scene.getStylesheets().add(
            this.getClass().getResource("log-view.css").toExternalForm()
        );
        tKeeper = new TaskKeeper(logger);
        primaryStage.setOnCloseRequest((event ) -> {
            tKeeper.stopTask();
        });
        tKeeper.start();
        primaryStage.setTitle("The Right Cleaner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}

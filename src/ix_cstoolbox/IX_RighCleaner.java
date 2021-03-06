/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

/**
 *
 * @author bho
 */
public class IX_RighCleaner extends Application {
    private TextField is_dbServer, is_dbName ,remPe_dbServer, remPe_dbName, remPe_sql, remPe_group, movVerToY_dbServer, movVerToY_dbName, movVerToY_sourceFolderId, movVerToY_destFolderId,movVerToY_partitionSize, movVerToY_parallelThreads, movVer_dbServerField, movVer_dbNameField, movVer_ThreadCountField,movVer_PartitionField,movVer_dbServer,movVer_dbName, movVer_sourceFolder, movVer_dstFolder, remNodes_idField,remOwn_idField,iCatSqlField, iCatCatField, iCatDBNameField, iCatDBServerField, arch_dbServerField, arch_dbNameField, arch_dirField, arch_destDirField, updater_RightIdField, updater_DBServerField, updater_DBNameField,moveRg_mandantField, moveRg_srcFoldField,moveRg_bpField, moveRg_invoiceField,remCat_hasIdField, remCat_remIdField, remCat_fromIdField,xml_CatNameField, xml_folderField,cat_CatFromField, cat_IdField,class_IdField, class_ClassIdsField, obTemp_dbServerField, obTemp_dbNameField, obTemp_templateField, appl_nodeToCopyField,appl_folderIdsField,regionNameField,valueField,searchGroupField, folderField, userField,groupField, itemField, depthField, partitionField,dataIdField,folderPermField,catVersionField, catField;
    private PasswordField passField;
    private LogView logView;
    private Logger logger;
    private CheckBox iCatFetchFirstField,debugField,moveRg_clearClassField,moveRg_excludeCopyField,moveRg_categoriesField,moveRg_inheritField,obTemp_inheritField, exportField, exportParentField, appl_inherit;
    private final TabPane tPane = new TabPane();
    private TaskKeeper tKeeper;
    private FileChooser iCatFileChooser, iCatMappingFileChooser;
    private File iCatFile,iCatMappingFile;
    private final ArrayList<InfoStoreMapping> iCatMapping = new ArrayList<>();
    private boolean checkGlobalFields() {
        if(userField.getText() == null || userField.getText().isEmpty()) {
            
            return false;
        }
        if(passField.getText() == null ||passField.getText().isEmpty()) {
            return false;
        }
        return groupField.getText() != null;
    }
    
    private boolean checkISRechn() {
        if(is_dbServer.getText() == null || is_dbServer.getText().isEmpty()) {
            return false;
        }
        return !is_dbName.getText().isEmpty();
    }
    
    private boolean checkRemPe() {
        if(remPe_dbServer.getText() == null || remPe_dbServer.getText().isEmpty()) {
            return false;
        }
        if(remPe_dbName.getText() == null || remPe_dbName.getText().isEmpty()) {
            return false;
        }
        if(remPe_sql.getText() == null || remPe_sql.getText().isEmpty()) {
            return false;
        }
        return !remPe_group.getText().isEmpty();
    }
    
    private boolean checkMoveVerkaufToY() {
        if(movVerToY_dbServer.getText() == null || movVerToY_dbServer.getText().isEmpty()) {
            return false;
        }
        if(movVerToY_dbName.getText() == null || movVerToY_dbName.getText().isEmpty()) {
            return false;
        }  
        if(movVerToY_sourceFolderId.getText() == null || movVerToY_sourceFolderId.getText().isEmpty()) {
            return false;
        }  
        if(movVerToY_destFolderId.getText() == null || movVerToY_destFolderId.getText().isEmpty()) {
            return false;
        }  
        if(movVerToY_partitionSize.getText() == null || movVerToY_partitionSize.getText().isEmpty()) {
            return false;
        }  
        return !movVerToY_parallelThreads.getText().isEmpty();
        
    }
    
    private boolean checkMoveVerkauf() {
        if(movVer_ThreadCountField.getText() == null || movVer_ThreadCountField.getText().isEmpty()) {
            return false;
        }
        if(movVer_PartitionField.getText() == null || movVer_PartitionField.getText().isEmpty()) {
            return false;
        }
        if(movVer_dbServer.getText() == null || movVer_dbServer.getText().isEmpty()) {
            return false;
        }
        if(movVer_dbName.getText() == null || movVer_dbName.getText().isEmpty()) {
            return false;
        }
        if(movVer_sourceFolder.getText() == null || movVer_sourceFolder.getText().isEmpty()) {
            return false;
        }
        return movVer_dstFolder.getText() != null;
    }
    
    private boolean checkArchTab() {
        if(arch_dbServerField.getText() == null || arch_dbServerField.getText().isEmpty()) {
            return false;
        }
        if(arch_dbNameField.getText()== null || arch_dbNameField.getText().isEmpty()) {
            return false;
        }
        if(arch_dirField.getText() == null || arch_dirField.getText().isEmpty()) {
            return false;
        }
        if(arch_destDirField.getText() == null || arch_destDirField.getText().isEmpty()) {
            return false;
        }
        return true;
    }
    
    private boolean checkMoveRgTab() {
        if(moveRg_invoiceField.getText() == null || moveRg_invoiceField.getText().isEmpty()){
            return false;
        }
        if(moveRg_bpField.getText() == null || moveRg_bpField.getText().isEmpty()) {
            return false;
        }
        if(moveRg_mandantField.getText() == null || moveRg_mandantField.getText().isEmpty()) {
            return false;
        }
        if(movVer_dbServerField.getText() == null || movVer_dbServerField.getText().isEmpty()) {
            return false;
        }
        if(movVer_dbNameField.getText() == null || movVer_dbNameField.getText().isEmpty()) {
            return false;
        }
        return moveRg_srcFoldField.getText() != null;
    }
    
    private boolean checkRemoveCatTab() {
        if(remCat_hasIdField.getText() == null || remCat_hasIdField.getText().isEmpty()) {
            
            return false;
        }
        if(remCat_remIdField.getText() == null || remCat_remIdField.getText().isEmpty()) {
            
            return false;
        }
        return remCat_fromIdField.getText() != null;
    }
    
    private boolean checkXmlTab() {
        if(xml_CatNameField.getText() == null || xml_CatNameField.getText().isEmpty()) {
            return false;
        }
        return !(xml_folderField.getText() == null ||xml_folderField.getText().isEmpty());
    }
    
    private boolean checkSetCatTab() {
        if(cat_CatFromField.getText() == null | cat_CatFromField.getText().isEmpty()) {
            return false;
        }
        return !(cat_IdField.getText() == null || cat_IdField.getText().isEmpty());
    }
    
    private boolean checkClassifyTab() {
        if(class_IdField.getText() == null || class_IdField.getText().isEmpty() ) {
            return false;
        }
        return !(class_ClassIdsField.getText() == null || class_ClassIdsField.getText().isEmpty());
    }
    
    
    private boolean checkObjectTemplateTab(){
        if(obTemp_dbNameField.getText() == null || obTemp_dbNameField.getText().isEmpty()) {
            System.out.println("dbanme");
            return false;
        }
        if(obTemp_dbServerField.getText() == null || obTemp_dbServerField.getText().isEmpty()) {
            System.out.println("dbserver");
            return false;
        }
        return !(obTemp_templateField.getText() == null || obTemp_templateField.getText().isEmpty());
    }
    
    private boolean checkApplierTab(){
        if(appl_nodeToCopyField.getText() == null ||appl_nodeToCopyField.getText().isEmpty()) {
            return false;
        }
        if(appl_inherit == null) {
            return false;
        }
        return !(appl_folderIdsField.getText() == null || appl_folderIdsField.getText().isEmpty());
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
        if(updater_RightIdField.getText() == null) {
            return false;
        }
        if(updater_RightIdField.getText().isEmpty()) {
            updater_RightIdField.setText("0");
        }
        if(updater_DBServerField.getText() == null || updater_DBServerField.getText().isEmpty()) {
            return false;
        }
        if(updater_DBNameField.getText() == null || updater_DBNameField.getText().isEmpty()) {
            return false;
        }
        if(folderField.getText().equals("2000")){Alert alert = new Alert(AlertType.ERROR); alert.setTitle("ARE YOU MAD?!"); alert.showAndWait(); return false;}
        return !(folderField.getText() == null ||folderField.getText().isEmpty());
    }
    
    private boolean checkMiddleTab() {
        
        return !(folderPermField.getText() == null || folderPermField.getText().isEmpty());
    }
    
    private boolean checkCategoryTab(){
        if(catVersionField.getText() == null || catVersionField.getText().isEmpty()) return false;
        return !(catField.getText() == null || catField.getText().isEmpty());
    }
    
    private boolean checkICatTab() {
        if(iCatSqlField == null && iCatSqlField.getText().isEmpty()){
            return false;
        }
        if(iCatCatField == null && iCatCatField.getText().isEmpty()){
            return false;
        }
        if(iCatDBNameField == null && iCatDBNameField.getText().isEmpty()){
            return false;
        }
        if(iCatDBServerField == null && iCatDBServerField.getText().isEmpty()){
            return false;
        }
        if(iCatFile == null && !Files.exists(iCatFile.toPath())) {
            return false;
        }
        return true;
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
    
    public boolean checkSearchGroupTab(){
        if(regionNameField.getText() == null || regionNameField.getText().isEmpty()) {
            return false;
        }
        if(valueField.getText() == null || valueField.getText().isEmpty()) {
            return false;
        }
        return !(searchGroupField.getText() == null ||searchGroupField.getText().isEmpty());
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
                        Updater updater_1 = new Updater(logger, userField.getText(),passField.getText(), debugField.isSelected() , Integer.valueOf(partitionField.getText()) ,groupField.getText(), folderIds, updater_DBServerField.getText(), updater_DBNameField.getText(), Long.valueOf(updater_RightIdField.getText()), exportField.isSelected());
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
                    case "Upgrade categorywith id and version":
                        if(!checkCategoryTab()){
                            return;
                        }
                        CategoryUpdater catupdater_1 = new CategoryUpdater(logger, userField.getText(), passField.getText(), Long.valueOf(catField.getText()), Long.valueOf(catVersionField.getText()), exportField.isSelected());
                        tKeeper.addNewTask(catupdater_1);
                        break;
                    case "Search Objects with Group":
                        if(!checkSearchGroupTab()){
                            return;
                        }
                        split = searchGroupField.getText().split(",");
                        stringList = new ArrayList<>(Arrays.asList(split)); //new ArrayList is only needed if you absolutely need an ArrayList
                        //Start update
                        ArrayList<String> groupIds = new ArrayList<>();
                        for(String string : stringList ) {
                            groupIds.add(string);                    
                        }
                        SearchObjects sObjects_1 = new SearchObjects(logger, userField.getText(), passField.getText(),groupIds, regionNameField.getText(), valueField.getText(),exportField.isSelected());
                        tKeeper.addNewTask(sObjects_1);
                        break;
                    case "Apply Permissions to Folder":
                        if(!checkApplierTab()) {
                            return;
                        }
                        split = appl_folderIdsField.getText().split(",");
                        stringList = new ArrayList<>(Arrays.asList(split)); //new ArrayList is only needed if you absolutely need an ArrayList
                        //Start update
                        ArrayList<String> apply_folderIds = new ArrayList<>();
                        for(String string : stringList ) {
                            apply_folderIds.add(string);                    
                        }
                        RightApplier rApplier_1 = new RightApplier(logger, userField.getText(), passField.getText(),apply_folderIds, Long.valueOf(appl_nodeToCopyField.getText()), appl_inherit.isSelected(),exportField.isSelected());
                        tKeeper.addNewTask(rApplier_1);
                        break;
                    case "Update Objects with Template ID":
                        if(!checkObjectTemplateTab()){
                            return;
                        }
                        UpdateObjectsWithTemplate utemplate_1 = new UpdateObjectsWithTemplate(logger, userField.getText(), passField.getText(),Long.valueOf(obTemp_templateField.getText()), obTemp_dbServerField.getText(), obTemp_dbNameField.getText(), obTemp_inheritField.isSelected(),exportField.isSelected());
                        tKeeper.addNewTask(utemplate_1);
                        break;
                    case "Remove classifications based on folder":
                        if(!checkClassifyTab()) {
                            return;
                        }
                        split = class_ClassIdsField.getText().split(",");
                        stringList = new ArrayList<>(Arrays.asList(split)); //new ArrayList is only needed if you absolutely need an ArrayList
                        //Start update
                        ArrayList<Long> class_folderI = new ArrayList<>();
                        for(String string : stringList ) {
                            class_folderI.add(Long.valueOf(string));                    
                        }
                        Classify classify_1 = new Classify(logger,  userField.getText(), passField.getText(),Long.valueOf(class_IdField.getText()),  class_folderI, exportField.isSelected());
                        tKeeper.addNewTask(classify_1);
                        break;
                    case "Set Category Value to Node ID":
                        if(!checkSetCatTab()){
                            return;
                        }
                        SetCategoryToId catId_1 = new SetCategoryToId(logger,  userField.getText(), passField.getText(), Long.valueOf(cat_IdField.getText()), Long.valueOf(cat_CatFromField.getText()),  exportField.isSelected());
                        tKeeper.addNewTask(catId_1);
                        break;
                    case "Assign Category to Node from XML":
                        if(!checkXmlTab()) {
                            return;
                        }
                        CreateCategoryForCsv xml_1 = new CreateCategoryForCsv(logger, userField.getText(), passField.getText(), xml_folderField.getText(),xml_CatNameField.getText(), exportField.isSelected());
                        tKeeper.addNewTask(xml_1);
                        break;
                    case "Remove Category based on folder":
                        if(!checkRemoveCatTab()) {
                            return;
                        }
                        RemoveCategory rem_1 = new RemoveCategory(logger, userField.getText(), passField.getText(),Long.valueOf(remCat_fromIdField.getText()), Long.valueOf(remCat_hasIdField.getText()),Long.valueOf(remCat_remIdField.getText()), exportField.isSelected());
                        tKeeper.addNewTask(rem_1);
                        break;
                    case "Move Rechnungen based on Category":
                        if(!checkMoveRgTab()) {
                            return;
                        }
                        MoveRechnungen move_1 = new MoveRechnungen(logger, userField.getText(), passField.getText(),Long.valueOf(moveRg_srcFoldField.getText()),Long.valueOf(moveRg_invoiceField.getText()),moveRg_inheritField.isSelected(),moveRg_categoriesField.isSelected(),moveRg_excludeCopyField.isSelected(),moveRg_clearClassField.isSelected(),Long.valueOf(moveRg_bpField.getText()), Long.valueOf(moveRg_mandantField.getText()), movVer_dbServerField.getText(), movVer_dbNameField.getText(), debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(move_1);
                        break;
                    case "Search for Objects with Classification":
                        
                        SearchForClassi searchClass_1 = new SearchForClassi(logger, userField.getText(), passField.getText(), 556887l , exportField.isSelected());
                        tKeeper.addNewTask(searchClass_1);
                        break;
                    case "Remove Nodes":
                        String[] remNodes_split = remNodes_idField.getText().split(",");
                        ArrayList<Long> remNodes_id = new ArrayList<>();
                        for(String string : remNodes_split) {
                            remNodes_id.add(Long.valueOf(string));
                        }
                        RemoveNodes cRights_1 = new RemoveNodes(logger, userField.getText(), passField.getText(), remNodes_id,debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(cRights_1);
                        break;

                    case "Move Error":
                        CheckAlreadyArchived arch_1 = new CheckAlreadyArchived(logger, userField.getText(),  passField.getText(), debugField.isSelected(), arch_dbServerField.getText(), arch_dbNameField.getText(), arch_dirField.getText(), arch_destDirField.getText(), exportField.isSelected());
                        tKeeper.addNewTask(arch_1);
                        break;
                    case "Infostore Cat":
                        try {
                            if(iCatMapping.size()>0)iCatMapping.clear();
                            Files.readAllLines(iCatMappingFile.toPath()).stream().forEach(line -> {
                                InfoStoreMapping map = new InfoStoreMapping();
                                String[] split1 = line.split(",");
                                map.setSrc(split1[0]);
                                switch(split1[1]){
                                    case "Integer":
                                        map.setSrcType(Integer.class);
                                        break;
                                    case "String":
                                        map.setSrcType(String.class);
                                        break;
                                    case "Date":
                                        map.setSrcType(Date.class);
                                        break;
                                }
                                map.setDst(split1[2]);
                                switch(split1[3]){
                                    case "Integer":
                                        map.setDstType(Integer.class);
                                        break;
                                    case "String":
                                        map.setDstType(String.class);
                                        break;
                                    case "Date":
                                        map.setDstType(Date.class);
                                        break;
                                }
                                iCatMapping.add(map);
                            });
                            InfoCatMod iCat_1 = new InfoCatMod(logger, userField.getText(), passField.getText(), iCatDBServerField.getText(), iCatDBNameField.getText(), iCatFile, iCatSqlField.getText(), iCatMapping, iCatCatField.getText(),iCatFetchFirstField.isSelected(), debugField.isSelected(),exportField.isSelected());
                            tKeeper.addNewTask(iCat_1);    
                        } catch (IOException ex) {
                            java.util.logging.Logger.getLogger(IX_RighCleaner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        }
                        break;
                    case "Remove Owner Permissions":
                        ArrayList<Long> ids = new ArrayList<>();
                        String[] split1 = remOwn_idField.getText().split(",");
                        for(String string : split1) {
                            ids.add(Long.valueOf(string));
                        }
                        RemoveOwnerPermissions remOwner_1 = new RemoveOwnerPermissions(logger, userField.getText(), passField.getText(), ids, debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(remOwner_1);
                        break;
                    case "Move Verkauf":
                        MoveVerkauf moveVerkauf_1 = new MoveVerkauf(logger, userField.getText(), passField.getText(), movVer_dbServer.getText(), movVer_dbName.getText(), Long.valueOf(movVer_sourceFolder.getText())
                                , Long.valueOf(movVer_dstFolder.getText()), Integer.valueOf(movVer_PartitionField.getText()), Integer.valueOf(movVer_ThreadCountField.getText()), debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(moveVerkauf_1);
                        break;
                    case "Move Verkauf To Year":
                        MoveVerkaufToYear moveVerkaufToYear_1 = new MoveVerkaufToYear(logger, userField.getText(), passField.getText(), 
                                movVerToY_dbServer.getText(), movVerToY_dbName.getText(),Long.valueOf(movVerToY_sourceFolderId.getText()), Long.valueOf(movVerToY_destFolderId.getText()),
                        Integer.valueOf(movVerToY_partitionSize.getText()), Integer.valueOf(movVerToY_parallelThreads.getText()),debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(moveVerkaufToYear_1);
                        break;
                    case "Remove Permissions from Nodes":
                        //Logger logger, String user, String password, String dbServer, String dbName, String sql, String group, boolean debug, boolean export
                        if(!checkRemPe())
                        {
                            return;
                        }
                        RemovePermFromObj remPerm_1 = new RemovePermFromObj(logger, userField.getText(), passField.getText(),
                            remPe_dbServer.getText(), remPe_dbName.getText(), remPe_sql.getText(), remPe_group.getText(),
                                debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(remPerm_1);
                        break;
                    case "Infostore Rechnungen":
                        if(!checkISRechn()) {
                            return;
                        }
                        ISRechnr isrechnr_1 = new ISRechnr(logger,userField.getText(), passField.getText(), is_dbServer.getText(), is_dbName.getText(), debugField.isSelected(), exportField.isSelected());
                        tKeeper.addNewTask(isrechnr_1);
                        break;
                    default:
                        logger.error("Something went wrong");
                }
                //Clear input fields
                folderField.clear();
                itemField.clear();
                depthField.clear();
                partitionField.clear();
                dataIdField.clear();
                folderPermField.clear();
                catVersionField.clear();
                catField.clear();
                searchGroupField.clear();
                regionNameField.clear();
                valueField.clear();
                appl_nodeToCopyField.clear();
                appl_folderIdsField.clear();
                obTemp_templateField.clear();
                obTemp_dbServerField.clear();
                obTemp_dbNameField.clear();
                class_IdField.clear();
                class_ClassIdsField.clear();
                cat_IdField.clear();
                cat_CatFromField.clear();
                xml_folderField.clear();
                xml_CatNameField.clear();
                remCat_hasIdField.clear();
                remCat_remIdField.clear();
                remCat_fromIdField.clear();
                moveRg_srcFoldField.clear();
                moveRg_invoiceField.clear();
                moveRg_bpField.clear();
                updater_DBServerField.clear();
                updater_DBNameField.clear();
                updater_RightIdField.clear();
                remOwn_idField.clear();
                remNodes_idField.clear();
                movVer_dbServer.clear();
                movVer_dbName.clear();
                movVer_sourceFolder.clear();
                movVer_dstFolder.clear();
                movVer_ThreadCountField.clear();
                movVer_PartitionField.clear();
                movVer_dbServerField.clear();
                movVer_dbNameField.clear();
                movVerToY_dbServer.clear();
                movVerToY_dbName.clear();
                movVerToY_sourceFolderId.clear();
                movVerToY_destFolderId.clear();
                movVerToY_partitionSize.clear();
                movVerToY_parallelThreads.clear();
                //
                remPe_dbServer.clear();
                remPe_dbName.clear();
                remPe_sql.clear();
                remPe_group.clear();
                is_dbServer.clear();
                is_dbName.clear();
                //appl_inherit;
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
        catVersionField = new TextField();
        catField = new TextField();
        searchGroupField = new TextField();
        regionNameField = new TextField();
        valueField = new TextField();
        appl_nodeToCopyField = new TextField();
        appl_folderIdsField = new TextField();
        appl_inherit = new CheckBox();
        obTemp_templateField = new TextField();
        obTemp_dbServerField= new TextField();
        obTemp_dbNameField= new TextField();
        obTemp_inheritField = new CheckBox();
        class_IdField = new TextField();
        class_ClassIdsField = new TextField();
        cat_IdField = new TextField();
        cat_CatFromField = new TextField();
        xml_folderField = new TextField();
        xml_CatNameField = new TextField();
        remCat_hasIdField = new TextField();
        remCat_remIdField = new TextField();
        remCat_fromIdField = new TextField();
        moveRg_srcFoldField = new TextField();
        moveRg_invoiceField = new TextField();
        moveRg_inheritField = new CheckBox();
        moveRg_categoriesField = new CheckBox();
        moveRg_excludeCopyField = new CheckBox();
        moveRg_clearClassField = new CheckBox();
        moveRg_bpField = new TextField();
        moveRg_mandantField = new TextField();
        updater_DBServerField = new TextField();
        updater_DBNameField = new TextField();
        debugField = new CheckBox();
        iCatFetchFirstField = new CheckBox();
        updater_RightIdField = new TextField();
        arch_dbServerField = new TextField();
        arch_dbNameField = new TextField();
        arch_dirField = new TextField();
        arch_destDirField = new TextField();
        
        iCatSqlField = new TextField();
        iCatCatField = new TextField();
        iCatDBNameField = new TextField();
        iCatDBServerField = new TextField();
        iCatFileChooser = new FileChooser();
        
        remNodes_idField = new TextField();
        /*try {
            iCatFileChooser.setInitialDirectory(new File(IX_RighCleaner.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
        } catch (URISyntaxException ex) {
            logger.error("Couldn't set initial dir. Forget it -.-");
        }*/
        iCatMappingFileChooser = new FileChooser();
        /*try {
            iCatMappingFileChooser.setInitialDirectory(new File(IX_RighCleaner.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
        } catch (URISyntaxException ex) {
            logger.error("Couldn't set initial dir. Forget it -.-");
        }*/
        remOwn_idField = new TextField();
        
        movVer_dbServer = new TextField();
        movVer_dbName = new TextField();
        movVer_sourceFolder = new TextField();
        movVer_dstFolder = new TextField();
        movVer_ThreadCountField = new TextField();
        movVer_PartitionField = new TextField();
        
        
        movVer_dbServerField = new TextField();
        movVer_dbNameField = new TextField();
        
        movVerToY_dbServer = new TextField();
        movVerToY_dbName = new TextField();
        movVerToY_sourceFolderId = new TextField();
        movVerToY_destFolderId = new TextField();
        movVerToY_partitionSize = new TextField();
        movVerToY_parallelThreads = new TextField();
        
        //
        remPe_dbServer = new TextField();
        remPe_dbName = new TextField();
        remPe_sql = new TextField();
        remPe_group = new TextField();
        
        is_dbServer = new TextField();
        is_dbName = new TextField();
        
        movVer_ThreadCountField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        movVer_PartitionField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        
        updater_RightIdField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        
        moveRg_mandantField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        moveRg_bpField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        moveRg_invoiceField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        moveRg_srcFoldField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        remCat_fromIdField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        remCat_remIdField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        remCat_hasIdField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        cat_IdField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        obTemp_templateField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        appl_nodeToCopyField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        catVersionField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        catField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        partitionField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        partitionField.setTooltip(new Tooltip("Kill laptop with < 50, stay safe with 200 or more"));
        depthField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        itemField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        
        
        
        HBox arch_dbServerBox = new HBox(10, new Label("DB Server"), arch_dbServerField);
        arch_dbServerBox.setPadding(new Insets(5,5,5,5));
        HBox arch_dbNameBox = new HBox(10, new Label("DB Name"), arch_dbNameField);
        arch_dbNameBox.setPadding(new Insets(5,5,5,5));
        HBox arch_dirBox = new HBox(10, new Label("Vz wo die Errors liegen"), arch_dirField);
        arch_dirBox.setPadding(new Insets(5,5,5,5));
        HBox arch_destDirBox = new HBox(10, new Label("Wohin sie verschoben werden sollen"), arch_destDirField);
        arch_destDirBox.setPadding(new Insets(5,5,5,5));
        
        HBox userBox = new HBox(10,new Label("CS Nutzer"),userField);
        userBox.setPadding(new Insets(5,5,5,5));
        HBox passBox = new HBox(10,new Label("Passwort"), passField);
        passBox.setPadding(new Insets(5,5,5,5));
        HBox debugBox = new HBox(10, new Label("Debug"), debugField);
        debugBox.setPadding(new Insets(5,5,5,5));
        
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
        HBox up_dbServerBox = new HBox(10, new Label("DB Server"), updater_DBServerField);
        up_dbServerBox.setPadding(new Insets(5,5,5,5));
        HBox up_dbNameBox = new HBox(10, new Label("DB Name"), updater_DBNameField);
        up_dbNameBox.setPadding(new Insets(5,5,5,5));
        HBox up_rightIdBox = new HBox(10, new Label("Right Id"), updater_RightIdField);
        up_rightIdBox.setPadding(new Insets(5,5,5,5));
        
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
        HBox catBox = new HBox(10, new Label("Category ID"), catField);
        catBox.setPadding(new Insets(5,5,5,5));
        HBox catVerBox = new HBox(10, new Label("Category Version"), catVersionField);
        catVerBox.setPadding(new Insets(5,5,5,5));
        HBox seGroupBox = new HBox(10, new Label("Gruppensuche"), searchGroupField);
        seGroupBox.setPadding(new Insets(5,5,5,5));
        
        HBox seRegionBox = new HBox(10, new Label("Region Bsp: Attr_82554_2"), regionNameField);
        seRegionBox.setPadding(new Insets(5,5,5,5));
        
        HBox seValueBox = new HBox(10, new Label("Kategorie Wert"), valueField);
        seValueBox.setPadding(new Insets(5,5,5,5));
        
        HBox appl_folderIdsBox = new HBox(10, new Label("Folder Ids"), appl_folderIdsField);
        appl_folderIdsBox.setPadding(new Insets(5,5,5,5));
        HBox appl_nodeCopyBox  = new HBox(10, new Label("ID wovon kopiert wird"), appl_nodeToCopyField);
        appl_nodeCopyBox.setPadding(new Insets(5,5,5,5));
        HBox appl_inheritToChildBox = new HBox(10, new Label("Vererbe gesetzte Rechte auf Subitems"), appl_inherit);
        appl_inheritToChildBox.setPadding(new Insets(5,5,5,5));
        
        
        HBox objTemp_templateBox = new HBox(10, new Label("Template ID"), obTemp_templateField);
        objTemp_templateBox.setPadding(new Insets(5,5,5,5));
        HBox objTemp_dbServerBox = new HBox(10, new Label("DB Server"), obTemp_dbServerField);
        objTemp_dbServerBox.setPadding(new Insets(5,5,5,5));
        HBox objTemp_dbNameBox = new HBox(10 , new Label("Database name"), obTemp_dbNameField);
        objTemp_dbNameBox.setPadding(new Insets(5,5,5,5));
        HBox objTemp_inheritBox = new HBox(10, new Label("Inherit for every Subfolder"), obTemp_inheritField);
        objTemp_inheritBox.setPadding(new Insets(5,5,5,5));
        
        HBox class_folderIdBox = new HBox(10, new Label("Id of folder"), class_IdField);
        class_folderIdBox.setPadding(new Insets(5,5,5,5));
        
        HBox class_ClassIdsBox = new HBox(10, new Label("Ids of classifications"), class_ClassIdsField);
        class_ClassIdsBox.setPadding(new Insets(5,5,5,5));
        
        HBox cat_IdBox = new HBox(10, new Label("ID of folder"), cat_IdField);
        cat_IdBox.setPadding(new Insets(5,5,5,5));
        HBox cat_catFromBox = new HBox(10, new Label("ID of Dummy Object"), cat_CatFromField);
        cat_catFromBox.setPadding(new Insets(5,5,5,5));
        
        HBox xml_folderBox = new HBox(10, new Label("Folder of xmls"), xml_folderField);
        xml_folderBox.setPadding(new Insets(5,5,5,5));
        HBox xml_CatNameBox = new HBox(10, new Label("Category Name"), xml_CatNameField);
        xml_CatNameBox.setPadding(new Insets(5,5,5,5));
        
        HBox remCat_hasIdBox = new HBox(10, new Label("Has Category Id"), remCat_hasIdField);
        remCat_hasIdBox.setPadding(new Insets(5,5,5,5));
        HBox remCat_remIdBox = new HBox(10, new Label("ID of category to remove"), remCat_remIdField);
        remCat_remIdBox.setPadding(new Insets(5,5,5,5));
        HBox remCat_fromIdBox = new HBox(10, new Label("Id of folder root"), remCat_fromIdField);
        remCat_fromIdBox.setPadding(new Insets(5,5,5,5));
        
        HBox moveRg_srcFoldIdBox = new HBox(10, new Label("Quellordner"), moveRg_srcFoldField);
        moveRg_srcFoldIdBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_invoiceIdBox = new HBox(10, new Label("Invoice Category ID"),moveRg_invoiceField);
        moveRg_invoiceIdBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_inheritBox = new HBox(10, new Label("Inherit Permissions from destination"), moveRg_inheritField);
        moveRg_inheritBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_categoriesBox = new HBox(10, new Label("If not selected, keeps the original categories"), moveRg_categoriesField);
        moveRg_categoriesBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_excludeCopyBox = new HBox(10, new Label("Exclude documents with (copy)"), moveRg_excludeCopyField);
        moveRg_excludeCopyBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_clearClassBox = new HBox(10, new Label("Clear classification on move"), moveRg_clearClassField);
        moveRg_clearClassBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_bpBox = new HBox(10, new Label("Business Partner Category ID"), moveRg_bpField);
        moveRg_bpBox.setPadding(new Insets(5,5,5,5));
        HBox moveRg_mandantBox = new HBox(10, new Label("Mandant Category ID"), moveRg_mandantField);
        moveRg_mandantBox.setPadding(new Insets(5,5,5,5));
        
        HBox iCatSqlBox = new HBox(10, new Label("SQL Abfrage"), iCatSqlField);
        iCatSqlBox.setPadding(new Insets(5,5,5,5));
        HBox iCatCatBox= new HBox(10, new Label("Kategorie ID"), iCatCatField);
        iCatCatBox.setPadding(new Insets(5,5,5,5));
        HBox iCatDBNameBox = new HBox(10, new Label("DB Name"), iCatDBNameField);
        iCatDBNameBox.setPadding(new Insets(5,5,5,5));
        HBox iCatDBServerBox = new HBox(10, new Label("DB Server"), iCatDBServerField);
        iCatDBServerBox.setPadding(new Insets(5,5,5,5));
        Button but = new Button("Benutz mich");
        but.setOnAction(action -> {
            iCatFile = iCatFileChooser.showOpenDialog(null);
        });
        HBox iCatFileChooserBox = new HBox(10, new Label("ID Datei Auswahl"), but);
        iCatFileChooserBox.setPadding(new Insets(5,5,5,5));
        Button but2 = new Button("Mich auch");
        but2.setOnAction(action -> iCatMappingFile = iCatMappingFileChooser.showOpenDialog(null));
        HBox iCatMappingChooserBox = new HBox(10, new Label("Mapping Datei Auswahl"), but2);
        iCatMappingChooserBox.setPadding(new Insets(5,5,5,5));
        HBox iCatFetchFirstBox = new HBox(10, new Label("Fetch data first"), iCatFetchFirstField);
        iCatFetchFirstField.setPadding(new Insets(5,5,5,5));
        
        HBox remOwn_idBox = new HBox(10, new Label("IDs"), remOwn_idField);
        remOwn_idBox.setPadding(new Insets(5,5,5,5));
        
        HBox remNodes_idBox = new HBox(10, new Label("IDs"), remNodes_idField);
        remNodes_idBox.setPadding(new Insets(5,5,5,5));
        
        HBox movVer_dbServerBox = new HBox(10, new Label("DBServer"), movVer_dbServer);
        movVer_dbServerBox.setPadding(new Insets(5,5,5,5));
        HBox movVer_dbNameBox = new HBox(10, new Label("DBName"), movVer_dbName);
        movVer_dbNameBox.setPadding(new Insets(5,5,5,5));
        HBox movVer_sourceFolderBox = new HBox(10, new Label("Quell Ordner ID"), movVer_sourceFolder);
        movVer_sourceFolderBox.setPadding(new Insets(5,5,5,5));
        HBox movVer_dstFolderBox = new HBox(10, new Label("Ziel Ordner ID"), movVer_dstFolder);
        movVer_dstFolderBox.setPadding(new Insets(5,5,5,5));
        HBox movVer_ThreadCountBox = new HBox(10, new Label("Thread Count"), movVer_ThreadCountField);
        HBox movVer_PartitionBox = new HBox(10, new Label("Partition Size"), movVer_PartitionField);
        movVer_ThreadCountBox.setPadding(new Insets(5,5,5,5));
        movVer_PartitionBox.setPadding(new Insets(5,5,5,5));
        
        HBox movRG_dbServerBox  = new HBox(10, new Label("DB Server"), movVer_dbServerField);
        movRG_dbServerBox.setPadding(new Insets(5,5,5,5));
        HBox movRG_dbNameBox = new HBox(10, new Label("DB Name"), movVer_dbNameField);
        movRG_dbNameBox.setPadding(new Insets(5,5,5,5));
               
        HBox movVerToY_dbServerBox = new HBox(10, new Label("DB Server"), movVerToY_dbServer);
        HBox movVerToY_dbNameBox = new HBox(10, new Label("DB Name"),movVerToY_dbName);
        HBox movVerToY_sourceFolderIdBox = new HBox(10, new Label("Source Folder ID"), movVerToY_sourceFolderId);
        HBox movVerToY_destFolderIdBox = new HBox(10, new Label("Dest Folder ID"), movVerToY_destFolderId); 
        HBox movVerToY_partitionSizeBox = new HBox(10, new Label("Partition Size"), movVerToY_partitionSize);
        HBox movVerToY_parallelThreadsBox = new HBox(10, new Label("Thread count"), movVerToY_parallelThreads);
        movVerToY_dbServerBox.setPadding(new Insets(5,5,5,5));
        movVerToY_dbNameBox.setPadding(new Insets(5,5,5,5));
        movVerToY_sourceFolderIdBox.setPadding(new Insets(5,5,5,5));
        movVerToY_destFolderIdBox.setPadding(new Insets(5,5,5,5));
        movVerToY_partitionSizeBox.setPadding(new Insets(5,5,5,5));
        movVerToY_parallelThreadsBox.setPadding(new Insets(5,5,5,5));
        
        //remPe_dbServer, remPe_dbName, remPe_sql, remPe_group
        HBox remPe_dbServerBox, remPe_dbNameBox, remPe_sqlBox, remPe_groupBox;
        remPe_dbServerBox = new HBox(10, new Label("DB Server"), remPe_dbServer);
        remPe_dbNameBox= new HBox(10, new Label("DB Name"), remPe_dbName);
        remPe_sqlBox = new HBox(10, new Label("SQL"), remPe_sql);
        remPe_groupBox= new HBox(10, new Label("Group to remove"), remPe_group);
        remPe_dbServerBox.setPadding(new Insets(5,5,5,5));
        remPe_dbNameBox.setPadding(new Insets(5,5,5,5));
        remPe_sqlBox.setPadding(new Insets(5,5,5,5));
        remPe_groupBox.setPadding(new Insets(5,5,5,5));
        
        HBox is_dbServerBox = new HBox(10, new Label("DB Server"), is_dbServer);
        is_dbServerBox.setPadding(new Insets(5,5,5,5));
        HBox is_dbNameBox = new HBox(10, new Label("DB Name"), is_dbName);
        is_dbNameBox.setPadding(new Insets(5,5,5,5));
        Container a = new Container("Update Items with folder id");
        Container b = new Container("Update Permissions from folder");
        Container c = new Container("Update with Item ID");
        Container d = new Container("Upgrade categorywith id and version");
        Container e = new Container("Search Objects with Group");
        Container f = new Container("Apply Permissions to Folder");
        Container g = new Container("Update Objects with Template ID");
        Container h = new Container("Remove classifications based on folder");
        Container i = new Container("Set Category Value to Node ID");
        Container j = new Container("Assign Category to Node from XML");
        Container k = new Container("Remove Category based on folder");
        Container l = new Container("Move Rechnungen based on Category");
        Container m = new Container("Search for Objects with Classification");
        Container n = new Container("Remove Nodes");
        Container o = new Container("Move Error");
        Container p = new Container("Infostore Cat");
        Container r = new Container("Remove Owner Permissions");
        Container s = new Container("Move Verkauf");
        Container t = new Container("Move Verkauf To Year");
        Container u = new Container("Remove Permissions from Nodes");
        Container v = new Container("Infostore Rechnungen");
        a.addNode(partitionBox);
        a.addNode(sizeBox);
        a.addNode(up_dbServerBox);
        a.addNode(up_dbNameBox);
        a.addNode(up_rightIdBox);
        b.addNode(folderPermBox);
        c.addNode(dataIdBox);
        c.addNode(exportParentBox);
        d.addNode(catBox);
        d.addNode(catVerBox);
        e.addNode(seGroupBox);
        e.addNode(seRegionBox);
        e.addNode(seValueBox);//("Attr_82554_2":"Test")
        f.addNode(appl_folderIdsBox);
        f.addNode(appl_nodeCopyBox);
        f.addNode(appl_inheritToChildBox);
        g.addNode(objTemp_templateBox);
        g.addNode(objTemp_dbServerBox);
        g.addNode(objTemp_dbNameBox);
        g.addNode(objTemp_inheritBox);
        h.addNode(class_folderIdBox);
        h.addNode(class_ClassIdsBox);
        i.addNode(cat_IdBox);
        i.addNode(cat_catFromBox);
        j.addNode(xml_folderBox);
        j.addNode(xml_CatNameBox);
        k.addNode(remCat_hasIdBox);
        k.addNode(remCat_remIdBox);
        k.addNode(remCat_fromIdBox);
        l.addNode(moveRg_srcFoldIdBox);
        l.addNode(moveRg_invoiceIdBox);
        l.addNode(moveRg_bpBox);
        l.addNode(moveRg_mandantBox);
        l.addNode(moveRg_inheritBox);
        l.addNode(moveRg_categoriesBox);
        l.addNode(moveRg_excludeCopyBox);
        l.addNode(moveRg_clearClassBox);
        l.addNode(movRG_dbServerBox);
        l.addNode(movRG_dbNameBox);
        n.addNode(remNodes_idBox);
        o.addNode(arch_dbServerBox);
        o.addNode(arch_dbNameBox);
        o.addNode(arch_dirBox);
        o.addNode(arch_destDirBox);
        p.addNode(iCatDBServerBox);
        p.addNode(iCatDBNameBox);
        p.addNode(iCatCatBox);
        p.addNode(iCatSqlBox);
        p.addNode(iCatFileChooserBox);
        p.addNode(iCatMappingChooserBox);
        p.addNode(iCatFetchFirstBox);
        r.addNode(remOwn_idBox);
        s.addNode(movVer_dbServerBox);
        s.addNode(movVer_dbNameBox);
        s.addNode(movVer_sourceFolderBox);
        s.addNode(movVer_dstFolderBox);
        s.addNode(movVer_ThreadCountBox);
        s.addNode(movVer_PartitionBox);
        t.addNode(movVerToY_dbServerBox);
        t.addNode(movVerToY_dbNameBox);
        t.addNode(movVerToY_sourceFolderIdBox);
        t.addNode(movVerToY_destFolderIdBox);
        t.addNode(movVerToY_partitionSizeBox);
        t.addNode(movVerToY_parallelThreadsBox);
        u.addNode(remPe_dbServerBox);
        u.addNode( remPe_dbNameBox);
        u.addNode(remPe_sqlBox);
        u.addNode(remPe_groupBox);
        v.addNode(is_dbServerBox);
        v.addNode(is_dbNameBox);
        VBox bottom = new VBox(userBox, passBox, groupBox ,exportBox, debugBox,runBox);
        tPane.getTabs().add(a);
        tPane.getTabs().add(b);
        tPane.getTabs().add(c);
        tPane.getTabs().add(d);
        tPane.getTabs().add(e);
        tPane.getTabs().add(f);
        tPane.getTabs().add(g);
        tPane.getTabs().add(h);
        tPane.getTabs().add(i);
        tPane.getTabs().add(j);
        tPane.getTabs().add(k);
        tPane.getTabs().add(l);
        tPane.getTabs().add(m);
        tPane.getTabs().add(n);
        tPane.getTabs().add(o);
        tPane.getTabs().add(p);
        tPane.getTabs().add(r);
        tPane.getTabs().add(s);
        tPane.getTabs().add(t);
        tPane.getTabs().add(u);
        tPane.getTabs().add(v);
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

/**
 *
 * @author Taccu
 */
public class Container extends Tab{
    private ContentServerTask task;
    private final VBox vbox;
    public Container(String name) {
        super(name);
        vbox = new VBox();
        setContent(vbox);
    }
    public void setTask(ContentServerTask task) {
        this.task = task;
    }
    
    public void addNode(Node node) {
        vbox.getChildren().add(node);
    }    
}

package notsotiny.ui;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

/**
 * Deals with the state info panel
 * 
 * @author Mechafinch
 */
public class StateInfoPanelManager implements NodeManager {
    
    private NotSoTinyUI app;
    
    private GridPane pane;
    
    /**
     * Creates the panel
     * 
     * @param app
     */
    public StateInfoPanelManager(NotSoTinyUI app) {
        this.app = app;
    }
    
    @Override
    public void update() {
        
    }

    @Override
    public Node getNode() {
        return this.pane;
    }
}

package notsotiny.ui;

import javafx.scene.Node;

/**
 * A class that manages some part of a JFX UI
 * 
 * @author Mechafinch
 */
public interface NodeManager {
    
    /**
     * Gets the node this manages
     * 
     * @return managed node
     */
    public Node getNode();
    
    /**
     * Called when information changes
     */
    public void update();
}

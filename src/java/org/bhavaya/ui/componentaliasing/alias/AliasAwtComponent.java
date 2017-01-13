package org.bhavaya.ui.componentaliasing.alias;

import org.bhavaya.ui.componentaliasing.listeners.AbstractPopUpMenuHandler;

import java.awt.*;

/**
 * Created by ga2adaz
 * Provides a mechanism to place/use an alisable button other than where it normally calls home.
 */
public interface AliasAwtComponent<E extends Component> {

    /**
     * The identifier of the Component being aliased.
     * @return
     */
    public String getAliasedComponentIdentifier();

    /**
     * Returns the component that is acting as the alias to another component
     * @return
     */
    public E getAliasComponent();

    /**
     * uses the provided listener to provide popupmenu functionality for this AliasableAwtComponent
     * @param abstractPopUpMenuListener
     */
    public void setPopUpMenuListener(AbstractPopUpMenuHandler abstractPopUpMenuListener);

    /**
     * Returns a String which uniquely identifies this Alias button.
     * @return
     */
    public abstract String getIdentifier();

    /**
     * Returns the Alias action powering this Alias Component
     * @return
     */
    public AliasAction getAliasAction();

}

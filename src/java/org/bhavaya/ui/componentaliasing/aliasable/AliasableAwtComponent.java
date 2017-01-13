package org.bhavaya.ui.componentaliasing.aliasable;

import org.bhavaya.ui.componentaliasing.listeners.AbstractPopUpMenuHandler;

import java.awt.*;

/**
 * Created ga2adaz
 * Provides a mechanism to make Swing Components aliasable. AliasableAwtComponent are not themselves aliases but merely
 * open the underlying swing component to aliasing. This will allowing a component to appear/be used other than where it
 * usually calls home. The first use case for this is to allow the icom freeze button from the portfolio quote control
 * view to appear on pricing views.
 */
public interface AliasableAwtComponent<E extends Component>{

    /**
     * Used to identify this component. Usually it is the fully qualified class name of the action that is powering
     * the component
     * @return
     */
    public String getAliasableComponentIdentifier();

    /**
     * uses the provided listener to provide popupmenu functionality for this AliasableAwtComponent as well as . This is what will
     * @param abstractPopUpMenuListener
     */
    public void setPopUpMenuListenerListener(AbstractPopUpMenuHandler abstractPopUpMenuListener);

    /**
     * Gets the AWT component that will be used in the place of the normal component which is being made aliasable
     * @return
     */
    public E getAliasableComponent();



}

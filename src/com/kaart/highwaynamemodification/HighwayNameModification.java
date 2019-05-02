package com.kaart.highwaynamemodification;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 *
 * @author Taylor Smock
 *
 */
public class HighwayNameModification extends Plugin {
	public static final String NAME = "Highway Name Modification";
	public static final String PLUGIN_IMAGE = "addresses.png";

	public HighwayNameModification(PluginInformation info) {
		super(info);
		HighwayNameModificationLayerChangeListener listener = new HighwayNameModificationLayerChangeListener();
		MainApplication.getLayerManager().addLayerChangeListener(listener);

		AbstractAction highwayNameModificationAction = new HighwayNameChangeAction(NAME,
				ImageProvider.get(PLUGIN_IMAGE, ImageProvider.ImageSizes.MENU), listener);
		MainApplication.getMenu().dataMenu.add(highwayNameModificationAction);
	}
}

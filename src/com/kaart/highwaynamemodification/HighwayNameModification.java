package com.kaart.highwaynamemodification;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 *
 * @author Taylor Smock
 *
 */
public class HighwayNameModification extends Plugin {
	public static final String NAME = "Highway Name Modification";
	public static String PLUGIN_IMAGE = "openqa.svg";

	public HighwayNameModification(PluginInformation info) {
		super(info);
		MainApplication.getLayerManager().addLayerChangeListener(new HighwayNameModificationLayerChangeListener());
	}
}

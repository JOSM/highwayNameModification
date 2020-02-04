package com.kaart.highwaynamemodification;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 *
 * @author Taylor Smock
 *
 */
public class HighwayNameModification extends Plugin implements Destroyable {
    public static final String NAME = "Highway Name Modification";
    public static final String PLUGIN_IMAGE = "deltasignmod";
    private final HighwayNameListener listener;
    private final AbstractAction highwayNameModificationAction;

    public HighwayNameModification(PluginInformation info) {
        super(info);
        listener = new HighwayNameListener();

        DatasetEventManager.getInstance().addDatasetListener(listener, FireMode.IMMEDIATELY);
        highwayNameModificationAction = new HighwayNameChangeAction(NAME,
                ImageProvider.get(PLUGIN_IMAGE, ImageProvider.ImageSizes.MENU), listener);
        MainApplication.getMenu().dataMenu.add(highwayNameModificationAction);
    }

    @Override
    public void destroy() {
        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
        DatasetEventManager.getInstance().removeDatasetListener(listener);
        final Map<Action, Component> actions = Arrays.asList(dataMenu.getMenuComponents()).stream()
                .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                .collect(Collectors.toMap(JMenuItem::getAction, component -> component));

        for (final Entry<Action, Component> action : actions.entrySet()) {
            if (highwayNameModificationAction.equals(action.getKey())) {
                dataMenu.remove(action.getValue());
            }
        }
    }
}

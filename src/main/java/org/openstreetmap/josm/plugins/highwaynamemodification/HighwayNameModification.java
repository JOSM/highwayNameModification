// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * The POJO class for HighwayNameModification
 *
 * @author Taylor Smock
 *
 */
public class HighwayNameModification extends Plugin implements Destroyable {
    public static final String NAME = marktr("Highway Name Modification");
    public static final String PLUGIN_IMAGE = "deltasignmod";
    private final HighwayNameListener listener;
    private final JosmAction highwayNameModificationAction;

    public HighwayNameModification(PluginInformation info) {
        super(info);
        listener = new HighwayNameListener();

        DatasetEventManager.getInstance().addDatasetListener(listener, FireMode.IMMEDIATELY);
        highwayNameModificationAction = new HighwayNameChangeAction(tr(NAME), PLUGIN_IMAGE, listener);
        JMenu dataMenu = MainApplication.getMenu().dataMenu;
        MainMenu.add(dataMenu, highwayNameModificationAction);
    }

    @Override
    public void destroy() {
        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
        DatasetEventManager.getInstance().removeDatasetListener(listener);
        final Map<Action, Component> actions = Arrays.stream(dataMenu.getMenuComponents())
                .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                .collect(Collectors.toMap(JMenuItem::getAction, component -> component));

        for (final Entry<Action, Component> action : actions.entrySet()) {
            if (highwayNameModificationAction.equals(action.getKey())) {
                dataMenu.remove(action.getValue());
            }
        }
    }
}

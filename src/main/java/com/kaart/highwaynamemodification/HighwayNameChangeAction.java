// License: GPL. For details, see LICENSE file.
package com.kaart.highwaynamemodification;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The action for changing a highway name
 *
 * @author Taylor Smock
 */
public class HighwayNameChangeAction extends JosmAction {

    /** the UUID for this action class */
    private static final long serialVersionUID = -4464200665520297125L;

    private final HighwayNameListener listener;

    /**
     * Create a new action
     *
     * @param name      The name of the action
     * @param imageIcon The icon for the action
     * @param listener  The listener to call when the action is fired
     */
    public HighwayNameChangeAction(String name, String imageIcon, HighwayNameListener listener) {
        super(name, imageIcon, name, Shortcut.registerShortcut("highwaynamemodification:namechange",
                tr("HighwayNameModification: Name Change"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), false);
        this.listener = listener;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
        Collection<OsmPrimitive> selection = ds.getAllSelected();
        ModifyWays modifyWays = listener.getModifyWays();
        modifyWays.setNameChangeInformation(selection, null, true);
        modifyWays.setDownloadTask(true);
        MainApplication.worker.execute(modifyWays);
    }

}

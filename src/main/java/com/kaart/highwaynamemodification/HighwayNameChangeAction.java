/**
 *
 */
package com.kaart.highwaynamemodification;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * @author tsmock
 *
 */
public class HighwayNameChangeAction extends AbstractAction {

    /** the UUID for this action class */
    private static final long serialVersionUID = -4464200665520297125L;

    private final HighwayNameListener listener;

    public HighwayNameChangeAction(String name, ImageIcon imageIcon, HighwayNameListener listener) {
        super(name, imageIcon);
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

/**
 *
 */
package com.kaart.highwaynamemodification;

import java.util.Map;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent.DatasetEventType;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * @author Taylor Smock
 *
 */
public class HighwayNameListener implements DataSetListener {
    private ModifyWays modifyWays = ModifyWays.getInstance();

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        // Don't care
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        // Don't care
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        final OsmPrimitive osm = event.getPrimitive();
        final Map<String, String> originalKeys = event.getOriginalKeys();
        if (osm.hasKey("highway") && originalKeys.containsKey("name") && osm.hasKey("name")) {
            String newName = osm.get("name");
            String oldName = originalKeys.get("name");
            if (newName.equals(oldName))
                return;
            modifyWays.setNameChangeInformation(event.getPrimitives(), oldName);
            modifyWays.setDownloadTask(true);
            MainApplication.worker.submit(modifyWays);
        }
    }

    public ModifyWays getModifyWays() {
        return modifyWays;
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        // Don't care
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        // Don't care
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        // Don't care
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // Don't care
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        // Validation fixes don't call tagsChanged, so we call it for them.
        if (event == null || event.getEvents() == null)
            return;
        for (AbstractDatasetChangedEvent tEvent : event.getEvents()) {
            if (DatasetEventType.TAGS_CHANGED.equals(tEvent.getType())) {
                tagsChanged((TagsChangedEvent) tEvent);
            }
        }
    }

}

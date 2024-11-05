// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 * The listener for name changes
 *
 * @author Taylor Smock
 */
public class HighwayNameListener implements DataSetListener {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

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
        final String[] oldNew = getOldNewName(event);
        if (oldNew.length == 2) {
            String newName = oldNew[1];
            String oldName = oldNew[0];
            if (newName.equals(oldName))
                return;
            performTagChanges(oldName, Collections.singleton(event));
        }
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
        final Map<String, List<TagsChangedEvent>> groupedEvents = event.getEvents().stream()
                .filter(tEvent -> DatasetEventType.TAGS_CHANGED == tEvent.getType()).map(TagsChangedEvent.class::cast)
                .collect(Collectors.groupingBy(tEvent -> String.join("----xxxx----", getOldNewName(tEvent))));
        for (List<TagsChangedEvent> events : groupedEvents.values()) {
            String[] newOldName = getOldNewName(events.get(0));
            if (newOldName.length == 2) {
                String oldName = newOldName[0];
                performTagChanges(oldName, events);
            }
        }
    }

    private void performTagChanges(String oldName, Collection<TagsChangedEvent> events) {
        final Collection<OsmPrimitive> objects = events.stream().flatMap(event -> event.getPrimitives().stream())
                .collect(Collectors.toList());
        final ModifyWays modifyWays = new ModifyWays(objects, oldName, false, true, null);
        MainApplication.worker.execute(modifyWays);
    }

    private String[] getOldNewName(TagsChangedEvent event) {
        final OsmPrimitive osm = event.getPrimitive();
        final Map<String, String> originalKeys = event.getOriginalKeys();
        if (osm.hasKey("highway") && originalKeys.containsKey("name") && osm.hasKey("name")) {
            String newName = osm.get("name");
            String oldName = originalKeys.get("name");
            if (!newName.equals(oldName)) {
                return new String[] { oldName, newName };
            }
        }
        return EMPTY_STRING_ARRAY;
    }
}

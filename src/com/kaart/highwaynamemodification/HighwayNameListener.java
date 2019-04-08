/**
 *
 */
package com.kaart.highwaynamemodification;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * @author Taylor Smock
 *
 */
public class HighwayNameListener implements DataSetListener {

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
		OsmPrimitive osm = event.getPrimitive();
		Map<String, String> originalKeys = event.getOriginalKeys();
		if (osm.hasKey("highway") && originalKeys.containsKey("name") && osm.hasKey("name")) {
			String originalName = originalKeys.get("name");
			String oldName = osm.get("name");
			if (originalName.equals(oldName)) return;
			Collection<OsmPrimitive> potentialAddrChange = osm.getDataSet().getPrimitives(new Predicate<OsmPrimitive>() {
				@Override
				public boolean test(OsmPrimitive t) {
					if (originalName.equals(t.get("addr:street"))) {
						return true;
					}
					return false;
				}
			});
			Collection<OsmPrimitive> roads = osm.getDataSet().getPrimitives(new Predicate<OsmPrimitive>() {
				@Override
				public boolean test(OsmPrimitive t) {
					if (t.hasKey("highway") &&
							(originalName.equals(t.get("name")) || oldName.equals(t.get("name")))) {
						return true;
					}
					return false;
				}
			});
			changeAddrTags(osm, osm.get("name"), potentialAddrChange, roads);
		}
	}

	public void changeAddrTags(OsmPrimitive highway, String newAddrStreet, Collection<OsmPrimitive> primitives, Collection<OsmPrimitive> roads) {
		String key = HighwayNameModification.NAME.concat(".changeAddrStreetTags");
		ConditionalOptionPaneUtil.startBulkOperation(key);
		ArrayList<OsmPrimitive> toChange = new ArrayList<>();
		for (OsmPrimitive osm : primitives) {
			if (!osm.hasKey("addr:street")) continue; // TODO throw something
			DataSet ds = osm.getDataSet();
			ds.setSelected(osm);
			ds.clearHighlightedWaySegments();
			List<IPrimitive> zoomPrimitives = new ArrayList<>();
			OsmPrimitive closest = GeometryCustom.getClosestPrimitive(osm, roads);
			if (!highway.equals(closest)) continue;
			if (closest instanceof Way) {
				WaySegment tWay = GeometryCustom.getClosestWaySegment((Way) closest, osm);
				List<WaySegment> segments = new ArrayList<>();
				segments.add(tWay);
				ds.setHighlightedWaySegments(segments);
				zoomPrimitives.add(tWay.getFirstNode());
				zoomPrimitives.add(tWay.getSecondNode());
			}
			zoomPrimitives.add(osm);
			AutoScaleAction.zoomTo(zoomPrimitives);
			final int answer = ConditionalOptionPaneUtil.showOptionDialog(key,
					MainApplication.getMainFrame(), tr("{0}Should {1} be changed to {2}{3}", "<html><h3>", osm.get("addr:street"), newAddrStreet, "</h3></html>"),
					tr("Highway name changed"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null);
			switch(answer) {
			default:
				break;
			case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
			case JOptionPane.YES_OPTION:
				toChange.add(osm);
			}
		}
		ConditionalOptionPaneUtil.endBulkOperation(key);
		if (toChange.isEmpty()) return;
		ChangePropertyCommand command = new ChangePropertyCommand(toChange, "addr:street", newAddrStreet);
		MainApplication.worker.execute(new Runnable() {
			/**
			 * This is required due to there still being a lock on the dataset from the
			 * {@code TagsChangedEvent}.
			 */
			@Override
			public void run() {
				UndoRedoHandler.getInstance().add(command);
			}
		});
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
		// Don't care
	}

}

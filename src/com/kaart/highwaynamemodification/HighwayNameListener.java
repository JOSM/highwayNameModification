/**
 *
 */
package com.kaart.highwaynamemodification;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
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
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.Logging;

/**
 * @author Taylor Smock
 *
 */
public class HighwayNameListener implements DataSetListener {

	HashMap<String, HashSet<OsmPrimitive>> downloadedWays = new HashMap<>();

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
			final String originalName = originalKeys.get("name");
			final String newName = osm.get("name");
			Future<?> additionalWays = getAdditionalWays(event.getPrimitives(), originalName);
			ModifyWays modifyWays = new ModifyWays(additionalWays, new GuiWork(osm, newName, originalName));
			MainApplication.worker.submit(modifyWays);
		}
	}

	private class ModifyWays implements Runnable {
		Future<?> downloadTask;
		GuiWork guiWork;

		ModifyWays(Future<?> downloadTask, GuiWork guiWork) {
			this.downloadTask = downloadTask;
			this.guiWork = guiWork;
		}

		@Override
		public void run() {
			try {
				if (downloadTask != null) {
					downloadTask.get();
				}
				SwingUtilities.invokeAndWait(guiWork);
			} catch (InterruptedException | InvocationTargetException | ExecutionException e) {
				Logging.error(e);
			}
		}
	}

	private class GuiWork implements Runnable {
		String originalName;
		String newName;
		OsmPrimitive osm;

		GuiWork(OsmPrimitive osm, String newName, String originalName) {
			this.osm = osm;
			this.newName = newName;
			this.originalName = originalName;
		}

		@Override
		public void run() {
			final Collection<OsmPrimitive> potentialAddrChange = osm.getDataSet().getPrimitives(new Predicate<OsmPrimitive>() {
				@Override
				public boolean test(OsmPrimitive t) {
					if (originalName.equals(t.get("addr:street")))
						return true;
					return false;
				}
			});
			final Collection<OsmPrimitive> roads = osm.getDataSet().getPrimitives(new Predicate<OsmPrimitive>() {
				@Override
				public boolean test(OsmPrimitive t) {
					return (t.hasKey("highway") &&
							(originalName.equals(t.get("name")) || newName.equals(t.get("name"))));
				}
			});
			changeAddrTags(osm, osm.get("name"), potentialAddrChange, roads);
		}
	}

	public void changeAddrTags(OsmPrimitive highway, String newAddrStreet, Collection<OsmPrimitive> primitives, Collection<OsmPrimitive> roads) {
		final String key = HighwayNameModification.NAME.concat(".changeAddrStreetTags");
		ConditionalOptionPaneUtil.startBulkOperation(key);
		boolean continueZooming = true;
		final ArrayList<OsmPrimitive> toChange = new ArrayList<>();
		for (final OsmPrimitive osm : primitives) {
			if (!osm.hasKey("addr:street")) {
				continue; // TODO throw something
			}
			final DataSet ds = osm.getDataSet();
			ds.setSelected(osm);
			ds.clearHighlightedWaySegments();
			final List<IPrimitive> zoomPrimitives = new ArrayList<>();
			final OsmPrimitive closest = GeometryCustom.getClosestPrimitive(osm, roads);
			if (!highway.equals(closest)) {
				continue;
			}
			if (closest instanceof Way) {
				final WaySegment tWay = GeometryCustom.getClosestWaySegment((Way) closest, osm);
				final List<WaySegment> segments = new ArrayList<>();
				segments.add(tWay);
				ds.setHighlightedWaySegments(segments);
				zoomPrimitives.add(tWay.getFirstNode());
				zoomPrimitives.add(tWay.getSecondNode());
			}
			zoomPrimitives.add(osm);
			if (continueZooming) AutoScaleAction.zoomTo(zoomPrimitives);
			final int answer = ConditionalOptionPaneUtil.showOptionDialog(key,
					MainApplication.getMainFrame(), tr("{0}Should {1} be changed to {2}{3}", "<html><h3>", osm.get("addr:street"), newAddrStreet, "</h3></html>"),
					tr("Highway name changed"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null);
			switch(answer) {
			case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
			case JOptionPane.YES_OPTION:
				if (ConditionalOptionPaneUtil.isInBulkOperation(key) && ConditionalOptionPaneUtil.getDialogReturnValue(key) >= 0) {
					toChange.add(osm);
					continueZooming = false;
				} else {
					UndoRedoHandler.getInstance().add(new ChangePropertyCommand(osm, "addr:street", newAddrStreet));
				}
			default:
			}
			ds.clearHighlightedWaySegments();
		}
		ConditionalOptionPaneUtil.endBulkOperation(key);
		if (toChange.isEmpty()) return;
		AutoScaleAction.zoomTo(toChange);

		/**
		 * This is required due to there still being a lock on the dataset from the
		 * {@code TagsChangedEvent}.
		 */
		MainApplication.worker.submit(() ->
				UndoRedoHandler.getInstance().add(new ChangePropertyCommand(toChange, "addr:street", newAddrStreet))
		);
	}

	/**
	 * Get additional ways that have addr:street/name tags that are the same as
	 * the old highway name tag
	 * @param highway The highway whose name is changing
	 * @param name The original name of the highway
	 * @return A future which will "finish" when the additional data is downloaded.
	 * May be {@code null} if we have already downloaded the data for this layer.
	 */
	private <T extends OsmPrimitive> Future<?> getAdditionalWays(List<T> highways, String name) {
		List<T> notDownloaded = new ArrayList<>();
		for (T highway : highways) {
			if (downloadedWays.containsKey(name) && downloadedWays.get(name).contains(highway)) continue;
			HashSet<OsmPrimitive> downloadedHighways = downloadedWays.containsKey(name) ?
					downloadedWays.get(name) : new HashSet<>();
			downloadedHighways.add(highway);
			notDownloaded.add(highway);
			downloadedWays.put(name, downloadedHighways);
		}
		if (notDownloaded.isEmpty()) return null;
		final Bounds bound = new Bounds(notDownloaded.get(0).getBBox().getBottomRight());
		final DataSet ds1 = notDownloaded.get(0).getDataSet();
		for (T highway : notDownloaded) {
			final BBox bbox = highway.getBBox();
			bbox.add(bbox.getTopLeftLon() - 0.01, bbox.getTopLeftLat() + 0.01);
			bbox.add(bbox.getTopLeftLon() + 0.01, bbox.getTopLeftLat() - 0.01);
			bbox.add(bbox.getBottomRightLon() + 0.01, bbox.getBottomRightLat() - 0.01);
			bbox.add(bbox.getBottomRightLon() - 0.01, bbox.getBottomRightLat() + 0.01);
			bound.extend(bbox.getTopLeft());
			bound.extend(bbox.getBottomRight());
			for (Bounds bounding : ds1.getDataSourceBounds()) {
				if (bounding.toBBox().bounds(bbox)) return null;
			}
		}
		final StringBuilder overpassQuery = new StringBuilder();
		overpassQuery.append("[out:xml][timeout:90][bbox:{{bbox}}];(");

		overpassQuery.append("node[\"name\"=\"NAME\"];way[\"name\"=\"NAME\"];"
				.concat("relation[\"name\"=\"NAME\"];")
				.concat("node[\"addr:street\"=\"NAME\"];way[\"addr:street\"=\"NAME\"];")
				.concat("relation[\"addr:street\"=\"NAME\"];);")
				.replaceAll("NAME", name));

		overpassQuery.append("(._;>;);(._;<;);out meta;");

		final OverpassDownloadReader overpass = new OverpassDownloadReader(bound,
				OverpassDownloadReader.OVERPASS_SERVER.get(), overpassQuery.toString());

		final DownloadOsmTask download = new DownloadOsmTask();
		download.setZoomAfterDownload(false);
		DownloadParams params = new DownloadParams();
		params.withNewLayer(true);
		params.withLayerName("haMoyQ4uVVcYTJR4");
		Future<?> future = download.download(overpass, params, bound, null);

		RunnableFuture<Collection<OsmPrimitive>> mergeData = new RunnableFuture<Collection<OsmPrimitive>>() {
			private boolean canceled = false;
			private boolean done = false;
			private Collection<OsmPrimitive> primitives;
			public void run() {
				ds1.beginUpdate();
				try {
					future.get();
					DataSet dataSet = download.getDownloadedData();
					primitives = dataSet.allPrimitives();
					new DataSetMergerExtended(ds1, dataSet).merge();
				} catch (InterruptedException | ExecutionException | DataIntegrityProblemException e) {
					Logging.error(e);
				} finally {
					ds1.endUpdate();
					List<Layer> layers = MainApplication.getLayerManager().getLayers();
					for (Layer layer : layers) {
						if (params.getLayerName().equals(layer.getName())) {
							MainApplication.getLayerManager().removeLayer(layer);
						}
					}
				}
				done = true;
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				canceled = true;
				return false;
			}

			@Override
			public boolean isCancelled() {
				return canceled;
			}

			@Override
			public boolean isDone() {
				return done;
			}

			@Override
			public Collection<OsmPrimitive> get() throws InterruptedException, ExecutionException {
				while (!done) {
					this.wait();
				}
				return primitives;
			}

			@Override
			public Collection<OsmPrimitive> get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				synchronized (this) {
					this.wait(unit.toMillis(timeout));
				}
				return primitives;
			}
		};
		MainApplication.worker.submit(mergeData);
		return mergeData;
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

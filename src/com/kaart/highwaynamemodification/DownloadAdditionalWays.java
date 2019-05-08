/**
 *
 */
package com.kaart.highwaynamemodification;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.Logging;

/**
 * @author Taylor Smock
 *
 */
public class DownloadAdditionalWays {

	static HashMap<OsmDataLayer, HashMap<String, HashSet<OsmPrimitive>>> downloadedLayerWays = new HashMap<>();

	private DownloadAdditionalWays() {
		// Hide constructor
	}

	/**
	 * Get additional ways that have addr:street/name tags that are the same as
	 * the old highway name tag
	 * @param highway The highway whose name is changing
	 * @param oldNames The original name of the highway
	 * @return A future which will "finish" when the additional data is downloaded.
	 * May be {@code null} if we have already downloaded the data for this layer.
	 */
	public static <T extends OsmPrimitive> Future<?> getAdditionalWays(Collection<T> highways, String... oldNames) {
		HashSet<T> notDownloaded = new HashSet<>();
		HashSet<String> otherNames = new HashSet<>();
		HashMap<String, HashSet<OsmPrimitive>> tDownloadedWays = new HashMap<>();
		OsmDataLayer layer = MainApplication.getLayerManager().getActiveDataLayer();
		if (downloadedLayerWays.containsKey(layer))
			downloadedLayerWays.get(layer);
		HashMap<String, HashSet<OsmPrimitive>> downloadedWays = downloadedLayerWays.containsKey(layer) ? downloadedLayerWays.get(layer) : new HashMap<>();
		for (T highway : highways) {
			boolean alreadyDownloaded = false;
			for (String key : highway.keySet()) {
				if (key.contains("name") && !key.contains("tiger") && !key.contains("type") && !key.contains("base")) {
					String tName = highway.get(key);
					HashSet<OsmPrimitive> tDownloadedHighways = downloadedWays.containsKey(key.concat(tName)) ?
							downloadedWays.get(key.concat(tName)) : new HashSet<>();
					otherNames.add(tName);
					if (tDownloadedHighways.contains(highway))
						alreadyDownloaded = true;
					tDownloadedHighways.add(highway);
					tDownloadedWays.put(key.concat(tName), tDownloadedHighways);
				}
			}
			if (!alreadyDownloaded)
				notDownloaded.add(highway);
		}
		downloadedLayerWays.put(layer, downloadedWays);
		if (notDownloaded.isEmpty()) return null;
		T initialWay = notDownloaded.iterator().next();
		final Bounds bound = new Bounds(initialWay.getBBox().getBottomRight());
		final DataSet ds1 = initialWay.getDataSet();
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
		overpassQuery.append("[out:xml][timeout:15][bbox:{{bbox}}];(");

		otherNames.addAll(Arrays.asList(oldNames));
		for (String tName : otherNames) {
			// TODO if Overpass ever allows wildcard keys without wildcard values, replace this.
			overpassQuery.append("node[~\"name\"~\"^NAME$\"];way[~\"name\"~\"^NAME$\"];"
					.concat("relation[~\"name\"~\"^NAME$\"];")
					.concat("node[\"addr:street\"=\"NAME\"];way[\"addr:street\"=\"NAME\"];")
					.concat("relation[\"addr:street\"=\"NAME\"];")
					.replaceAll("NAME", tName));
		}

		overpassQuery.append(");(._;<;);(._;>;);out meta;");

		Logging.info(overpassQuery.toString());

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

			@Override
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
				downloadedWays.putAll(tDownloadedWays);
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
				synchronized (this) {
					while (!done) {
						this.wait();
					}
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
}

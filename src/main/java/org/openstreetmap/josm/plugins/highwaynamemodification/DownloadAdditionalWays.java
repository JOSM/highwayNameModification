// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.Logging;

import jakarta.annotation.Nonnull;

/**
 * Download additional highways
 * @author Taylor Smock
 */
public final class DownloadAdditionalWays {

    static HashMap<OsmDataLayer, HashMap<String, HashSet<OsmPrimitive>>> downloadedLayerWays = new HashMap<>();

    private DownloadAdditionalWays() {
        // Hide constructor
    }

    /**
     * Check if we have already downloaded a way
     *
     * @param ways Ways that we (may) have downloaded
     * @param <T>  Some class that extends {@link OsmPrimitive}
     * @return true if all of the ways have been downloaded on the same layer
     */
    public static <T extends OsmPrimitive> boolean checkIfDownloaded(Collection<T> ways) {
        boolean rValue = false;
        final Collection<Layer> layers = new ArrayList<>(MainApplication.getLayerManager().getLayers());
        downloadedLayerWays.keySet().removeIf(not(layers::contains));
        for (HashMap<String, HashSet<OsmPrimitive>> map : downloadedLayerWays.values()) {
            for (HashSet<OsmPrimitive> set : map.values()) {
                if (set.containsAll(ways)) {
                    rValue = true;
                    break;
                }
            }
        }
        return rValue;
    }

    /**
     * Get additional ways that have addr:street/name tags that are the same as the
     * old highway name tag
     *
     * @param highways The highways whose names are changing
     * @param oldNames The original name(s) of the highway
     * @param <T>      Some class that extends {@link OsmPrimitive}
     * @return A future which will "finish" when the additional data is downloaded.
     */
    @Nonnull
    public static <T extends OsmPrimitive> CompletableFuture<Collection<OsmPrimitive>> getAdditionalWays(
            @Nonnull Collection<T> highways, @Nonnull String... oldNames) {
        HashSet<T> notDownloaded = new HashSet<>();
        HashSet<String> otherNames = new HashSet<>();
        HashMap<String, HashSet<OsmPrimitive>> tDownloadedWays = new HashMap<>();
        OsmDataLayer layer = MainApplication.getLayerManager().getActiveDataLayer();
        HashMap<String, HashSet<OsmPrimitive>> downloadedWays = downloadedLayerWays.getOrDefault(layer,
                new HashMap<>());
        for (T highway : highways) {
            boolean alreadyDownloaded = false;
            for (String key : highway.keySet()) {
                if (key.contains("name") && !key.contains("tiger") && !key.contains("type") && !key.contains("base")) {
                    String tName = highway.get(key);
                    HashSet<OsmPrimitive> tDownloadedHighways = downloadedWays.getOrDefault(key.concat(tName),
                            new HashSet<>());
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
        if (notDownloaded.isEmpty())
            return CompletableFuture.completedFuture(Collections.emptyList());
        T initialWay = notDownloaded.iterator().next();
        final Bounds bound = new Bounds(initialWay.getBBox().getBottomRight());
        final DataSet ds1 = initialWay.getDataSet();
        for (T highway : notDownloaded) {
            final BBox bbox = new BBox(highway.getBBox());
            bbox.add(bbox.getTopLeftLon() - 0.01, bbox.getTopLeftLat() + 0.01);
            bbox.add(bbox.getTopLeftLon() + 0.01, bbox.getTopLeftLat() - 0.01);
            bbox.add(bbox.getBottomRightLon() + 0.01, bbox.getBottomRightLat() - 0.01);
            bbox.add(bbox.getBottomRightLon() - 0.01, bbox.getBottomRightLat() + 0.01);
            bound.extend(bbox.getTopLeft());
            bound.extend(bbox.getBottomRight());
            for (Bounds bounding : ds1.getDataSourceBounds()) {
                if (bounding.toBBox().bounds(bbox))
                    return CompletableFuture.completedFuture(Collections.emptyList());
            }
        }
        final StringBuilder overpassQuery = new StringBuilder(118);
        overpassQuery.append("[out:xml][timeout:15][bbox:{{bbox}}];(");

        otherNames.addAll(Arrays.asList(oldNames));
        for (String tName : otherNames) {
            // TODO if Overpass ever allows wildcard keys without wildcard values, replace
            // this.
            overpassQuery.append(
                    "node[~\"name\"~\"^NAME$\"];way[~\"name\"~\"^NAME$\"];".concat("relation[~\"name\"~\"^NAME$\"];")
                            .concat("node[\"addr:street\"=\"NAME\"];way[\"addr:street\"=\"NAME\"];")
                            .concat("relation[\"addr:street\"=\"NAME\"];").replace("NAME", tName));
        }

        overpassQuery.append(");(._;<;);(._;>;);out meta;");

        Logging.info(overpassQuery.toString());

        final OverpassDownloadReader overpass = new OverpassDownloadReader(bound,
                OverpassDownloadReader.OVERPASS_SERVER.get(), overpassQuery.toString());

        Supplier<Collection<OsmPrimitive>> mergeData = () -> {
            Collection<OsmPrimitive> primitives = Collections.emptyList();
            final DataSet dataSet;
            try {
                dataSet = overpass.parseOsm(NullProgressMonitor.INSTANCE);
            } catch (DataIntegrityProblemException | OsmTransferException e) {
                Logging.error(e);
                return Collections.emptyList();
            }
            try {
                ds1.beginUpdate();
                if (dataSet != null) {
                    primitives = new HashSet<>(dataSet.allPrimitives());
                    new DataSetMerger(ds1, dataSet).merge(null, false);
                    primitives = primitives.stream().map(ds1::getPrimitiveById)
                            .filter(p -> p.hasTag("name", otherNames)).collect(Collectors.toList());
                }
            } finally {
                ds1.endUpdate();
            }
            downloadedWays.putAll(tDownloadedWays);
            return primitives;
        };
        return CompletableFuture.supplyAsync(mergeData);
    }
}

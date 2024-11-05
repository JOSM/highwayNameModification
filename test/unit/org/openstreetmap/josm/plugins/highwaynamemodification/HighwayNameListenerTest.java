// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.plugins.highwaynamemodification.testutils.GuiAnswers;
import org.openstreetmap.josm.plugins.highwaynamemodification.testutils.Overpass;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.ThreadSync;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * @author Ryan Fleming
 *
 */
@BasicPreferences
@BasicWiremock
@ExtendWith(BasicWiremock.OsmApiExtension.class)
@Overpass
@Projection
@GuiAnswers
@ThreadSync
class HighwayNameListenerTest {
    @Test
    final void testTagsChanged() {
        HighwayNameListener tester = new HighwayNameListener();
        Way prim = TestUtils.newWay("highway=residential name=\"North 8th Street\"",
                new Node(new LatLon(39.084616, -108.559293)), new Node(new LatLon(39.0854611, -108.5592888)));
        DataSet newDataset = new DataSet();
        prim.getNodes().forEach(newDataset::addPrimitive);
        newDataset.addPrimitive(prim);
        newDataset.addDataSetListener(tester);
        prim.put("name", "Road 2");
        assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("addr:street"))
                .allMatch(way -> "Road 2".equals(way.get("addr:street"))));

    }

    @Test
    final void testDataChanged() {
        HighwayNameListener tester = new HighwayNameListener();
        Way prim = TestUtils.newWay("highway=residential name=\"North 8th Street\"",
                new Node(new LatLon(39.084616, -108.559293)), new Node(new LatLon(39.0854611, -108.5592888)));
        DataSet newDataset = new DataSet();
        prim.getNodes().forEach(newDataset::addPrimitive);
        newDataset.addPrimitive(prim);
        newDataset.addDataSetListener(tester);
        for (int i = 0; i < 31; i++) { // 30 is minimum single stack events to trigger a DataChangedEvent
            Way newprim = TestUtils.newWay("highway=residential name=\"North 8th Street\"",
                    new Node(new LatLon(39.084616, -108.559293 + (i / 100d))),
                    new Node(new LatLon(39.0854611, -108.5592888 + (i / 100d))));
            newprim.getNodes().forEach(newDataset::addPrimitive);
            newDataset.addPrimitive(newprim);
            newprim.put("name", "Road " + i);

        }
        assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("addr:street"))
                .allMatch(way -> "Road 2".equals(way.get("addr:street"))));
    }

    @Main
    @Test
    void testRecursiveNameChange(WireMockRuntimeInfo wireMockRuntimeInfo)
            throws ExecutionException, InterruptedException, TimeoutException {
        for (GuiAnswers.Options option : GuiAnswers.Options.values()) {
            GuiAnswers.StandardAnswers.setResponse(option, JOptionPane.YES_OPTION);
        }
        final Bounds downloadArea = new Bounds(39.6781268, -104.9597687, 39.6787585, -104.9589962);
        final DownloadOsmTask task = new DownloadOsmTask();
        Future<?> future = task.download(new BoundingBoxDownloader(downloadArea), new DownloadParams(), downloadArea,
                NullProgressMonitor.INSTANCE);
        future.get(10, TimeUnit.SECONDS);
        assertNotNull(task.getDownloadedData());
        assertFalse(task.getDownloadedData().isEmpty());
        final OsmDataLayer layer = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).get(0);
        assertNotNull(layer);
        final DataSet ds = layer.getDataSet();
        ds.addDataSetListener(new HighwayNameListener());
        final Way highway = assertInstanceOf(Way.class, ds.getPrimitiveById(628859022, OsmPrimitiveType.WAY));
        new ThreadSync.ThreadSyncExtension().threadSync(); // double to catch instances where a finishing task put more stuff in the queue
        new ChangePropertyCommand(highway, "name", "North University Boulevard").executeCommand();
        final ThreadSync.ThreadSyncExtension sync = new ThreadSync.ThreadSyncExtension();
        // catch instances where a finishing task put more stuff in the queue
        for (int i = 0; i < 10; i++) {
            sync.threadSync();
        }
        assertAll(ds.allPrimitives().stream()
                .map(p -> () -> assertNotEquals("South University Boulevard", p.get("name"))));
        assertAll(ds.allPrimitives().stream()
                .map(p -> () -> assertNotEquals("South University Boulevard", p.get("addr:street"))));
    }
}

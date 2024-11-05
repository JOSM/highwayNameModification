// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.highwaynamemodification.testutils.GuiAnswers;
import org.openstreetmap.josm.plugins.highwaynamemodification.testutils.Overpass;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.ThreadSync;
import org.openstreetmap.josm.tools.Logging;

@BasicPreferences
@BasicWiremock
@ExtendWith(BasicWiremock.OsmApiExtension.class)
@GuiAnswers
@Overpass
@Projection
@ThreadSync
class HighwayNameChangeActionTest {
    @Test
    final void testActionPerformed() {
        HighwayNameListener tester = new HighwayNameListener();
        Way prim = TestUtils.newWay("highway=residential name=\"North 8th Street\"",
                new Node(new LatLon(39.084616, -108.559293)), new Node(new LatLon(39.0854611, -108.5592888)));
        DataSet newDataset = new DataSet();
        prim.getNodes().forEach(newDataset::addPrimitive);
        newDataset.addPrimitive(prim);
        newDataset.addDataSetListener(tester);
        prim.put("name", "Road 2");
        assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("name"))
                .allMatch(way -> "Road 2".equals(way.get("name"))));
        prim.put("highway", "residential");
        assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("name"))
                .allMatch(way -> "Road 2".equals(way.get("name"))));
    }

    @Test
    final void testWayNoName() throws ExecutionException, InterruptedException, TimeoutException {
        TestLogHandler handler = new TestLogHandler();
        try {
            Logging.getLogger().addHandler(handler);
            DataSet ds = new DataSet();
            Way prim = TestUtils.newWay("highway=residential", new Node(new LatLon(39.084616, -108.559293)),
                    new Node(new LatLon(39.0854611, -108.5592888)));
            ds.addPrimitiveRecursive(prim);
            ds.setSelected(prim);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "testWayNoName", null));
            HighwayNameChangeAction action = new HighwayNameChangeAction("test", null, new HighwayNameListener());
            assertDoesNotThrow(() -> action.actionPerformed(null));
            MainApplication.worker.submit(() -> {
                /* Sync */
            }).get(10, TimeUnit.SECONDS);
            assertTrue(handler.recordList.isEmpty());
        } finally {
            Logging.getLogger().removeHandler(handler);
        }
    }

    private static final class TestLogHandler extends Handler {
        List<LogRecord> recordList = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            this.recordList.add(record);
        }

        @Override
        public void flush() {
            // Do nothing
        }

        @Override
        public void close() throws SecurityException {
            // Do nothing
        }
    }
}

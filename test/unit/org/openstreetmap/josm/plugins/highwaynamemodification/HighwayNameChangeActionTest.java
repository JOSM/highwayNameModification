// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.WireMockServer;

@BasicPreferences
class HighwayNameChangeActionTest {
    @RegisterExtension
    static JOSMTestRules rule = new JOSMTestRules().projection();
    WireMockServer wireMock = new WireMockServer(options().usingFilesUnderDirectory("test/resources/wiremock"));

    @BeforeEach
    void setUp() {
        wireMock.start();

        Config.getPref().put("download.overpass.server", wireMock.baseUrl());
        Config.getPref().putBoolean("message." + HighwayNameModification.NAME + ".downloadAdditional", false);
        Config.getPref().putInt("message." + HighwayNameModification.NAME + ".downloadAdditional" + ".value",
                JOptionPane.YES_OPTION);

    }

    @AfterEach
    void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            MainApplication.worker.submit(() -> {
                /* Sync */}).get(10, TimeUnit.SECONDS);
        } finally {
            wireMock.stop();
            Config.getPref().put("osm-server.url", Config.getUrls().getDefaultOsmApiUrl());
        }
    }

    @Test
    final void testActionPerformed() {
        HighwayNameListener tester = new HighwayNameListener();
        Way prim = TestUtils.newWay("highway=residential name=\"North 8th Street\"",
                new Node(new LatLon(39.084616, -108.559293)), new Node(new LatLon(39.0854611, -108.5592888)));
        DataSet newDataset = new DataSet();
        prim.getNodes().forEach(newDataset::addPrimitive);
        newDataset.addPrimitive(prim);
        newDataset.addDataSetListener(tester);
        wireMock.startRecording(Config.getUrls().getDefaultOsmApiUrl());
        wireMock.saveMappings();
        prim.put("name", "Road 2");
        wireMock.stopRecording();
        wireMock.saveMappings();
        assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("name"))
                .allMatch(way -> "Road 2".equals(way.get("name"))));
        wireMock.startRecording(Config.getUrls().getDefaultOsmApiUrl());
        wireMock.saveMappings();
        prim.put("highway", "residential");
        wireMock.stopRecording();
        wireMock.saveMappings();
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

// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.OsmApi;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.ThreadSync;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * @author Ryan Fleming
 *
 */
@BasicPreferences
@Projection
@OsmApi(OsmApi.APIType.FAKE)
@ThreadSync
class HighwayNameListenerTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort().usingFilesUnderDirectory("test/resources/wiremock"))
            .build();

    @BeforeEach
    void setUp() {
        Config.getPref().put("download.overpass.server", wireMock.baseUrl());
        Config.getPref().putBoolean("message." + HighwayNameModification.NAME + ".downloadAdditional", false);
        Config.getPref().putInt("message." + HighwayNameModification.NAME + ".downloadAdditional" + ".value",
                JOptionPane.YES_OPTION);

    }

    @AfterEach
    void tearDown() {
        Config.getPref().put("osm-server.url", Config.getUrls().getDefaultOsmApiUrl());
        Config.getPref().put("download.overpass.server", "https://overpass-api.de/api/");
    }

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
}

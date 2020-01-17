package com.kaart.highwaynamemodification;

import static org.junit.Assert.*;

import javax.swing.JOptionPane;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.WireMockServer;

import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;

/**
 * @author Ryan Fleming
 *
 */
public class HighwayNameListenerTest {
	 @Rule
	    public JOSMTestRules rule = new JOSMTestRules().projection();
	    WireMockServer wireMock = new WireMockServer(options().usingFilesUnderDirectory("test/resources/wiremock"));

	 @Before
	 	public void setUp()    {
	        wireMock.start();
	        
	        Config.getPref().put("osm-server.url", wireMock.baseUrl()); // TODO do overpass, not OSM server...
			Config.getPref().putBoolean("message." + HighwayNameModification.NAME +".downloadAdditional", false);
			Config.getPref().putInt("message." + HighwayNameModification.NAME + ".downloadAdditional" + ".value", JOptionPane.YES_OPTION);
			
	    }

	 @After
	    public void tearDown() {
	        wireMock.stop();
	        Config.getPref().put("osm-server.url", Config.getUrls().getDefaultOsmApiUrl());
	        
	    }

	 @Test
	 	public final void testTagsChanged() {
			HighwayNameListener tester = new HighwayNameListener();
			Way prim = TestUtils.newWay("highway=residential name=\"North 8th Street\"", new Node(new LatLon(39.084616, -108.559293)), new Node(new LatLon(39.0854611, -108.5592888)));
			DataSet newDataset = new DataSet();
			prim.getNodes().forEach(newDataset::addPrimitive);
			newDataset.addPrimitive(prim);
			newDataset.addDataSetListener(tester);
			wireMock.startRecording(Config.getUrls().getDefaultOsmApiUrl()); // TODO save mappings and remove
			prim.put("name", "Road 2");
			wireMock.stopRecording();
			wireMock.saveMappings();
			assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("addr:street")).allMatch(way -> "Road 2".equals(way.get("addr:street"))));
			
		}
	
	@Test
		public final void testDataChanged() {
		HighwayNameListener tester = new HighwayNameListener();
		Way prim = TestUtils.newWay("highway=residential name=\"North 8th Street\"", new Node(new LatLon(39.084616, -108.559293)), new Node(new LatLon(39.0854611, -108.5592888)));
		DataSet newDataset = new DataSet();
		prim.getNodes().forEach(newDataset::addPrimitive);
		newDataset.addPrimitive(prim);
		newDataset.addDataSetListener(tester);
		wireMock.startRecording(Config.getUrls().getDefaultOsmApiUrl()); // TODO save mappings and remove
		for(int i=0; i<31; i++) { //30 is minimum single stack events to trigger a DataChangedEvent
			Way newprim = TestUtils.newWay("highway=residential name=\"North 8th Street\"", new Node(new LatLon(39.084616, -108.559293+(i/100))), new Node(new LatLon(39.0854611, -108.5592888+(i/100))));
			newprim.getNodes().forEach(newDataset::addPrimitive);
			newDataset.addPrimitive(newprim);
			newprim.put("name", "Road " + i);
			
		}
		wireMock.stopRecording();
		wireMock.saveMappings();
		assertTrue(newDataset.getWays().stream().filter(way -> way.hasTag("addr:street")).allMatch(way -> "Road 2".equals(way.get("addr:street"))));
			
		}
}

package com.kaart.highwaynamemodification;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
/**
 * @author Taylor Smock & Ryan Fleming
 *
 */
public class HighwayNameModificationLayerChangeListenerTest {
   @Rule
    public JOSMTestRules rule = new JOSMTestRules().projection();

    @Test
    public void testLayerAdded() {
        /*
         * Original method passes in a layer, checks if its an OSM data layer and that
         * its not already in the listener list then adds a listener
         */
        HighwayNameModificationLayerChangeListener tester = new HighwayNameModificationLayerChangeListener();
        Layer layer = new OsmDataLayer(new DataSet(), "Layer 1", null);
        MainApplication.getLayerManager().addLayerChangeListener(tester);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(tester.getListeners().getOrDefault(layer, null));
    }
    
    @Test
    public void testLayerRemoving() {
        // removes layer which exists in the listener list
    	HighwayNameModificationLayerChangeListener tester1 = new HighwayNameModificationLayerChangeListener();
        Layer layer = new OsmDataLayer(new DataSet(), "Layer 1", null);
        MainApplication.getLayerManager().addLayerChangeListener(tester1);
        MainApplication.getLayerManager().addLayer(layer);
        MainApplication.getLayerManager().removeLayer(layer);
        assertNull(tester1.getListeners().getOrDefault(layer,null));
    }

    @Test
    public void testGetListeners() {
        /*
         * Taylor Smock
         */
        HighwayNameModificationLayerChangeListener listener = new HighwayNameModificationLayerChangeListener();
        Layer layer = new OsmDataLayer(new DataSet(), "Layer 1", null);
        MainApplication.getLayerManager().addLayerChangeListener(listener);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(listener.getListeners().get(layer));
        MainApplication.getLayerManager().removeLayer(layer);
        assertTrue(listener.getListeners().isEmpty());
    }
}
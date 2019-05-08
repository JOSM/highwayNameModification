/**
 *
 */
package com.kaart.highwaynamemodification;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * @author Taylor Smock
 *
 */
public class HighwayNameModificationLayerChangeListener implements LayerChangeListener {
	HashMap<Layer, HighwayNameListener> listeners = new HashMap<>();
	@Override
	public void layerAdded(LayerAddEvent e) {
		Layer layer = e.getAddedLayer();
		if (layer instanceof OsmDataLayer && !listeners.containsKey(layer)) {
			OsmDataLayer osmDataLayer = (OsmDataLayer) layer;
			HighwayNameListener listener = new HighwayNameListener();
			listeners.put(layer, listener);
			osmDataLayer.getDataSet().addDataSetListener(listener);
		}
	}

	@Override
	public void layerRemoving(LayerRemoveEvent e) {
		if (listeners.containsKey(e.getRemovedLayer()) && e.getRemovedLayer() instanceof OsmDataLayer) {
			OsmDataLayer osmDataLayer = (OsmDataLayer) e.getRemovedLayer();
			osmDataLayer.getDataSet().removeDataSetListener(listeners.get(osmDataLayer));
			listeners.remove(osmDataLayer);
		}
	}

	@Override
	public void layerOrderChanged(LayerOrderChangeEvent e) {
		// Don't care
	}

	public Map<Layer, HighwayNameListener> getListeners() {
		return listeners;
	}

}

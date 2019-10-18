package com.kaart.highwaynamemodification;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
public class DataSetMergerExtended extends DataSetMerger {
	DataSet targetDataSet;
	DataSet sourceDataSet;
	public DataSetMergerExtended(DataSet targetDataSet, DataSet sourceDataSet) {
		super(targetDataSet, sourceDataSet);
		this.targetDataSet = targetDataSet;
		this.sourceDataSet = sourceDataSet;
	}

	@Override
    public void merge(ProgressMonitor progressMonitor) {
        if (sourceDataSet == null)
            return;
        if (progressMonitor != null) {
            progressMonitor.beginTask(tr("Merging data..."), sourceDataSet.allPrimitives().size());
        }
        targetDataSet.beginUpdate();
        try {
            List<? extends OsmPrimitive> candidates = new ArrayList<>(targetDataSet.getNodes());
            for (Node node: sourceDataSet.getNodes()) {
                mergePrimitive(node, candidates);
                if (progressMonitor != null) {
                    progressMonitor.worked(1);
                }
            }
            candidates.clear();
            candidates = new ArrayList<>(targetDataSet.getWays());
            for (Way way: sourceDataSet.getWays()) {
                mergePrimitive(way, candidates);
                if (progressMonitor != null) {
                    progressMonitor.worked(1);
                }
            }
            candidates.clear();
            candidates = new ArrayList<>(targetDataSet.getRelations());
            for (Relation relation: sourceDataSet.getRelations()) {
                mergePrimitive(relation, candidates);
                if (progressMonitor != null) {
                    progressMonitor.worked(1);
                }
            }
            candidates.clear();
            fixReferences();

            // copy the merged layer's API version
            if (targetDataSet.getVersion() == null) {
                targetDataSet.setVersion(sourceDataSet.getVersion());
            }

            // copy the merged layer's policies and locked status
            if (sourceDataSet.getUploadPolicy() != null && (targetDataSet.getUploadPolicy() == null
                    || sourceDataSet.getUploadPolicy().compareTo(targetDataSet.getUploadPolicy()) > 0)) {
                targetDataSet.setUploadPolicy(sourceDataSet.getUploadPolicy());
            }
            if (sourceDataSet.getDownloadPolicy() != null && (targetDataSet.getDownloadPolicy() == null
                    || sourceDataSet.getDownloadPolicy().compareTo(targetDataSet.getDownloadPolicy()) > 0)) {
                targetDataSet.setDownloadPolicy(sourceDataSet.getDownloadPolicy());
            }
            if (sourceDataSet.isLocked() && !targetDataSet.isLocked()) {
                targetDataSet.lock();
            }
        } finally {
            targetDataSet.endUpdate();
        }
        if (progressMonitor != null) {
            progressMonitor.finishTask();
        }
    }
}

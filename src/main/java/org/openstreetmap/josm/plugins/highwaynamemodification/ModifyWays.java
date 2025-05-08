// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * The actual class for modifying ways
 *
 * @author Taylor Smock
 */
final class ModifyWays implements Runnable {

    /** The addr:street tag */
    static final String ADDR_STREET = "addr:street";

    private final boolean downloadTask;

    private final Collection<? extends OsmPrimitive> wayChangingName;
    private final String originalName;
    private final boolean ignoreNewName;
    private Boolean recursive;

    /**
     * Create a new {@link ModifyWays} object
     *
     * @param osmCollection    The collection of ways that are changing names
     * @param originalName     The old name of the ways
     * @param ignoreNameChange If true, don't stop if the new name is the same as
     *                         the old name
     * @param isDownloadTask {@code true} if this is called as part of a download task
     */
    ModifyWays(Collection<? extends OsmPrimitive> osmCollection, String originalName, boolean ignoreNameChange,
            boolean isDownloadTask, Boolean recursive) {
        this.wayChangingName = osmCollection;
        this.originalName = originalName == null ? "" : originalName;
        this.ignoreNewName = ignoreNameChange;
        this.downloadTask = isDownloadTask;
        this.recursive = recursive;
    }

    private static class DownloadAdditionalAsk implements Runnable {
        private boolean done;
        private boolean download;
        private Boolean recursive;

        DownloadAdditionalAsk(Boolean recursive) {
            this.recursive = recursive;
        }

        @Override
        public void run() {
            final String key = HighwayNameModification.NAME.concat(".downloadAdditional");
            final int answer = ConditionalOptionPaneUtil.showOptionDialog(key, MainApplication.getMainFrame(),
                    tr("{0}Should we download additional information for {1}? (WARNING: May be buggy!){2}",
                            "<html><h3>", HighwayNameModification.NAME, "</h3></html>"),
                    tr("Download additional information"), JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null);
            switch (answer) {
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
            case JOptionPane.YES_OPTION:
                download = true;
                if (this.recursive == null) {
                    this.recursive = JOptionPane.YES_OPTION == ConditionalOptionPaneUtil.showOptionDialog(
                            HighwayNameModification.NAME.concat(".recursive"), MainApplication.getMainFrame(),
                            tr("This will change names in the newly downloaded data as well"),
                            tr("Should we recursively change names?"), JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, null);
                }

                break;
            default:
            }
            done = true;
        }

        /**
         * Wait for the runnable to finish
         *
         * @return {@code true} if the download finished
         * @throws InterruptedException If this thread was interrupted
         */
        public boolean get() throws InterruptedException {
            while (!done) {
                synchronized (this) {
                    this.wait(100);
                }
            }
            return download;
        }
    }

    @Override
    public void run() {
        try {
            CompletableFuture<Collection<OsmPrimitive>> newWays = CompletableFuture
                    .completedFuture(Collections.emptyList());
            if (originalName != null && downloadTask && !DownloadAdditionalWays.checkIfDownloaded(wayChangingName)) {
                DownloadAdditionalAsk ask = new DownloadAdditionalAsk(this.recursive);
                GuiHelper.runInEDTAndWait(ask);
                if (ask.get()) {
                    this.recursive = ask.recursive;
                    newWays = DownloadAdditionalWays.getAdditionalWays(wayChangingName, originalName);
                }
            }
            newWays.thenApplyAsync(ignored -> {
                for (OsmPrimitive osm : wayChangingName) {
                    if (originalName != null) {
                        doRealRun(osm, originalName);
                    } else {
                        for (String key : osm.keySet()) {
                            if (key.contains("name") && !"name".equals(key)) {
                                doRealRun(osm, osm.get(key));
                            }
                        }
                    }
                }
                return ignored;
            }).thenApplyAsync(primitives -> {
                List<OsmPrimitive> toChange = primitives.stream().filter(p -> p.hasTag("name", this.originalName))
                        .collect(Collectors.toList());
                if (Boolean.TRUE.equals(this.recursive) && !toChange.isEmpty()) {
                    final ChangePropertyCommand changePropertyCommand = new ChangePropertyCommand(toChange, "name",
                            wayChangingName.iterator().next().get("name"));
                    GuiHelper.runInEDT(() -> UndoRedoHandler.getInstance().add(changePropertyCommand));
                } else if (toChange.isEmpty() || !Boolean.TRUE.equals(this.recursive)) {
                    final DataSet ds = this.wayChangingName.iterator().next().getDataSet();
                    GuiHelper.runInEDT(() -> {
                        try {
                            ds.setSelected(SubclassFilteredCollection.filter(ds.allPrimitives(),
                                    SearchCompiler.compile(this.originalName)));
                            TodoHelper.addTodoItems();
                        } catch (SearchParseError searchParseError) {
                            throw new JosmRuntimeException(searchParseError);
                        }
                    });
                }
                return primitives;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logging.error(e);
        }
    }

    private static void doRealRun(final OsmPrimitive osm, final String name) {
        final String newName = osm.get("name");
        final Collection<OsmPrimitive> potentialAddrChange = osm.getDataSet()
                .getPrimitives(t -> name.equals(t.get(ADDR_STREET)));
        final Collection<OsmPrimitive> roads = osm.getDataSet().getPrimitives(
                t -> (t.hasKey("highway") && (name.equals(t.get("name")) || newName.equals(t.get("name")))));
        changeAddrTags(osm, name, potentialAddrChange, roads);
    }

    /**
     * Change the address tags of all buildings near the highway
     *
     * @param highway       The highway which changed names
     * @param oldAddrStreet The old name of the highway
     * @param primitives    The building primitives with addr:street tags
     * @param roads         The connecting roads to the highway with the highway's
     *                      old name
     */
    private static void changeAddrTags(OsmPrimitive highway, String oldAddrStreet, Collection<OsmPrimitive> primitives,
            Collection<OsmPrimitive> roads) {
        if (primitives == null || primitives.isEmpty()) {
            primitives = highway.getDataSet()
                    .getPrimitives(t -> t.hasKey(ADDR_STREET) && oldAddrStreet.equals(t.get(ADDR_STREET)));
        }
        if (roads == null || roads.isEmpty()) {
            roads = highway.getDataSet()
                    .getPrimitives(t -> t.hasKey("highway") && t.hasKey("name") && oldAddrStreet.equals(t.get("name")));
        }
        CreateGuiAskDialog dialog = new CreateGuiAskDialog(highway, primitives, roads);
        GuiHelper.runInEDTAndWait(dialog);
    }

    protected static class CreateGuiAskDialog implements Runnable {
        OsmPrimitive highway;
        Collection<OsmPrimitive> primitives;
        Collection<OsmPrimitive> roads;

        public CreateGuiAskDialog(OsmPrimitive highway, Collection<OsmPrimitive> primitives,
                Collection<OsmPrimitive> roads) {
            this.highway = highway;
            final Map<OsmPrimitive, List<OsmPrimitive>> ways = primitives.parallelStream()
                    .collect(Collectors.groupingBy(osm -> Geometry.getClosestPrimitive(osm, roads)));
            this.primitives = ways.getOrDefault(highway, Collections.emptyList());
            this.roads = new HashSet<>(roads); // Make copy to avoid expensive calls in FilteredCollection
        }

        @Override
        public void run() {
            if (primitives.isEmpty()) {
                return;
            }
            String newAddrStreet = highway.get("name");
            final String key = HighwayNameModification.NAME.concat(".changeAddrStreetTags");
            ConditionalOptionPaneUtil.startBulkOperation(key);
            boolean continueZooming = true;
            final ArrayList<OsmPrimitive> toChange = new ArrayList<>();
            final DataSet ds = primitives.iterator().next().getDataSet();
            final Collection<OsmPrimitive> initialSelection = ds.getSelected();
            int i = 0;
            for (final OsmPrimitive osm : this.primitives) {
                i++;
                if (!osm.hasKey(ADDR_STREET) || osm.get(ADDR_STREET).equals(newAddrStreet)) {
                    throw new IllegalStateException("Primitive does not match expected state");
                }
                ds.setSelected(osm);
                ds.clearHighlightedWaySegments();
                final List<IPrimitive> zoomPrimitives = new ArrayList<>();
                if (this.highway instanceof Way) {
                    final WaySegment tWay = Geometry.getClosestWaySegment((Way) this.highway, osm);
                    ds.setHighlightedWaySegments(Collections.singleton(tWay));
                    zoomPrimitives.add(tWay.getFirstNode());
                    zoomPrimitives.add(tWay.getSecondNode());
                }
                zoomPrimitives.add(osm);
                if (continueZooming)
                    AutoScaleAction.zoomTo(zoomPrimitives);
                final int answer = ConditionalOptionPaneUtil.showOptionDialog(key, MainApplication.getMainFrame(),
                        tr("{0}Should {1} be changed to {2}{3}", "<html><h3>", osm.get(ADDR_STREET), newAddrStreet,
                                "</h3></html>"),
                        tr("Highway name changed ({0}/{1})", i, primitives.size()), JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null);
                switch (answer) {
                case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
                case JOptionPane.YES_OPTION:
                    if (ConditionalOptionPaneUtil.isInBulkOperation(key)
                            && ConditionalOptionPaneUtil.getDialogReturnValue(key) >= 0) {
                        toChange.add(osm);
                        continueZooming = false;
                    } else {
                        UndoRedoHandler.getInstance().add(new ChangePropertyCommand(osm, ADDR_STREET, newAddrStreet));
                    }
                    break;
                default:
                }
                ds.clearHighlightedWaySegments();
            }
            ConditionalOptionPaneUtil.endBulkOperation(key);
            ds.setSelected(initialSelection);
            if (toChange.isEmpty())
                return;
            AutoScaleAction.zoomTo(toChange);

            UndoRedoHandler.getInstance().add(new ChangePropertyCommand(toChange, ADDR_STREET, newAddrStreet));
        }
    }
}

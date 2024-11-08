// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JMenu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

@BasicPreferences
@Main
class HighwayNameModificationTest {
    PluginInformation info;
    HighwayNameModification plugin;

    private static final String VERSION = "no-such-version";

    /**
     * @throws PluginException if the plugin could not be loaded
     */
    @BeforeEach
    void setUp() throws PluginException {
        final InputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        info = new PluginInformation(in, "HighwayNameModification", null);
        info.localversion = VERSION;
    }

    @Test
    final void testHighwayNameModification()
            throws SecurityException, IllegalArgumentException, ExceptionInInitializerError {
        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
        final int dataMenuSize = dataMenu.getMenuComponentCount();
        plugin = new HighwayNameModification(info);
        assertEquals(dataMenuSize + 1, dataMenu.getMenuComponentCount());
        plugin.destroy();
        assertEquals(dataMenuSize, dataMenu.getMenuComponentCount());
    }

}

package com.kaart.highwaynamemodification;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.swing.JMenu;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.WireMockServer;


public class HighwayNameModificationTest {
	@Rule
	public JOSMTestRules test = new JOSMTestRules().preferences().main();

	public PluginInformation info;
	public HighwayNameModification plugin;
	
	private static final String VERSION = "no-such-version";
	WireMockServer wireMock = new WireMockServer(options().usingFilesUnderDirectory("test/resources/wiremock"));
	/**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws ExceptionInInitializerError, Exception {
        wireMock.start();
        final InputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        info = new PluginInformation(in, "HighwayNameModification", null);
        info.localversion = VERSION;
        
    }

    @After
    public void tearDown() {
        wireMock.stop();
    }
    
	
	@Test
	public final void testHighwayNameModification() 
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,  ExceptionInInitializerError {
	        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
			final int dataMenuSize = dataMenu.getMenuComponentCount();
			plugin = new HighwayNameModification(info);
			assertEquals(dataMenuSize+ 1, dataMenu.getMenuComponentCount());
			plugin.destroy();
			assertEquals(dataMenuSize, dataMenu.getMenuComponentCount());

	       
	}

	

}

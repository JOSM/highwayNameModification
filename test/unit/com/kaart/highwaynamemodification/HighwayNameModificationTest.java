package com.kaart.highwaynamemodification;

import static org.junit.Assert.*;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;


import javax.swing.JMenu;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.JOSMTestRules;


public class HighwayNameModificationTest {
	@Rule
	public JOSMTestRules test = new JOSMTestRules().preferences().main();

	public PluginInformation info;
	public HighwayNameModification plugin;
	
	private static final String VERSION = "no-such-version";
	
	/**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws ExceptionInInitializerError, Exception {

        final InputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        info = new PluginInformation(in, "HighwayNameModification", null);
        info.localversion = VERSION;
        
    }


    
	
	@Test
	public final void testHighwayNameModification() 
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,  ExceptionInInitializerError {
	        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
			final int dataMenuSize = dataMenu.getMenuComponentCount();
			plugin = new HighwayNameModification(info);
			assertEquals(dataMenuSize + 1, dataMenu.getMenuComponentCount());
			plugin.destroy();
			assertEquals(dataMenuSize, dataMenu.getMenuComponentCount());

	       
	}

	

}

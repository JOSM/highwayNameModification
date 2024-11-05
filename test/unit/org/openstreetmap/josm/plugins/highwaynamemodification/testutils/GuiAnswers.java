// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification.testutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.plugins.highwaynamemodification.HighwayNameModification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@BasicPreferences
@ExtendWith(GuiAnswers.StandardAnswers.class)
public @interface GuiAnswers {
    enum Options {
        DOWNLOAD_ADDITIONAL("downloadAdditional"), RECURSIVE("recursive"), CHANGE_ADDR_STREET_TAGS(
                "changeAddrStreetTags");

        private final String key;

        Options(String key) {
            this.key = key;
        }
    }

    class StandardAnswers implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) {
            for (Options option : Options.values()) {
                Config.getPref().putBoolean("message." + HighwayNameModification.NAME + '.' + option.key, false);
                setResponse(option, JOptionPane.NO_OPTION);
            }
        }

        /**
         * Set the response
         * @param option The response dialog to set the response for
         * @param value The new value to respond with ({@link JOptionPane#NO_OPTION}, {@link JOptionPane#YES_OPTION})
         */
        public static void setResponse(Options option, int value) {
            Config.getPref().putInt("message." + HighwayNameModification.NAME + '.' + option.key + ".value", value);
        }
    }
}

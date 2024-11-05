// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification.testutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@BasicWiremock
@ExtendWith(Overpass.Extension.class)
public @interface Overpass {
    class Extension implements BeforeEachCallback, AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) {
            Config.getPref().put("download.overpass.server", "https://invalid.de/api/");
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            final WireMockRuntimeInfo wireMockRuntimeInfo = context
                    .getStore(ExtensionContext.Namespace.create(BasicWiremock.WireMockExtension.class))
                    .get(BasicWiremock.WireMockExtension.class, BasicWiremock.WireMockExtension.class).getRuntimeInfo();
            Config.getPref().put("download.overpass.server", wireMockRuntimeInfo.getHttpBaseUrl() + "/api/");
        }
    }
}

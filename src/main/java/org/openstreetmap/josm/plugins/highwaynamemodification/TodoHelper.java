// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.highwaynamemodification;

import java.lang.reflect.Field;

import javax.swing.AbstractListModel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Helper for the TODO plugin (so we don't <i>have</i> to depend upon it)
 */
public final class TodoHelper {
    private static final Class<? extends ToggleDialog> CLASS;
    private static final Field ACT_ADD;
    private static final Field MODEL;
    static {
        Class<? extends ToggleDialog> clazz = null;
        Field actAdd = null;
        Field model = null;
        try {
            final PluginInformation info = PluginInformation.findPlugin("todo");
            if (info != null) {
                clazz = (Class<? extends ToggleDialog>) Class.forName("org.openstreetmap.josm.plugins.todo.TodoDialog",
                        false, info.getClass().getClassLoader());
                actAdd = clazz.getDeclaredField("actAdd");
                model = clazz.getDeclaredField("model");
                ReflectionUtils.setObjectsAccessible(actAdd, model);
            }
        } catch (ClassNotFoundException | PluginException | NoSuchFieldException classNotFoundException) {
            Logging.trace(classNotFoundException);
        }
        ACT_ADD = actAdd;
        MODEL = model;
        CLASS = clazz;
    }

    public static void addTodoItems() {
        if (CLASS == null || MODEL == null || ACT_ADD == null) {
            return;
        }
        final ToggleDialog todoDialog = MainApplication.getMap().getToggleDialog(CLASS);
        try {
            AbstractListModel<?> m = (AbstractListModel<?>) MODEL.get(todoDialog);
            if (m.getSize() == 0) {
                JosmAction a = (JosmAction) ACT_ADD.get(todoDialog);
                a.actionPerformed(null);
            }
        } catch (ReflectiveOperationException e) {
            Logging.error(e);
        }
    }
}

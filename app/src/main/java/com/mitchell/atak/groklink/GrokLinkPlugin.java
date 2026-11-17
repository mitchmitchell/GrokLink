
package com.mitchell.atak.groklink;

import android.content.Context;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

/**
 * GrokLink plugin entry point.
 *
 * ATAK finds this class through assets/plugin.xml, which names it as the
 * implementation of gov.tak.api.plugin.IPlugin. ATAK constructs it once with
 * an IServiceController, then calls onStart() when the plugin loads and
 * onStop() when it unloads.
 *
 * Pattern source: samples/plugintemplate (PluginTemplate.java) in the
 * ATAK-CIV 5.8.0.10 SDK. The structure below is kept as close to the
 * template as possible so it is easy to compare.
 */
public class GrokLinkPlugin implements IPlugin {

    private final IServiceController serviceController;

    /** The plugin's own context: use it for the plugin's layouts and resources. */
    private Context pluginContext;

    /** ATAK's UI service: toolbar buttons and drop-down panes. */
    private final IHostUIService uiService;

    private final ToolbarItem toolbarItem;
    private Pane grokPane;

    public GrokLinkPlugin(IServiceController serviceController) {
        this.serviceController = serviceController;

        // ATAK integration point: the plugin context comes from the
        // PluginContextProvider service (plugintemplate does the same).
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        // ATAK integration point: the host UI service adds toolbar items
        // and shows panes.
        uiService = serviceController.getService(IHostUIService.class);

        // The identifier must stay stable (the package name), otherwise ATAK
        // loses the button's position when the user rearranges the toolbar.
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                }).setIdentifier(pluginContext.getPackageName())
                .build();
    }

    @Override
    public void onStart() {
        if (uiService == null)
            return;
        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        if (uiService == null)
            return;
        // Close the pane and remove the button so nothing is left behind
        // when ATAK unloads or reloads the plugin.
        if (grokPane != null && uiService.isPaneVisible(grokPane))
            uiService.closePane(grokPane);
        uiService.removeToolbarItem(toolbarItem);
    }

    private void showPane() {
        if (grokPane == null) {
            // ATAK integration point: inflate plugin layouts with
            // PluginLayoutInflater and the plugin context, never ATAK's.
            grokPane = new PaneBuilder(PluginLayoutInflater.inflate(pluginContext,
                    R.layout.main_layout, null))
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                    .build();
        }
        if (!uiService.isPaneVisible(grokPane)) {
            uiService.showPane(grokPane, null);
        }
    }
}

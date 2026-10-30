package com.windowmarkers;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

/** Display coordinates in our own account/profile storage, never Bank Tags settings. */
final class BankPresetPositions
{
    @Inject ConfigManager configs;
    @Inject Gson gson;

    static final class Layout
    {
        Map<String, int[]> cells = new LinkedHashMap<>();
    }

    String key(String view, int width)
    {
        if (configs == null || configs.getProfile() == null || view == null) return null;
        return "presetPositions_" + configs.getProfile().getId() + "_"
            + UUID.nameUUIDFromBytes((view + "/" + width).getBytes(StandardCharsets.UTF_8));
    }

    Layout read(String account, String key)
    {
        Layout result = null;
        if (account != null && key != null)
        {
            String raw = configs.getConfiguration("windowmarkers", account, key);
            try { result = raw == null ? null : gson.fromJson(raw, Layout.class); }
            catch (JsonParseException ignored) { /* Fall back to the generated setup. */ }
        }
        if (result == null) result = new Layout();
        if (result.cells == null || result.cells.size() > 4000) result.cells = new LinkedHashMap<>();
        result.cells.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null
            || e.getValue().length != 2 || e.getValue()[0] < 0 || e.getValue()[0] > 4000
            || e.getValue()[1] < 0 || e.getValue()[1] > 50000);
        return result;
    }

    void save(String account, String key, Layout layout)
    {
        if (account != null && key != null && configs.getProfile() != null
            && key.startsWith("presetPositions_" + configs.getProfile().getId() + "_")
            && account.equals(configs.getRSProfileKey()))
            configs.setConfiguration("windowmarkers", account, key, gson.toJson(layout));
    }
}

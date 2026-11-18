package com.windowmarkers;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
final class BankLayouts
{
	static final int MAX_MARKERS = 250;
	private final ConfigManager configs;
	private final Gson gson;
	CustomPresets customPresets = new CustomPresets();
	final List<Marker> markers = new ArrayList<>();
	String accountKey;
	String layoutKey;
	LayoutCatalog catalog = new LayoutCatalog();
	MarkerHistory history = new MarkerHistory();
	final java.util.Map<String, MarkerHistory> histories = new java.util.LinkedHashMap<>();

	@Inject
	BankLayouts(ConfigManager configs, Gson gson)
	{
		this.configs = configs;
		this.gson = gson;
	}

	void load(java.util.function.Consumer<String> status)
	{
		customPresets = new CustomPresets();
		markers.clear();
		accountKey = configs.getRSProfileKey();
		catalog = new LayoutCatalog();
		if (accountKey != null && baseLayoutKey() != null)
		{
			try
			{
				String rawCatalog = configs.getConfiguration("windowmarkers", accountKey, baseLayoutKey() + "_catalog");
				LayoutCatalog saved = rawCatalog == null ? null : gson.fromJson(rawCatalog, LayoutCatalog.class);
				if (saved != null)
				{
					catalog = saved;
					catalog.normalize();
				}
				String templates = configs.getConfiguration("windowmarkers", accountKey,
						baseLayoutKey() + "_customPresets");
				CustomPresets library = templates == null ? null : gson.fromJson(templates, CustomPresets.class);
				if (library != null)
				{
					library.normalize();
					customPresets = library;
				}
			}
			catch (JsonParseException ex)
			{
				status.accept("Could not read named layouts; using Default.");
			}
		}
		layoutKey = currentLayoutKey();
		String raw = accountKey == null || layoutKey == null ? null
				: configs.getConfiguration("windowmarkers", accountKey, layoutKey);
		readMarkers(raw, status);
		String historyKey = accountKey + "/" + layoutKey;
		if (histories.size() > 40)
		{
			histories.clear();
		}
		history = histories.computeIfAbsent(historyKey, k -> new MarkerHistory());
		String snapshot = gson.toJson(markers);
		if (!history.matches(snapshot))
		{
			history.reset(snapshot);
		}
	}

	void readMarkers(String raw, java.util.function.Consumer<String> status)
	{
		try
		{
			Marker[] saved = raw == null ? null : gson.fromJson(raw, Marker[].class);
			if (saved != null)
			{
				for (Marker marker : saved)
				{
					if (marker != null && marker.valid() && markers.size() < MAX_MARKERS)
					{
						markers.add(marker);
					}
				}
			}
		}
		catch (JsonParseException ex)
		{
			status.accept("Saved markers could not be read.");
		}
	}

	String baseLayoutKey()
	{
		return configs.getProfile() == null ? null : "markersV3_profile_" + configs.getProfile().getId();
	}

	String currentLayoutKey()
	{
		String base = baseLayoutKey();
		return base == null ? null : catalog.storageKey(base);
	}

	boolean currentLayout()
	{
		return accountKey != null && layoutKey != null && accountKey.equals(configs.getRSProfileKey())
				&& layoutKey.equals(currentLayoutKey());
	}

	void save(boolean record)
	{
		String snapshot = gson.toJson(markers);
		if (record)
		{
			history.record(snapshot);
		}
		configs.setConfiguration("windowmarkers", accountKey, layoutKey, snapshot);
	}

	void savePresets()
	{
		configs.setConfiguration("windowmarkers", accountKey, baseLayoutKey() + "_customPresets",
				gson.toJson(customPresets));
	}
}

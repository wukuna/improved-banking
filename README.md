# Improved Banking

Find bank items by name, category or activity. Preview gear and inventory setups, save your own views, and organize items with named, colored groups. Adjust the bank layout, move its controls and capture full-tab screenshots.

**Beta:** categories and activity setups are still being refined. The Wiki-based catalogue is bundled with the plugin and updated through plugin releases; it is not fetched live while you play.

![Barrows activity view with equipment, inventory and suggested runes](docs/media/barrows-activity-view.png)

An example Barrows view. Equipment and inventory sections retain item quantities and distinguish missing items from owned alternatives.

To hide the sidebar icon while keeping the plugin running, enable **Hide side panel** in RuneLite’s regular plugin settings. Turn it off there to restore the panel.

## What you can do

- **Find items:** search categories, activities, item uses and saved groups. Optionally add those matches to the bank’s native search.
- **Plan an activity:** open equipment and inventory views with guide context and owned alternatives.
- **Save a personal view:** copy a preset, edit its items and groups, and drag its displayed items into place. Your stored bank order stays unchanged.
- **Mark and label groups:** link markers to items or draw an area, then customize borders, backgrounds and labels.
- **Customize the bank:** drag to move and resize it, with item columns that fit automatically. Alt-drag compact controls into the side or bottom racks.
- **Capture a bank view:** save the complete current tab or search results with item icons, quantities and a legend.
- **Explore optional GE tools:** search categories and activities, with supported guide tiers, prices, owned-item and requirement information.

## Getting started

1. Install **Improved Banking** from RuneLite’s Plugin Hub.
2. Enable the built-in **Bank Tags** plugin before using preset views. **Bank Tag Layouts** is optional.
3. Open your bank and the square-frame Improved Banking sidebar icon.
4. In **Legend**, search an activity or category and choose **Show in bank**. **Add to Legend** saves its group; **Clear bank filter** returns to ordinary bank contents.
5. Use **Legend → Saved views → Save as custom view** to make an editable copy. Commit editor changes with **Save changes**.

Use **Custom** to create groups and markers. **Tools** contains bank sizing, screenshots and marker layouts. The camera button beside the bank’s X also saves a bank capture.

## Controls and saving

**Alt-drag** moves bank buttons when **Customize bank view** is enabled. **Tools → Reset bank buttons** restores their default arrangement. Ctrl+Alt remains available for RuneLite’s overlay controls.

Enable **Customize bank view** in the plugin’s **Bank layout** settings, then drag the bank title to move the window or its lower-right grip to resize it. Columns fit the available width automatically; item icons keep their normal size. These controls work in resizable game modes and save separately for each profile. **Tools → Customize bank view → Reset bank window position** returns the window to its native position while keeping your chosen size.

Customizing the bank moves native controls into the scrollbar area; use the mouse wheel over items to scroll. Alt-drag controls to an empty spot along the right, left or bottom rack. Buttons stay where you drop them, leaving your chosen gaps; dropping onto another button cancels the move. **Share bank tag strip** places scrollable tags above a divider and bank controls below it. Short banks temporarily return controls that cannot fit to the bottom, preserving their saved rack positions. **Preview worn equipment** is a separate option that adds a gear-and-bonuses card when hovering the worn-equipment button; clicking still opens the native equipment screen.

Drag the left-strip divider directly to give Bank Tags more space. Its highest position is the original halfway split. Its lower limit reserves room for every button below it, including gaps you have left between buttons. The divider position is saved with your button arrangement; a smaller bank temporarily limits it without losing the saved position. Turn off **Adjustable Bank Tags divider** to remove its bar and drag handle while retaining the saved split. The remaining side borders match the right rack.

Markers and custom views are stored separately by character and RuneLite profile. Preset item positions belong to the displayed view; they do not move the real bank contents. Marker Undo/Redo is session history, while saved groups and views persist.

Bank screenshots are saved locally under RuneLite’s `screenshots/<character>/bank-markers` folder. They are not automatically uploaded.

## Compatibility

Bank Tags is required for preset views. Bank Tag Layouts can remain enabled; its layouts and Improved Banking’s view positions are separate.

**Flexible Bank** and **Bank Screenshot** are declared conflicts because their features overlap the integrated bank layout and capture controls. Existing settings are retained. Inventory Setups remains a separate personal-setup workflow.

Category membership is organizational information, not a guarantee of a complete or best-in-slot setup. Junk and sell-candidate review covers documented items and explains their uses; the plugin does not automatically sell, discard, equip or withdraw items.

## Detailed guide and support

See the [full guide](docs/guide.md) for search syntax, activity setups, custom views, marker controls, saving and compatibility.

[Report an issue or suggest a feature](https://github.com/wukuna/improved-banking/issues). Include the item/activity or affected bank control, other bank plugins and a screenshot when useful.

## License and attribution

Original Java code uses [BSD 2-Clause](LICENSE). Adapted Flexible Bank code retains its license and credit. Bundled Wiki-derived data uses **CC BY-NC-SA 3.0**; see [Wiki attribution](src/main/resources/com/windowmarkers/WIKI_DATA_ATTRIBUTION.md). Resource-pack artwork is not redistributed. Additional credits are retained in the [full guide](docs/guide.md#inspiration-and-recipe).

package com.windowmarkers;

import java.awt.Color;
import net.runelite.client.config.*;

@ConfigGroup("windowmarkers")
public interface WindowMarkersConfig extends Config
{
    @ConfigSection(name="Labels and highlighting",description="Marker visibility, labels and highlighting",position=0)
    String appearance="appearance";
    @ConfigSection(name="Drawing defaults",description="Defaults for new markers",position=1,closedByDefault=true)
    String drawing="drawing";
    @ConfigSection(name="Bank layout",description="Bank size, tabs and deposit controls",position=2,closedByDefault=true)
    String bankLayout="bankLayout";
    @ConfigSection(name="Screenshots",description="Local bank image captures",position=3,closedByDefault=true)
    String screenshots="screenshots";
    @ConfigSection(name="Grand Exchange search",description="Optional category and Wiki setup searches in the GE",position=4,closedByDefault=true)
    String ge="ge";
    @ConfigItem(section=ge,keyName="geCatalogueSearch",name="Enable Grand Exchange search",description="Search activities, equipment slots and categories, e.g. toa head, vorkath ranged or mining tools. Only GE-tradeable items appear. The catalogue takes a moment to load when enabled.",position=0)
    default boolean geCatalogueSearch() { return false; }
    @ConfigItem(section=ge,keyName="geActivityFilter",name="Activity items",description="All, Owned, Not owned or Next guide upgrade. Applies only to activity queries. Open your bank once this login; inventory and equipped items are included. You can also append owned, unowned or next upgrade to an activity search.",position=1)
    default GeActivityFilter.Mode geActivityFilter() { return GeActivityFilter.Mode.ALL; }
    @ConfigItem(section=ge,keyName="geHideWorseOwned",name="Hide worse than owned",description="Hide verified lower guide choices when usable owned equipment covers every matching role. Preserves conditional, set, specialist and unranked alternatives. Requires a bank snapshot this login.",position=2)
    default boolean geHideWorseOwned() { return false; }
    @ConfigItem(section=ge,keyName="geMarkOwned",name="Mark owned activity items",description="Add Owned or Owned variant to activity results using bank, inventory and equipment. Equivalent gear is checked without treating broken/empty equipment as usable.",position=3)
    default boolean geMarkOwned() { return false; }
    @ConfigItem(section=ge,keyName="geShowPrices",name="Show guide prices",description="Add the cached RuneLite price for one item beside its name. Respects RuneLite's Wiki-price setting. A guide price is not a guaranteed offer price.",position=6)
    default boolean geShowPrices() { return false; }
    @ConfigItem(section=ge,keyName="gePriceAffordability",name="Colour prices by funds",description="With prices enabled: green means observed inventory/bank coins and platinum cover one item at guide price; red means insufficient observed funds; neutral means balance or price is unknown. Excludes uncollected offers and item resale value.",position=7)
    default boolean gePriceAffordability() { return true; }
    @ConfigItem(section=ge,keyName="geMarkLocked",name="Mark unmet requirements",description="Add Locked for verified unmet skill or quest requirements. Hover for details. Coverage is partial; no tag does not prove every requirement is met. Items remain searchable and purchasable.",position=8)
    default boolean geMarkLocked() { return false; }
    @ConfigItem(section=ge,keyName="geSearchTips",name="Show search tips",description="Show a small Search tips hint in the GE chatbox for broad searches. Hover for more specific queries. Bank search is unchanged.",position=9)
    default boolean geSearchTips() { return true; }
    @ConfigSection(name="Gear guide tiers",description="Shared bank and GE colours and ordinal tags",position=5,closedByDefault=true)
    String tiers="tiers";
    @ConfigItem(section=tiers,keyName="geTierColours",name="Show Wiki gear tiers",description="Shared bank and GE colours with 1st/2nd/3rd tags mark the first three guide columns. Conflicting ranks, examples and items needing preparation stay neutral. Hover for context. These are not universal BIS ratings.",position=0)
    default boolean geTierColours() { return true; }
    @ConfigItem(section=tiers,keyName="geFirstColour",name="1st — setup BIS",description="Shared bank and GE colour for Wiki column 1 in the matching setup and role. Requirements and set bonuses still apply.",position=1)
    default Color geFirstColour() { return new Color(138,43,226); }
    @ConfigItem(section=tiers,keyName="geSecondColour",name="2nd choice",description="Shared bank and GE colour for Wiki column 2. Unavailable higher choices do not promote this tier.",position=2)
    default Color geSecondColour() { return new Color(0,102,170); }
    @ConfigItem(section=tiers,keyName="geThirdColour",name="3rd choice",description="Shared bank and GE colour for Wiki column 3 in the matching setup.",position=3)
    default Color geThirdColour() { return new Color(0,119,68); }
    @ConfigItem(section=ge,keyName="geGuideContext",name="GE guide context",description="Specific guide chosen through Views. Empty uses all matching guide setups.",hidden=true,position=5)
    default String geGuideContext() { return ""; }
    @ConfigItem(keyName="bankInventoryContext",name="Bank inventory example",description="Inventory example chosen in Views.",hidden=true)
    default String bankInventoryContext() { return ""; }
    @ConfigItem(keyName="screenshotButton",name="Button beside bank close",description="Show a screenshot button next to the bank close button. Saves the full current tab locally.",section=screenshots,position=0)
    default boolean screenshotButton() { return true; }

    enum BankLabels { MINI, FULL, OFF }
    @ConfigItem(section=appearance,keyName="bankLabels",name="Bank labels",description="Mini labels show short group names once where they fit. Full uses saved label appearance. Off leaves the sidebar legend and hover names.",position=5)
    default BankLabels bankLabels() { return BankLabels.MINI; }
    @ConfigItem(section=appearance,keyName="quietFills",name="Subtle marker backgrounds",description="Reduce background intensity without changing saved marker colors.",position=6)
    default boolean quietFills() { return true; }
    @ConfigItem(section=appearance,keyName="hoverHighlight",name="Highlight on hover",description="Highlight groups when hovering bank items or legend entries. Turn off to highlight only by clicking a legend entry. Hover names remain available.",position=7)
    default boolean hoverHighlight() { return true; }
    @ConfigItem(section=appearance,keyName="improvedBankSearch",name="Enable Improved Banking search",description="Add category, preset and group matches to plain-text bank searches and highlight matching items. Also available in the bank search prompt.",position=8)
    default boolean improvedBankSearch() { return false; }
    @ConfigItem(section=appearance,keyName="setupSearchLayout",name="Equipment and inventory search layout",description="Show an eight-column equipment and inventory view for activity searches, with alternatives and supplies below. Ordinary searches and existing Bank Tag layouts keep their order.",position=9)
    default boolean setupSearchLayout() { return true; }
    @ConfigItem(section=appearance,keyName="bankCatalogueButton",name="Bank category picker",description="Show a searchable category and preset dropdown beside the bank screenshot button.",position=10)
    default boolean bankCatalogueButton() { return true; }
    @ConfigItem(section=drawing,keyName="snapToGrid",name="Snap areas to grid",description="Snap drawn, moved, and resized item-area markers to bank item cells.",position=3)
    default boolean snapToGrid() { return false; }

    @ConfigItem(section=drawing,keyName="linkNewMarkers",name="Link new markers to items",description="New markers follow the enclosed item identities between bank tabs. Linked regions always use grid bounds.",position=4)
    default boolean linkNewMarkers() { return false; }
    enum BorderStyle
    {
        ACTIVE_THEME("Active theme"), VANILLA("Vanilla OSRS"), PLAIN("Plain");
        private final String label;
        BorderStyle(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    @ConfigItem(section=drawing,keyName="style", name="Border style", description="Active theme uses loaded equipment-frame sprite overrides, falling back to vanilla.", position=0)
    default BorderStyle style() { return BorderStyle.ACTIVE_THEME; }

    @ConfigItem(section=appearance,keyName="visible", name="Show markers", description="Show saved markers while the bank is open.", position=1)
    default boolean visible() { return true; }

    @Alpha
    @ConfigItem(section=drawing,keyName="color", name="Plain border color", description="Color for plain borders and the drawing preview.", position=2)
    default Color color() { return new Color(255, 190, 70, 220); }

    @ConfigItem(section=bankLayout,keyName="bankLayoutEnabled",name="Built-in bank layout",description="Use the integrated flexible bank layout. Disable the separate Flexible Bank plugin.",position=10)
    default boolean bankLayoutEnabled() { return true; }
    enum TabAlignment { LEFT, CENTER, RIGHT }
    @ConfigItem(section=bankLayout,keyName="tabAlignment",name="Bank tab alignment",description="Align the top bank tabs within the bank window.",position=20)
    default TabAlignment tabAlignment() { return TabAlignment.CENTER; }
    @Range(min=0,max=40)
    @ConfigItem(section=bankLayout,keyName="tabGap",name="Space below tabs",description="Reserve space between the bank tabs and item viewport.",position=21)
    default int tabGap() { return 12; }
	@ConfigItem(section=bankLayout,keyName="customWidth",
		name = "Custom width",
		description = "Use the width below. When off the bank keeps the game's own width.",
		position = 11
	)
	default boolean customWidth()
	{
		return false;
	}

	@Range(min = 488, max = 4000)
	@ConfigItem(section=bankLayout,keyName="width",
		name = "Width (px)",
		description = "Width of the bank window when Custom width is on. Limited to the game area next to the side panel.",
		position = 12
	)
	default int width()
	{
		return 488;
	}

	@ConfigItem(section=bankLayout,keyName="customHeight",
		name = "Custom height",
		description = "Use the height below. When off the game decides: in resizable mode the bank follows the window, up to 800.",
		position = 13
	)
	default boolean customHeight()
	{
		return false;
	}

	@Range(min = 200, max = 4000)
	@ConfigItem(section=bankLayout,keyName="height",
		name = "Height (px)",
		description = "Height of the bank window when Custom height is on. Limited to the game area.",
		position = 14
	)
	default int height()
	{
		return 330;
	}

	@Range(max = 60)
	@ConfigItem(section=bankLayout,keyName="columns",
		name = "Columns",
		description = "Items per row. 0 = as many as fit the width.",
		position = 15
	)
	default int columns()
	{
		return 0;
	}

	@ConfigItem(section=bankLayout,keyName="depositButtons",
		name = "Deposit buttons",
		description = "Keep the three deposit buttons in the bottom bar, or stack them on the left side below the incinerator and storage buttons.",
		position = 16
	)
	default DepositButtons depositButtons()
	{
		return DepositButtons.BOTTOM;
	}

	@ConfigItem(section=bankLayout,keyName="alignDepositRight",
		name = "Keep deposit buttons at right edge",
		description = "When the deposit buttons are in the bottom bar, keep them at the right edge of a widened bank.",
		position = 17
	)
	default boolean alignDepositRight()
	{
		return true;
	}
}

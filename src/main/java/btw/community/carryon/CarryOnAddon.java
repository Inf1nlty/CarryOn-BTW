package btw.community.carryon;

import api.AddonHandler;
import api.BTWAddon;

import tschipp.carryon.CarryOnItems;

public class CarryOnAddon extends BTWAddon {

    @Override
    public void initialize() {
        AddonHandler.logMessage(getName() + " v" + getVersionString() + " Initializing...");

        CarryOnItems.registerItems();
    }
}
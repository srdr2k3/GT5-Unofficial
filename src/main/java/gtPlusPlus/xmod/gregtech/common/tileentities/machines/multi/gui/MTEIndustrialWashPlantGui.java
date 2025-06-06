package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.gui;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.metatileentity.implementations.gui.MTEMultiBlockBaseGui;
import gregtech.api.modularui2.GTGuiTextures;

public class MTEIndustrialWashPlantGui extends MTEMultiBlockBaseGui {

    public MTEIndustrialWashPlantGui(MTEMultiBlockBase base) {
        super(base);
    }

    @Override
    protected void setMachineModeIcons() {
        machineModeIcons.add(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_WASHPLANT);
        machineModeIcons.add(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_SIMPLEWASHER);
        machineModeIcons.add(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_CHEMBATH);
    }
}

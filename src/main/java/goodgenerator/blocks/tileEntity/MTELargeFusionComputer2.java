package goodgenerator.blocks.tileEntity;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION2;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION2_GLOW;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import bartworks.common.loaders.ItemRegistry;
import goodgenerator.blocks.tileEntity.base.MTELargeFusionComputer;
import goodgenerator.loader.Loaders;
import goodgenerator.util.DescTextLocalization;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

public class MTELargeFusionComputer2 extends MTELargeFusionComputer {

    private static final ITexture textureOverlay = TextureFactory.of(
        TextureFactory.builder()
            .addIcon(OVERLAY_FUSION2)
            .extFacing()
            .build(),
        TextureFactory.builder()
            .addIcon(OVERLAY_FUSION2_GLOW)
            .extFacing()
            .glow()
            .build());

    public MTELargeFusionComputer2(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
    }

    public MTELargeFusionComputer2(String name) {
        super(name);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Fusion Reactor")
            .addInfo("Millions of nuclear.")
            .addInfo(
                EnumChatFormatting.AQUA + GTUtility.formatNumbers(getSingleHatchPower())
                    + EnumChatFormatting.GRAY
                    + " EU/t and "
                    + EnumChatFormatting.AQUA
                    + GTUtility.formatNumbers(capableStartupCanonical() / 32 / M)
                    + "M"
                    + EnumChatFormatting.GRAY
                    + " EU capacity per Energy Hatch")
            .addInfo("If the recipe has a startup cost greater than the")
            .addInfo("number of energy hatches * cap, you can't do it")
            .addInfo(
                "If the recipe requires a voltage tier over " + GTUtility.getColoredTierNameFromTier((byte) tier())
                    + EnumChatFormatting.GRAY
                    + " , you can't do it either")
            .addInfo("Startup < 160,000,000 EU: 128x Parallel")
            .addInfo("Startup >= 160,000,000 EU: 64x Parallel")
            .addTecTechHatchInfo()
            .addCasingInfoMin("Fusion Machine Casing", 1664, false)
            .addCasingInfoMin("Compact Fusion Coil", 560, false)
            .addCasingInfoMin("Duranium Frame Box", 128, false)
            .addCasingInfoMin("Iridium Reinforced Borosilicate Glass Block", 63, false)
            .addEnergyHatch("1-32, Hint block with dot 2", 2)
            .addInputHatch("1-16, Hint block with dot 1", 1)
            .addOutputHatch("1-16, Hint block with dot 1", 1)
            .addStructureInfo("Supports Crafting Input Buffer")
            .addStructureInfo(
                "Energy Hatches must be " + GTUtility.getColoredTierNameFromTier((byte) energyHatchTier())
                    + EnumChatFormatting.GRAY
                    + " or better")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public int tier() {
        return 7;
    }

    @Override
    public long capableStartupCanonical() {
        return 320_000_000;
    }

    @Override
    public Block getCasingBlock() {
        return GregTechAPI.sBlockCasings4;
    }

    @Override
    public int getCasingMeta() {
        return 6;
    }

    @Override
    public Block getCoilBlock() {
        return Loaders.compactFusionCoil;
    }

    @Override
    public int getCoilMeta() {
        return 1;
    }

    @Override
    public Block getGlassBlock() {
        return ItemRegistry.bw_realglas;
    }

    @Override
    public int getGlassMeta() {
        return 4;
    }

    @Override
    public int energyHatchTier() {
        return 7;
    }

    @Override
    public Materials getFrameBox() {
        return Materials.Duranium;
    }

    @Override
    public int getMaxPara() {
        return 64;
    }

    @Override
    public int extraPara(long startEnergy) {
        return startEnergy < 160000000L ? 2 : 1;
    }

    @Override
    public ITexture getTextureOverlay() {
        return textureOverlay;
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return DescTextLocalization.addText("LargeFusion2.hint", 9);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTELargeFusionComputer2(mName);
    }
}

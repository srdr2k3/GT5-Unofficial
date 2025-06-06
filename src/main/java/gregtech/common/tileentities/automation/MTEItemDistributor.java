package gregtech.common.tileentities.automation;

import static gregtech.api.enums.Textures.BlockIcons.AUTOMATION_ITEMDISTRIBUTOR;
import static gregtech.api.enums.Textures.BlockIcons.AUTOMATION_ITEMDISTRIBUTOR_GLOW;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;

import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBuffer;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;

public class MTEItemDistributor extends MTEBuffer {

    private byte[] itemsPerSide = new byte[6];
    private ForgeDirection currentSide = ForgeDirection.DOWN;
    private byte currentSideItemCount = 0;

    public MTEItemDistributor(int aID, String aName, String aNameRegional, int aTier) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            28,
            new String[] { "Distributes Items between different Machine Sides", "Default Items per Machine Side: 0",
                "Use Screwdriver to increase/decrease Items per Side" });
    }

    public MTEItemDistributor(String aName, int aTier, int aInvSlotCount, String[] aDescription,
        ITexture[][][] aTextures) {
        super(aName, aTier, aInvSlotCount, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEItemDistributor(
            this.mName,
            this.mTier,
            this.mInventory.length,
            this.mDescriptionArray,
            this.mTextures);
    }

    @Override
    public ITexture getOverlayIcon() {
        return TextureFactory.of(
            TextureFactory.of(AUTOMATION_ITEMDISTRIBUTOR),
            TextureFactory.builder()
                .addIcon(AUTOMATION_ITEMDISTRIBUTOR_GLOW)
                .glow()
                .build());
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return side == aBaseMetaTileEntity.getFrontFacing();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        if (side == aFacing) {
            return mTextures[0][colorIndex + 1];
        } else {
            return mTextures[1][colorIndex + 1];
        }
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] aTextures) {
        ITexture[][][] returnTextures = new ITexture[2][17][];
        ITexture baseIcon = getOverlayIcon(), pipeIcon = TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT);
        for (int i = 0; i < 17; i++) {
            returnTextures[0][i] = new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][i], baseIcon };
            returnTextures[1][i] = new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][i], pipeIcon, baseIcon };
        }
        return returnTextures;
    }

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        return getBaseMetaTileEntity().getFrontFacing() == side || itemsPerSide[side.ordinal()] == 0;
    }

    @Override
    public boolean isOutputFacing(ForgeDirection side) {
        return getBaseMetaTileEntity().getFrontFacing() != side && itemsPerSide[side.ordinal()] > 0;
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        itemsPerSide = aNBT.getByteArray("mItemsPerSide");
        if (itemsPerSide.length != 6) {
            itemsPerSide = new byte[6];
        }
        currentSide = ForgeDirection.getOrientation(aNBT.getByte("mCurrentSide"));
        currentSideItemCount = aNBT.getByte("mCurrentSideItemCount");
    }

    @Override
    protected void moveItems(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        int currentSideOrdinal = currentSide.ordinal();
        fillStacksIntoFirstSlots();
        int movedItems;
        TileEntity adjacentTileEntity = aBaseMetaTileEntity.getTileEntityAtSide(currentSide);
        int inspectedSides = 0;
        while (itemsPerSide[currentSideOrdinal] == 0) {
            currentSideOrdinal = ((currentSideOrdinal + 1) % 6);
            currentSide = ForgeDirection.getOrientation(currentSideOrdinal);
            currentSideItemCount = 0;
            adjacentTileEntity = aBaseMetaTileEntity.getTileEntityAtSide(currentSide);
            inspectedSides += 1;
            if (inspectedSides == 6) {
                return;
            }
        }
        movedItems = GTUtility.moveOneItemStack(
            aBaseMetaTileEntity,
            adjacentTileEntity,
            currentSide,
            currentSide.getOpposite(),
            null,
            false,
            (byte) 64,
            (byte) 1,
            (byte) (itemsPerSide[currentSideOrdinal] - currentSideItemCount),
            (byte) 1);
        currentSideItemCount += movedItems;
        if (currentSideItemCount >= itemsPerSide[currentSideOrdinal]) {
            currentSideOrdinal = ((currentSideOrdinal + 1) % 6);
            currentSide = ForgeDirection.getOrientation(currentSideOrdinal);
            currentSideItemCount = 0;
        }
        if (movedItems > 0 || aBaseMetaTileEntity.hasInventoryBeenModified()) {
            mSuccess = 50;
        }
        fillStacksIntoFirstSlots();
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        final int ordinalSide = side.ordinal();
        // Adjust items per side by 1 or -1, constrained to the cyclic interval [0, 127]
        itemsPerSide[ordinalSide] += aPlayer.isSneaking() ? -1 : 1;
        itemsPerSide[ordinalSide] = (byte) ((itemsPerSide[ordinalSide] + 128) % 128);
        GTUtility.sendChatToPlayer(aPlayer, GTUtility.trans("211", "Items per side: ") + itemsPerSide[ordinalSide]);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setByteArray("mItemsPerSide", itemsPerSide);
        aNBT.setByte("mCurrentSide", (byte) currentSide.ordinal());
        aNBT.setByte("mCurrentSideItemCount", currentSideItemCount);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        boolean hasSettings = false;
        for (byte i : itemsPerSide) {
            if (i != 0) {
                hasSettings = true;
                break;
            }
        }
        if (hasSettings) aNBT.setByteArray("mItemsPerSide", itemsPerSide);
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        super.addUIWidgets(builder, buildContext);
        addEmitRedstoneIfFullButton(builder);
        addInvertRedstoneButton(builder);
        builder.widget(
            new DrawableWidget().setDrawable(GTUITextures.PICTURE_ARROW_22_RED.apply(87, true))
                .setPos(62, 60)
                .setSize(87, 22));
        addInventorySlots(builder);
    }
}

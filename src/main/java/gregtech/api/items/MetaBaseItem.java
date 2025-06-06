package gregtech.api.items;

import static gregtech.api.enums.GTValues.D1;
import static gregtech.api.enums.GTValues.V;
import static gregtech.api.util.GTUtility.formatNumbers;
import static net.minecraft.util.StatCollector.translateToLocal;
import static net.minecraft.util.StatCollector.translateToLocalFormatted;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gtnewhorizons.modularui.api.KeyboardUtil;

import gregtech.GTMod;
import gregtech.api.enums.SubTag;
import gregtech.api.interfaces.IItemBehaviour;
import gregtech.api.util.GTLanguageManager;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;

public abstract class MetaBaseItem extends GTGenericItem
    implements ISpecialElectricItem, IElectricItemManager, IFluidContainerItem {

    /* ---------- CONSTRUCTOR AND MEMBER VARIABLES ---------- */
    private final ConcurrentHashMap<Short, ArrayList<IItemBehaviour<MetaBaseItem>>> mItemBehaviors = new ConcurrentHashMap<>();

    /**
     * Creates the Item using these Parameters.
     *
     * @param aUnlocalized The Unlocalized Name of this Item.
     */
    public MetaBaseItem(String aUnlocalized) {
        super(aUnlocalized, "Generated Item", null);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    /**
     * Adds a special Item Behaviour to the Item.
     * <p/>
     * Note: the boolean Behaviours sometimes won't be executed if another boolean Behaviour returned true before.
     *
     * @param aMetaValue the Meta Value of the Item you want to add it to. [0 - 32765]
     * @param aBehavior  the Click Behavior you want to add.
     * @return the Item itself for convenience in constructing.
     */
    public final MetaBaseItem addItemBehavior(int aMetaValue, IItemBehaviour<MetaBaseItem> aBehavior) {
        if (aMetaValue < 0 || aMetaValue >= 32766 || aBehavior == null) return this;
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors
            .computeIfAbsent((short) aMetaValue, k -> new ArrayList<>(1));
        tList.add(aBehavior);
        return this;
    }

    public abstract Long[] getElectricStats(ItemStack aStack);

    public abstract Long[] getFluidContainerStats(ItemStack aStack);

    @Override
    public boolean hasProjectile(SubTag aProjectileType, ItemStack aStack) {
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
            if (tBehavior.hasProjectile(this, aProjectileType, aStack)) return true;
        return super.hasProjectile(aProjectileType, aStack);
    }

    @Override
    public EntityArrow getProjectile(SubTag aProjectileType, ItemStack aStack, World aWorld, double aX, double aY,
        double aZ) {
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList) {
            EntityArrow rArrow = tBehavior.getProjectile(this, aProjectileType, aStack, aWorld, aX, aY, aZ);
            if (rArrow != null) return rArrow;
        }
        return super.getProjectile(aProjectileType, aStack, aWorld, aX, aY, aZ);
    }

    @Override
    public EntityArrow getProjectile(SubTag aProjectileType, ItemStack aStack, World aWorld, EntityLivingBase aEntity,
        float aSpeed) {
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList) {
            EntityArrow rArrow = tBehavior.getProjectile(this, aProjectileType, aStack, aWorld, aEntity, aSpeed);
            if (rArrow != null) return rArrow;
        }
        return super.getProjectile(aProjectileType, aStack, aWorld, aEntity, aSpeed);
    }

    @Override
    public ItemStack onDispense(IBlockSource aSource, ItemStack aStack) {
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
            if (tBehavior.canDispense(this, aSource, aStack)) return tBehavior.onDispense(this, aSource, aStack);
        return super.onDispense(aSource, aStack);
    }

    @Override
    public boolean isItemStackUsable(ItemStack aStack) {
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
            if (!tBehavior.isItemStackUsable(this, aStack)) return false;
        return super.isItemStackUsable(aStack);
    }

    public boolean onLeftClick(ItemStack aStack, EntityPlayer aPlayer) {
        return forEachBehavior(aStack, behavior -> behavior.onLeftClick(this, aStack, aPlayer));
    }

    public boolean onMiddleClick(ItemStack aStack, EntityPlayer aPlayer) {
        return forEachBehavior(aStack, behavior -> behavior.onMiddleClick(this, aStack, aPlayer));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack aStack, EntityPlayer aPlayer, Entity aEntity) {
        use(aStack, 0, aPlayer);
        isItemStackUsable(aStack);
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        try {
            if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
                if (tBehavior.onLeftClickEntity(this, aStack, aPlayer, aEntity)) {
                    if (aStack.stackSize <= 0) aPlayer.destroyCurrentEquippedItem();
                    return true;
                }
            if (aStack.stackSize <= 0) {
                aPlayer.destroyCurrentEquippedItem();
                return false;
            }
        } catch (Throwable e) {
            GTMod.GT_FML_LOGGER.error("Error left clicking entity", e);
        }
        return false;
    }

    @Override
    public boolean onItemUse(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ,
        int ordinalSide, float hitX, float hitY, float hitZ) {
        use(aStack, 0, aPlayer);
        isItemStackUsable(aStack);
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        try {
            if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
                if (tBehavior.onItemUse(this, aStack, aPlayer, aWorld, aX, aY, aZ, ordinalSide, hitX, hitY, hitZ)) {
                    if (aStack.stackSize <= 0) aPlayer.destroyCurrentEquippedItem();
                    return true;
                }
            if (aStack.stackSize <= 0) {
                aPlayer.destroyCurrentEquippedItem();
                return false;
            }
        } catch (Throwable e) {
            GTMod.GT_FML_LOGGER.error("Error using item", e);
        }
        return false;
    }

    @Override
    public boolean onItemUseFirst(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ,
        int ordinalSide, float hitX, float hitY, float hitZ) {
        use(aStack, 0, aPlayer);
        isItemStackUsable(aStack);
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        try {
            if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList) if (tBehavior.onItemUseFirst(
                this,
                aStack,
                aPlayer,
                aWorld,
                aX,
                aY,
                aZ,
                ForgeDirection.getOrientation(ordinalSide),
                hitX,
                hitY,
                hitZ)) {
                    if (aStack.stackSize <= 0) aPlayer.destroyCurrentEquippedItem();
                    return true;
                }
            if (aStack.stackSize <= 0) {
                aPlayer.destroyCurrentEquippedItem();
                return false;
            }
        } catch (Throwable e) {
            GTMod.GT_FML_LOGGER.error("Error using item", e);
        }
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack aStack, World aWorld, EntityPlayer aPlayer) {
        use(aStack, 0, aPlayer);
        isItemStackUsable(aStack);
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        try {
            if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
                aStack = tBehavior.onItemRightClick(this, aStack, aWorld, aPlayer);
        } catch (Throwable e) {
            GTMod.GT_FML_LOGGER.error("Error right clicking item", e);
        }
        return aStack;
    }

    @Override
    public final void addInformation(ItemStack aStack, EntityPlayer aPlayer, List<String> aList, boolean aF3_H) {
        String tKey = getUnlocalizedName(aStack) + ".tooltip";
        String[] tStrings = GTLanguageManager.getTranslation(tKey)
            .split("/n ");
        for (String tString : tStrings)
            if (GTUtility.isStringValid(tString) && !tKey.equals(tString)) aList.add(tString);

        Long[] tStats = getElectricStats(aStack);
        if (tStats != null) {
            if (tStats[3] > 0) {
                aList.add(
                    EnumChatFormatting.AQUA
                        + translateToLocalFormatted(
                            "gt.item.desc.stored_eu",
                            formatNumbers(tStats[3]),
                            "" + (tStats[2] >= 0 ? tStats[2] : 0))
                        + EnumChatFormatting.GRAY);
            } else {
                long tCharge = getRealCharge(aStack);
                if (tStats[3] == -2 && tCharge <= 0) {
                    aList.add(
                        EnumChatFormatting.AQUA + translateToLocal("gt.item.desc.empty") + EnumChatFormatting.GRAY);
                } else {
                    int voltageTier = (int) GTUtility.clamp(tStats[2], 0, V.length - 1);
                    aList.add(
                        EnumChatFormatting.AQUA
                            + translateToLocalFormatted(
                                "gt.item.desc.eu_info",
                                formatNumbers(tCharge),
                                formatNumbers(Math.abs(tStats[0])),
                                formatNumbers(V[voltageTier]))
                            + EnumChatFormatting.GRAY);
                }
            }
        }

        tStats = getFluidContainerStats(aStack);
        if (tStats != null && tStats[0] > 0) {
            FluidStack tFluid = getFluidContent(aStack);
            aList.add(
                EnumChatFormatting.BLUE + ((tFluid == null ? translateToLocal("gt.item.desc.no_fluid")
                    : GTUtility.getFluidName(tFluid, true))) + EnumChatFormatting.GRAY);
            aList.add(
                EnumChatFormatting.BLUE
                    + translateToLocalFormatted(
                        "gt.item.desc.fluid_info",
                        tFluid == null ? 0 : formatNumbers(tFluid.amount),
                        formatNumbers(tStats[0]))
                    + EnumChatFormatting.GRAY);
        }

        ArrayList<IItemBehaviour<MetaBaseItem>> behaviours = mItemBehaviors.get((short) getDamage(aStack));
        if (behaviours != null) {
            for (IItemBehaviour<MetaBaseItem> behavior : behaviours) {
                final Optional<List<String>> shiftTooltips = KeyboardUtil.isShiftKeyDown()
                    ? behavior.getAdditionalToolTipsWhileSneaking(this, aList, aStack)
                    : Optional.empty();
                if (shiftTooltips.isPresent()) {
                    aList = shiftTooltips.get();
                } else {
                    aList = behavior.getAdditionalToolTips(this, aList, aStack);
                }
            }
        }

        addAdditionalToolTips(aList, aStack, aPlayer);
    }

    @Override
    public void onUpdate(ItemStack aStack, World aWorld, Entity aPlayer, int aTimer, boolean aIsInHand) {
        ArrayList<IItemBehaviour<MetaBaseItem>> tList = mItemBehaviors.get((short) getDamage(aStack));
        if (tList != null) for (IItemBehaviour<MetaBaseItem> tBehavior : tList)
            tBehavior.onUpdate(this, aStack, aWorld, aPlayer, aTimer, aIsInHand);
    }

    @Override
    public final boolean canProvideEnergy(ItemStack aStack) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null) return false;
        return tStats[3] > 0 || (aStack.stackSize == 1 && (tStats[3] == -2 || tStats[3] == -3));
    }

    @Override
    public final double getMaxCharge(ItemStack aStack) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null) return 0;
        return Math.abs(tStats[0]);
    }

    @Override
    public final double getTransferLimit(ItemStack aStack) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null) return 0;
        return Math.max(tStats[1], tStats[3]);
    }

    @Override
    public final double charge(ItemStack aStack, double aCharge, int aTier, boolean aIgnoreTransferLimit,
        boolean aSimulate) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null || tStats[2] > aTier
            || !(tStats[3] == -1 || tStats[3] == -3 || (tStats[3] < 0 && aCharge == Integer.MAX_VALUE))
            || aStack.stackSize != 1) return 0;
        long tTransfer = aIgnoreTransferLimit ? (long) aCharge : Math.min(tStats[1], (long) aCharge);
        long tChargeBefore = getRealCharge(aStack), tNewCharge = Math.min(
            Math.abs(tStats[0]),
            Long.MAX_VALUE - tTransfer >= tChargeBefore ? tChargeBefore + tTransfer : Long.MAX_VALUE);
        if (!aSimulate) setCharge(aStack, tNewCharge);
        return tNewCharge - tChargeBefore;
    }

    @Override
    public final double discharge(ItemStack aStack, double aCharge, int aTier, boolean aIgnoreTransferLimit,
        boolean aBatteryAlike, boolean aSimulate) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null || tStats[2] > aTier) return 0;
        if (aBatteryAlike && !canProvideEnergy(aStack)) return 0;
        if (tStats[3] > 0) {
            if (aCharge < tStats[3] || aStack.stackSize < 1) return 0;
            if (!aSimulate) aStack.stackSize--;
            return tStats[3];
        }
        long tChargeBefore = getRealCharge(aStack), tNewCharge = Math
            .max(0, tChargeBefore - (aIgnoreTransferLimit ? (long) aCharge : Math.min(tStats[1], (long) aCharge)));
        if (!aSimulate) setCharge(aStack, tNewCharge);
        return tChargeBefore - tNewCharge;
    }

    @Override
    public final double getCharge(ItemStack aStack) {
        return getRealCharge(aStack);
    }

    @Override
    public final boolean canUse(ItemStack aStack, double aAmount) {
        return getRealCharge(aStack) >= aAmount;
    }

    @Override
    public final boolean use(ItemStack aStack, double aAmount, EntityLivingBase aPlayer) {
        chargeFromArmor(aStack, aPlayer);
        if (aPlayer instanceof EntityPlayer && ((EntityPlayer) aPlayer).capabilities.isCreativeMode) return true;
        double tTransfer = discharge(aStack, aAmount, Integer.MAX_VALUE, true, false, true);
        if (Math.abs(tTransfer - aAmount) < .0000001) {
            discharge(aStack, aAmount, Integer.MAX_VALUE, true, false, false);
            chargeFromArmor(aStack, aPlayer);
            return true;
        }
        discharge(aStack, aAmount, Integer.MAX_VALUE, true, false, false);
        chargeFromArmor(aStack, aPlayer);
        return false;
    }

    @Override
    public final void chargeFromArmor(ItemStack aStack, EntityLivingBase aPlayer) {
        if (aPlayer == null || aPlayer.worldObj.isRemote) return;
        for (int i = 1; i < 5; i++) {
            ItemStack tArmor = aPlayer.getEquipmentInSlot(i);
            if (GTModHandler.isElectricItem(tArmor)) {
                IElectricItem tArmorItem = (IElectricItem) tArmor.getItem();
                if (tArmorItem.canProvideEnergy(tArmor) && tArmorItem.getTier(tArmor) >= getTier(aStack)) {
                    double tCharge = ElectricItem.manager.discharge(
                        tArmor,
                        charge(aStack, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, true, true),
                        Integer.MAX_VALUE,
                        true,
                        true,
                        false);
                    if (tCharge > 0) {
                        charge(aStack, tCharge, Integer.MAX_VALUE, true, false);
                        if (aPlayer instanceof EntityPlayer) {
                            Container tContainer = ((EntityPlayer) aPlayer).openContainer;
                            if (tContainer != null) tContainer.detectAndSendChanges();
                        }
                    }
                }
            }
        }
    }

    /*
     * @Override public final int getMaxCharge(ItemStack aStack) { Long[] tStats = getElectricStats(aStack); if (tStats
     * == null) return 0; return (int)Math.abs(tStats[0]); }
     * @Override public final int getTransferLimit(ItemStack aStack) { Long[] tStats = getElectricStats(aStack); if
     * (tStats == null) return 0; return (int)Math.max(tStats[1], tStats[3]); }
     * @Override public final int charge(ItemStack aStack, int aCharge, int aTier, boolean aIgnoreTransferLimit, boolean
     * aSimulate) { Long[] tStats = getElectricStats(aStack); if (tStats == null || tStats[2] > aTier || !(tStats[3] ==
     * -1 || tStats[3] == -3 || (tStats[3] < 0 && aCharge == Integer.MAX_VALUE)) || aStack.stackSize != 1) return 0;
     * long tChargeBefore = getRealCharge(aStack), tNewCharge =
     * aCharge==Integer.MAX_VALUE?Long.MAX_VALUE:Math.min(Math.abs(tStats[0]), tChargeBefore +
     * (aIgnoreTransferLimit?aCharge:Math.min(tStats[1], aCharge))); if (!aSimulate) setCharge(aStack, tNewCharge);
     * return (int)(tNewCharge-tChargeBefore); }
     * @Override public final int discharge(ItemStack aStack, int aCharge, int aTier, boolean aIgnoreTransferLimit,
     * boolean aSimulate) { Long[] tStats = getElectricStats(aStack); if (tStats == null || tStats[2] > aTier) return 0;
     * if (tStats[3] > 0) { if (aCharge < tStats[3] || aStack.stackSize < 1) return 0; if (!aSimulate)
     * aStack.stackSize--; return (int)(long)tStats[3]; } long tChargeBefore = getRealCharge(aStack), tNewCharge =
     * Math.max(0, tChargeBefore - (aIgnoreTransferLimit?aCharge:Math.min(tStats[1], aCharge))); if (!aSimulate)
     * setCharge(aStack, tNewCharge); return (int)(tChargeBefore-tNewCharge); }
     * @Override public final int getCharge(ItemStack aStack) { return (int)Math.min(Integer.MAX_VALUE,
     * getRealCharge(aStack)); }
     * @Override public final boolean canUse(ItemStack aStack, int aAmount) { return getRealCharge(aStack) >= aAmount; }
     * @Override public final boolean use(ItemStack aStack, int aAmount, EntityLivingBase aPlayer) {
     * chargeFromArmor(aStack, aPlayer); if (aPlayer instanceof EntityPlayer &&
     * ((EntityPlayer)aPlayer).capabilities.isCreativeMode) return true; int tTransfer = discharge(aStack, aAmount,
     * Integer.MAX_VALUE, true, true); if (tTransfer == aAmount) { discharge(aStack, aAmount, Integer.MAX_VALUE, true,
     * false); chargeFromArmor(aStack, aPlayer); return true; } discharge(aStack, aAmount, Integer.MAX_VALUE, true,
     * false); chargeFromArmor(aStack, aPlayer); return false; }
     * @Override public final void chargeFromArmor(ItemStack aStack, EntityLivingBase aPlayer) { if (aPlayer == null ||
     * aPlayer.worldObj.isRemote) return; for (int i = 1; i < 5; i++) { ItemStack tArmor =
     * aPlayer.getEquipmentInSlot(i); if (GTModHandler.isElectricItem(tArmor)) { IElectricItem tArmorItem =
     * (IElectricItem)tArmor.getItem(); if (tArmorItem.canProvideEnergy(tArmor) && tArmorItem.getTier(tArmor) >=
     * getTier(aStack)) { int tCharge = ElectricItem.manager.discharge(tArmor, charge(aStack, Integer.MAX_VALUE-1,
     * Integer.MAX_VALUE, true, true), Integer.MAX_VALUE, true, false); if (tCharge > 0) { charge(aStack, tCharge,
     * Integer.MAX_VALUE, true, false); if (aPlayer instanceof EntityPlayer) { Container tContainer =
     * ((EntityPlayer)aPlayer).openContainer; if (tContainer != null) tContainer.detectAndSendChanges(); } } } } } }
     */
    public final long getRealCharge(ItemStack aStack) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null) return 0;
        if (tStats[3] > 0) return (int) (long) tStats[3];
        NBTTagCompound tNBT = aStack.getTagCompound();
        return tNBT == null ? 0 : tNBT.getLong("GT.ItemCharge");
    }

    public final boolean setCharge(ItemStack aStack, long aCharge) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats == null || tStats[3] > 0) return false;
        NBTTagCompound tNBT = aStack.getTagCompound();
        if (tNBT == null) tNBT = new NBTTagCompound();
        tNBT.removeTag("GT.ItemCharge");
        aCharge = Math.min(tStats[0] < 0 ? Math.abs(tStats[0] / 2) : aCharge, Math.abs(tStats[0]));
        if (aCharge > 0) {
            aStack.setItemDamage(getChargedMetaData(aStack));
            tNBT.setLong("GT.ItemCharge", aCharge);
        } else {
            aStack.setItemDamage(getEmptyMetaData(aStack));
        }
        if (tNBT.hasNoTags()) aStack.setTagCompound(null);
        else aStack.setTagCompound(tNBT);
        isItemStackUsable(aStack);
        return true;
    }

    public short getChargedMetaData(ItemStack aStack) {
        return (short) aStack.getItemDamage();
    }

    public short getEmptyMetaData(ItemStack aStack) {
        return (short) aStack.getItemDamage();
    }

    @Override
    public FluidStack getFluid(ItemStack aStack) {
        return getFluidContent(aStack);
    }

    @Override
    public int getCapacity(ItemStack aStack) {
        Long[] tStats = getFluidContainerStats(aStack);
        return tStats == null ? 0 : (int) Math.max(0, tStats[0]);
    }

    @Override
    public int fill(ItemStack aStack, FluidStack aFluid, boolean doFill) {
        if (aStack == null || aStack.stackSize != 1) return 0;

        ItemStack tStack = GTUtility.fillFluidContainer(aFluid, aStack, false, false);
        if (tStack != null) {
            aStack.setItemDamage(tStack.getItemDamage());
            aStack.func_150996_a(tStack.getItem());
            return GTUtility.getFluidForFilledItem(tStack, false).amount;
        }

        Long[] tStats = getFluidContainerStats(aStack);
        if (tStats == null || tStats[0] <= 0
            || aFluid == null
            || aFluid.getFluid()
                .getID() <= 0
            || aFluid.amount <= 0) return 0;

        FluidStack tFluid = getFluidContent(aStack);

        if (tFluid == null || tFluid.getFluid()
            .getID() <= 0) {
            if (aFluid.amount <= tStats[0]) {
                if (doFill) {
                    setFluidContent(aStack, aFluid);
                }
                return aFluid.amount;
            }
            if (doFill) {
                tFluid = aFluid.copy();
                tFluid.amount = (int) (long) tStats[0];
                setFluidContent(aStack, tFluid);
            }
            return (int) (long) tStats[0];
        }

        if (!tFluid.isFluidEqual(aFluid)) return 0;

        int space = (int) (long) tStats[0] - tFluid.amount;
        if (aFluid.amount <= space) {
            if (doFill) {
                tFluid.amount += aFluid.amount;
                setFluidContent(aStack, tFluid);
            }
            return aFluid.amount;
        }
        if (doFill) {
            tFluid.amount = (int) (long) tStats[0];
            setFluidContent(aStack, tFluid);
        }
        return space;
    }

    @Override
    public FluidStack drain(ItemStack aStack, int maxDrain, boolean doDrain) {
        if (aStack == null || aStack.stackSize != 1) return null;

        FluidStack tFluid = GTUtility.getFluidForFilledItem(aStack, false);
        if (tFluid != null && maxDrain >= tFluid.amount) {
            ItemStack tStack = GTUtility.getContainerItem(aStack, false);
            if (tStack == null) {
                if (doDrain) aStack.stackSize = 0;
                return tFluid;
            }
            if (doDrain) {
                aStack.setItemDamage(tStack.getItemDamage());
                aStack.func_150996_a(tStack.getItem());
            }
            return tFluid;
        }

        Long[] tStats = getFluidContainerStats(aStack);
        if (tStats == null || tStats[0] <= 0) return null;

        tFluid = getFluidContent(aStack);
        if (tFluid == null) return null;

        int used = maxDrain;
        if (tFluid.amount < used) used = tFluid.amount;
        if (doDrain) {
            tFluid.amount -= used;
            setFluidContent(aStack, tFluid);
        }

        FluidStack drained = tFluid.copy();
        drained.amount = used;
        return drained;
    }

    public FluidStack getFluidContent(ItemStack aStack) {
        Long[] tStats = getFluidContainerStats(aStack);
        if (tStats == null || tStats[0] <= 0) return GTUtility.getFluidForFilledItem(aStack, false);
        NBTTagCompound tNBT = aStack.getTagCompound();
        return tNBT == null ? null : FluidStack.loadFluidStackFromNBT(tNBT.getCompoundTag("GT.FluidContent"));
    }

    public void setFluidContent(ItemStack aStack, FluidStack aFluid) {
        NBTTagCompound tNBT = aStack.getTagCompound();
        if (tNBT == null) tNBT = new NBTTagCompound();
        else tNBT.removeTag("GT.FluidContent");
        if (aFluid != null && aFluid.amount > 0)
            tNBT.setTag("GT.FluidContent", aFluid.writeToNBT(new NBTTagCompound()));
        if (tNBT.hasNoTags()) aStack.setTagCompound(null);
        else aStack.setTagCompound(tNBT);
        isItemStackUsable(aStack);
    }

    @Override
    public int getItemStackLimit(ItemStack aStack) {
        Long[] tStats = getElectricStats(aStack);
        if (tStats != null && (tStats[3] == -1 || tStats[3] == -2 || tStats[3] == -3) && getRealCharge(aStack) > 0)
            return 1;
        tStats = getFluidContainerStats(aStack);
        if (tStats != null) return (int) (long) tStats[1];
        if (getDamage(aStack) == 32763) return 1;
        return 64;
    }

    @Override
    public final Item getChargedItem(ItemStack itemStack) {
        return this;
    }

    @Override
    public final Item getEmptyItem(ItemStack itemStack) {
        return this;
    }

    @Override
    public final int getTier(ItemStack aStack) {
        Long[] tStats = getElectricStats(aStack);
        return (int) (tStats == null ? Integer.MAX_VALUE : tStats[2]);
    }

    @Override
    public final String getToolTip(ItemStack aStack) {
        return null;
    } // This has its own ToolTip Handler, no need to let the IC2 Handler screw us up at this Point

    @Override
    public final IElectricItemManager getManager(ItemStack aStack) {
        return this;
    } // We are our own Manager

    @Override
    public final boolean getShareTag() {
        return true;
    } // just to be sure.

    @Override
    public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {
        return false;
    }

    public boolean forEachBehavior(ItemStack aStack, Predicate<IItemBehaviour<MetaBaseItem>> predicate) {
        ArrayList<IItemBehaviour<MetaBaseItem>> behaviorList = mItemBehaviors.get((short) getDamage(aStack));
        if (behaviorList == null) {
            return false;
        }

        try {
            for (IItemBehaviour<MetaBaseItem> behavior : behaviorList) {
                if (predicate.test(behavior)) {
                    // Returning true short circuits the loop, and false continues it.
                    return true;
                }
            }
        } catch (Exception e) {
            if (D1) e.printStackTrace(GTLog.err);
        }

        return false;
    }

}

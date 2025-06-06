package gtPlusPlus.core.util.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import gregtech.api.util.GTUtility;

public class NBTUtils {

    public static NBTTagCompound getNBT(ItemStack aStack) {
        NBTTagCompound rNBT = aStack.getTagCompound();
        return ((rNBT == null) ? new NBTTagCompound() : rNBT);
    }

    public static void setBookTitle(ItemStack aStack, String aTitle) {
        NBTTagCompound tNBT = getNBT(aStack);
        tNBT.setString("title", aTitle);
        GTUtility.ItemNBT.setNBT(aStack, tNBT);
    }

    public static ItemStack[] readItemsFromNBT(ItemStack itemstack) {
        NBTTagCompound tNBT = getNBT(itemstack);
        final NBTTagList list = tNBT.getTagList("Items", 10);

        ItemStack[] inventory = new ItemStack[tNBT.getInteger("InventorySlots")];
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound data = list.getCompoundTagAt(i);
            final int slot = data.getInteger("Slot");
            inventory[slot] = ItemStack.loadItemStackFromNBT(data);
        }
        return inventory;
    }

    public static ItemStack writeItemsToNBT(ItemStack itemstack, ItemStack[] stored) {
        NBTTagCompound tNBT = getNBT(itemstack);
        final NBTTagList list = new NBTTagList();
        for (int i = 0; i < stored.length; i++) {
            final ItemStack stack = stored[i];
            if (stack != null) {
                final NBTTagCompound data = new NBTTagCompound();
                stack.writeToNBT(data);
                data.setInteger("Slot", i);
                list.appendTag(data);
            }
        }
        tNBT.setTag("Items", list);
        tNBT.setInteger("InventorySlots", stored.length);
        itemstack.setTagCompound(tNBT);
        return itemstack;
    }

    public static ItemStack writeItemsToNBT(ItemStack itemstack, ItemStack[] stored, String customkey) {
        NBTTagCompound tNBT = getNBT(itemstack);
        final NBTTagList list = new NBTTagList();
        for (int i = 0; i < stored.length; i++) {
            final ItemStack stack = stored[i];
            if (stack != null) {
                final NBTTagCompound data = new NBTTagCompound();
                stack.writeToNBT(data);
                data.setInteger("Slot", i);
                list.appendTag(data);
            }
        }
        tNBT.setTag(customkey, list);
        itemstack.setTagCompound(tNBT);
        return itemstack;
    }

    public static void setBoolean(ItemStack aStack, String aTag, boolean aBoolean) {
        NBTTagCompound tNBT = getNBT(aStack);
        tNBT.setBoolean(aTag, aBoolean);
        GTUtility.ItemNBT.setNBT(aStack, tNBT);
    }

    public static boolean getBoolean(ItemStack aStack, String aTag) {
        NBTTagCompound tNBT = getNBT(aStack);
        return tNBT.getBoolean(aTag);
    }

    public static void setInteger(ItemStack aStack, String aTag, int aInt) {
        NBTTagCompound tNBT = getNBT(aStack);
        tNBT.setInteger(aTag, aInt);
        GTUtility.ItemNBT.setNBT(aStack, tNBT);
    }

    public static int getInteger(ItemStack aStack, String aTag) {
        NBTTagCompound tNBT = getNBT(aStack);
        return tNBT.getInteger(aTag);
    }

    public static void setLong(ItemStack aStack, String aTag, long aInt) {
        NBTTagCompound tNBT = getNBT(aStack);
        tNBT.setLong(aTag, aInt);
        GTUtility.ItemNBT.setNBT(aStack, tNBT);
    }

    public static long getLong(ItemStack aStack, String aTag) {
        NBTTagCompound tNBT = getNBT(aStack);
        return tNBT.getLong(aTag);
    }

    public static void setString(ItemStack aStack, String aTag, String aString) {
        NBTTagCompound tNBT = getNBT(aStack);
        tNBT.setString(aTag, aString);
        GTUtility.ItemNBT.setNBT(aStack, tNBT);
    }

    public static String getString(ItemStack aStack, String aTag) {
        NBTTagCompound tNBT = getNBT(aStack);
        return tNBT.getString(aTag);
    }

    public static boolean hasKey(ItemStack stack, String key) {
        final NBTTagCompound itemData = getNBT(stack);
        return itemData.hasKey(key);
    }

    public static boolean createIntegerTagCompound(ItemStack rStack, String tagName, String keyName, int keyValue) {
        final NBTTagCompound tagMain = getNBT(rStack);
        final NBTTagCompound tagNBT = new NBTTagCompound();
        tagNBT.setInteger(keyName, keyValue);
        tagMain.setTag(tagName, tagNBT);
        rStack.setTagCompound(tagMain);
        return true;
    }

    public static NBTTagCompound getTagCompound(ItemStack aStack, String tagName) {
        NBTTagCompound aNBT = getNBT(aStack);
        if (aNBT != null && hasKey(aStack, tagName)) {
            aNBT = aNBT.getCompoundTag(tagName);
            return aNBT;
        }
        return null;
    }

    public static boolean hasTagCompound(ItemStack aStack) {
        return aStack.hasTagCompound();
    }

    public static void createEmptyTagCompound(ItemStack aStack) {
        if (!hasTagCompound(aStack)) {
            NBTTagCompound aTag = new NBTTagCompound();
            aStack.setTagCompound(aTag);
        }
    }
}

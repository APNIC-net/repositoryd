package net.apnic.rpki.protocol;

import java.util.EnumSet;

/**
 * Flags that can be set on a file list transfer request.
 *
 * @author bje
 * @since 0.9
 */
public enum ItemFlag {
    /**
     * Never set.
     */
    ITEM_REPORT_ATIME (1),

    /**
     * Indicates the item has changed.
     */
    ITEM_REPORT_CHANGE (1<<1),

    /**
     * Failed to set times on a symlink.
     */
    ITEM_REPORT_TIMEFAIL (1<<2), /* symlinks only; obscured by REPORT_SIZE */

    /**
     * Indicates the item has changed size.
     */
    ITEM_REPORT_SIZE (1<<2),     /* regular files only */

    /**
     * Indicates the receiver will change the item's times.
     */
    ITEM_REPORT_TIME (1<<3),

    /**
     * Indicates the receiver will change the item's permissions.
     */
    ITEM_REPORT_PERMS (1<<4),

    /**
     * Indicates the receiver will change the item's owner.
     */
    ITEM_REPORT_OWNER (1<<5),

    /**
     * Indicates the receiver will change the item's group.
     */
    ITEM_REPORT_GROUP (1<<6),

    /**
     * Indicates the receiver will change the item's ACLs.
     */
    ITEM_REPORT_ACL (1<<7),

    /**
     * Indicates the receiver will change the item's xattrs.
     */
    ITEM_REPORT_XATTR (1<<8),

    /**
     * Indicates that a different basis type was on the wire.
     */
    ITEM_BASIS_TYPE_FOLLOWS (1<<11),

    /**
     * Indicates that the name of a fuzzy match was on the wire.
     */
    ITEM_XNAME_FOLLOWS (1<<12),

    /**
     * Indicates the receiver did not have the item.
     */
    ITEM_IS_NEW (1<<13),

    /**
     * Indicates the receiver will overwrite local changes.
     */
    ITEM_LOCAL_CHANGE (1<<14),

    /**
     * Indicates the receiver expects to transfer this item.
     */
    ITEM_TRANSFER (1<<15);

    private final int bitValue;
    private ItemFlag(int bitValue) {
        this.bitValue = bitValue;
    }

    /**
     * Creates an ItemFlag set from a bitfield.
     *
     * Note that ITEM_REPORT_TIMEFAIL and ITEM_REPORT_SIZE will always appear together.
     *
     * @param flags a bitfield of flags to set.
     * @return a set of ItemFlags corresponding to flags
     * @since 0.9
     */
    public static EnumSet<ItemFlag> setFromInt(int flags) {
        EnumSet<ItemFlag> set = EnumSet.noneOf(ItemFlag.class);
        for (ItemFlag value : values()) {
            if ((flags & value.bitValue) != 0) {
                set.add(value);
            }
        }

        return set;
    }

    /**
     * Creates a bitfield from an ItemFlag set.
     *
     * @param set the set of ItemFlags to map
     * @return a bitfield corresponding to the given set
     * @since 0.9
     */
    public static int intFromSet(final EnumSet<ItemFlag> set) {
        int bitfield = 0;
        for (ItemFlag flag : set) {
            bitfield |= flag.bitValue;
        }
        return bitfield;
    }
}

package me.crylonz.deadchest.deps.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.crylonz.deadchest.DeadChestLoader;
import me.crylonz.deadchest.utils.ConfigKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static me.crylonz.deadchest.DeadChestLoader.config;
import static me.crylonz.deadchest.utils.Utils.generateLog;

public class WorldGuardSoftDependenciesChecker {

    public static BooleanFlag DEADCHEST_GUEST_FLAG;
    public static BooleanFlag DEADCHEST_OWNER_FLAG;
    public static BooleanFlag DEADCHEST_MEMBER_FLAG;

    public void load() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            BooleanFlag owner_flag = new BooleanFlag("dc-owner");
            registry.register(owner_flag);
            DEADCHEST_OWNER_FLAG = owner_flag;

            BooleanFlag nobody_flag = new BooleanFlag("dc-guest");
            registry.register(nobody_flag);
            DEADCHEST_GUEST_FLAG = nobody_flag;

            BooleanFlag member_flag = new BooleanFlag("dc-member");
            registry.register(member_flag);
            DEADCHEST_MEMBER_FLAG = member_flag;

        } catch (
                FlagConflictException e) {
            DeadChestLoader.log.warning("Conflict in Deadchest flags");
        }
    }

    public boolean worldGuardChecker(Player p) {

        if (!config.getBoolean(ConfigKey.WORLD_GUARD_DETECTION)) {
            return true;
        }

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(p.getLocation().getWorld()));

            if (regions != null) {
                BlockVector3 position = BlockVector3.at(p.getLocation().getX(),
                        p.getLocation().getY(), p.getLocation().getZ());
                ApplicableRegionSet set = regions.getApplicableRegions(position);

                if (set.size() != 0) {

                    // Wrap player for WorldGuard API
                    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);

                    // Use queryValue to get flag values respecting region priorities and inheritance
                    Boolean ownerFlag = set.queryValue(localPlayer, DEADCHEST_OWNER_FLAG);
                    Boolean memberFlag = set.queryValue(localPlayer, DEADCHEST_MEMBER_FLAG);
                    Boolean guestFlag = set.queryValue(localPlayer, DEADCHEST_GUEST_FLAG);

                    // Check if player is owner or member of any applicable region
                    boolean isOwner = false;
                    boolean isMember = false;
                    for (ProtectedRegion region : set.getRegions()) {
                        if (region.isOwner(localPlayer)) {
                            isOwner = true;
                        }
                        if (region.isMember(localPlayer)) {
                            isMember = true;
                        }
                    }

                    Boolean chestPermission = true;
                    if (ownerFlag != null && !ownerFlag) {
                        if (isOwner) {
                            chestPermission = false;
                        }
                    }
                    if (memberFlag != null && !memberFlag) {
                        if (isMember) {
                            chestPermission = false;
                        }
                    }
                    if (guestFlag != null && !guestFlag) {
                        chestPermission = false;
                    }

                    if(!chestPermission) {
                        generateLog("Player [" + p.getName() + "] died without [Worldguard] region permission : No Deadchest generated");
                        return false;
                    }

                }
            }
            return true;
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().info(e.getMessage());
            return true;
        }
    }

}

package me.crylonz.deadchest.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import me.crylonz.deadchest.ChestData;
import me.crylonz.deadchest.DeadChestLoader;
import me.crylonz.deadchest.Localization;
import me.crylonz.deadchest.Permission;
import me.crylonz.deadchest.utils.ConfigKey;
import me.crylonz.deadchest.utils.DeadChestConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DCCommandRegistrationServiceTest {

    private ServerMock server;
    private DCCommandRegistrationService service;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        Localization localization = new Localization();
        Map<String, Object> values = new HashMap<>();
        values.put("common.prefix", "[DeadChest] ");
        values.put("commands.error.player-only", "Command must be called by a player");
        values.put("commands.config.deprecated.ignore", "Ignore command is deprecated");
        values.put("commands.config.usage", "Config usage");
        values.put("commands.config.get.success", "{0} = {1}");
        values.put("commands.config.set.success", "Set {0} = {1}");
        values.put("commands.config.reset.success", "Reset {0} = {1}");
        values.put("commands.config.edit.unsupported", "{0} cannot be edited interactively");
        values.put("commands.config.error.unknown-key", "Unknown config key: {0}");
        values.put("commands.config.error.invalid-value", "Invalid value for {0}: {1} ({2})");
        localization.set(values);
        DeadChestLoader.local = localization;
        DeadChestLoader.ignoreList = server.createInventory(null, 9);
        DeadChestLoader.plugin = MockBukkit.createMockPlugin();
        DeadChestLoader.config = new DeadChestConfig(DeadChestLoader.plugin);
        for (ConfigKey key : ConfigKey.values()) {
            DeadChestLoader.config.register(key);
        }
        service = new DCCommandRegistrationService(mock(DeadChestLoader.class));
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void registerRemoveOwnRequiresPlayerContext() {
        CommandSender console = mock(CommandSender.class);

        service.register(console, new String[]{"remove"});
        service.registerRemoveOwn();

        assertTrue(service.isCommandSucceed());
        verify(console, atLeastOnce()).sendMessage(contains("Command must be called by a player"));
    }

    @Test
    public void registerIgnoreListOpensInventoryForAdminPlayer() {
        PlayerMock admin = server.addPlayer("Admin");
        admin.addAttachment(MockBukkit.createMockPlugin(), Permission.ADMIN.label, true);
        Inventory ignoreInventory = DeadChestLoader.ignoreList;

        service.register(admin, new String[]{"ignore"});
        service.registerIgnoreList();

        assertTrue(service.isCommandSucceed());
        assertNotNull(admin.getOpenInventory());
        assertSame(ignoreInventory, admin.getOpenInventory().getTopInventory());
    }

    @Test
    public void registerConfigSetUpdatesCanonicalYamlKey() {
        PlayerMock admin = server.addPlayer("Admin");
        admin.addAttachment(MockBukkit.createMockPlugin(), Permission.CONFIG.label, true);

        service.register(admin, new String[]{"config", "set", "localization.language", "fr"});
        service.registerConfigSet();

        assertTrue(service.isCommandSucceed());
        assertEquals("fr", DeadChestLoader.config.getString(ConfigKey.LOCALIZATION_LANGUAGE));
        assertEquals("fr", DeadChestLoader.plugin.getConfig().getString(ConfigKey.LOCALIZATION_LANGUAGE.toString()));
    }

    @Test
    public void registerConfigEditRejectsNonInteractiveKeys() {
        PlayerMock admin = server.addPlayer("Admin");
        admin.addAttachment(MockBukkit.createMockPlugin(), Permission.CONFIG.label, true);

        service.register(admin, new String[]{"config", "edit", "localization.language"});
        service.registerConfigEdit();

        assertTrue(service.isCommandSucceed());
        assertNotSame(DeadChestLoader.ignoreList, admin.getOpenInventory().getTopInventory());
    }

    @Test
    public void registerGiveBackClassicRemovesTrackedChestForTargetPlayer() {
        PlayerMock admin = server.addPlayer("Admin");
        PlayerMock target = server.addPlayer("Steve");
        admin.addAttachment(MockBukkit.createMockPlugin(), Permission.GIVEBACK.label, true);

        ChestData chestData = chestDataAt(40, target);
        DeadChestLoader.getChestDataCache().addChestData(chestData);
        target.getWorld().getBlockAt(chestData.getChestLocation()).setType(Material.CHEST);

        service.register(admin, new String[]{"giveback", target.getName()});
        service.registerGiveBack();

        assertTrue(service.isCommandSucceed());
        assertNull(DeadChestLoader.getChestDataCache().getChestData(chestData.getChestLocation()));
        assertEquals(Material.AIR, target.getWorld().getBlockAt(chestData.getChestLocation()).getType());
    }

    @Test
    public void registerRemoveOtherClassicRemovesTrackedChestForOnlineTargetPlayer() {
        PlayerMock admin = server.addPlayer("Admin");
        PlayerMock target = server.addPlayer("Steve");
        admin.addAttachment(MockBukkit.createMockPlugin(), Permission.REMOVE_OTHER.label, true);

        ChestData chestData = chestDataAt(41, target);
        DeadChestLoader.getChestDataCache().addChestData(chestData);
        target.getWorld().getBlockAt(chestData.getChestLocation()).setType(Material.CHEST);

        service.register(admin, new String[]{"remove", target.getName()});
        service.registerRemoveOther();

        assertTrue(service.isCommandSucceed());
        assertNull(DeadChestLoader.getChestDataCache().getChestData(chestData.getChestLocation()));
        assertEquals(Material.AIR, target.getWorld().getBlockAt(chestData.getChestLocation()).getType());
    }

    private ChestData chestDataAt(int x, PlayerMock player) {
        UUID timerId = UUID.nameUUIDFromBytes(("timer-" + x).getBytes());
        UUID ownerId = UUID.nameUUIDFromBytes(("owner-" + x).getBytes());
        Location chestLocation = new Location(player.getWorld(), x, 64, x, 0f, 0f);
        Location holoLocation = new Location(player.getWorld(), x, 65, x, 0f, 0f);

        List<ItemStack> inventory = new ArrayList<>();
        inventory.add(new ItemStack(Material.DIAMOND, 1));

        return new ChestData(
                inventory,
                chestLocation,
                player.getName(),
                player.getUniqueId(),
                new Date(1_700_000_000_000L + x),
                false,
                false,
                holoLocation,
                timerId,
                null,
                ownerId,
                player.getWorld().getName(),
                0
        );
    }
}

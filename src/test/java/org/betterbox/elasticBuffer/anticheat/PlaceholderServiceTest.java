package org.betterbox.elasticBuffer.anticheat;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderServiceTest {

    @Test
    void replacesBasicPlaceholders() throws Exception {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Tester");
        Mockito.when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("192.168.0.5"), 25565);
        Mockito.when(player.getAddress()).thenReturn(address);

        PlaceholderService service = new PlaceholderService();
        String command = service.applyPlaceholders("ban %player_name% %player_uuid% %player_ip% %check_type% %severity% %count%",
                player, CheckType.KILLAURA, 7.5, 3);

        assertTrue(command.contains("Tester"));
        assertTrue(command.contains("00000000-0000-0000-0000-000000000001"));
        assertTrue(command.contains("192.168.0.5"));
        assertTrue(command.contains(CheckType.KILLAURA.name()));
        assertTrue(command.contains("7.5"));
        assertTrue(command.contains("3"));
    }
}

package tc.yigit.shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Base64;
import net.md_5.bungee.api.ProxyServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tc.yigit.bukkit.BukkitEvents;
import tc.yigit.bukkit.BukkitMain;
import tc.yigit.bungeecord.BungeeEvents;
import tc.yigit.bungeecord.BungeeMain;
import tc.yigit.events.EventManager;
import tc.yigit.shared.SocketConfig;


public class UtilSocket {

    private static EventManager manager;

    private static final boolean isFolia;



    static {

        boolean foliaFound = false;

        try {

            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");

            foliaFound = true;

        }

        catch (ClassNotFoundException e) {

        }

        isFolia = foliaFound;

    }



    public static EventManager getManager() {

        if (manager == null) {

            if (SocketConfig.isBukkit()) {

                manager = new BukkitEvents();

            }

            if (SocketConfig.isBungee()) {

                manager = new BungeeEvents();

            }

        }

        return manager;

    }



    public static String hash(String input) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-512");

            md.update(input.getBytes());

            String result = new BigInteger(1, md.digest()).toString(16);

            if (result.length() % 2 != 0) {

                result = "0" + result;

            }

            return result;

        }

        catch (Exception ex) {

            return "";

        }

    }



    public static String readString(DataInputStream in, boolean base64) throws IOException {

        int stringSize = in.readInt();

        StringBuilder buffer = new StringBuilder();

        int i = 0;

        while (i < stringSize) {

            buffer.append(in.readChar());

            ++i;

        }

        return base64 ? UtilSocket.DecodeBASE64(buffer.toString()) : buffer.toString();

    }



    public static String DecodeBASE64(String text) throws UnsupportedEncodingException {

        byte[] bytes = Base64.getDecoder().decode(text);

        return new String(bytes, "UTF-8");

    }



    public static void writeString(DataOutputStream out, String string) throws IOException {

        out.writeInt(string.length());

        out.writeChars(string);

    }



    public static void sendCommand(String command, DataOutputStream out) throws IOException {

        try {

            if (SocketConfig.isBungee()) {

                boolean success = BungeeMain.sendCommand(command);

                out.writeInt(success ? 1 : 0);

                

            } else if (SocketConfig.isBukkit()) {

                JavaPlugin plugin = BukkitMain.getPlugin();

                if (plugin == null) {

                    throw new IllegalStateException("WebSender's BukkitMain plugin instance is null!");

                }



                if (isFolia) {

                    Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                    });

                } else {

                    Bukkit.getScheduler().runTask(plugin, () -> {

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                    });

                }

                

                out.writeInt(1);

            }

        } catch (Exception ex) {

            UtilSocket.createLog(String.valueOf(String.valueOf(SocketConfig.prefix)) + "ERROR: " + ex.getMessage());

            ex.printStackTrace();

            out.writeInt(0);

        }

        

        out.flush();

    }



    public static void createLog(String data) {

        if (SocketConfig.consoleInfo.equals("true")) {

            if (SocketConfig.isBukkit()) {

                Bukkit.getConsoleSender().sendMessage(String.valueOf(String.valueOf(SocketConfig.prefix)) + data);

            } else {

                ProxyServer.getInstance().getConsole().sendMessage(String.valueOf(String.valueOf(SocketConfig.prefix)) + data);

            }

        }

    }



    public static void sendPlayerMsg(String playerName, String data) {

        if (SocketConfig.isBukkit()) {

            Bukkit.getPlayer(playerName).sendMessage(String.valueOf(String.valueOf(SocketConfig.prefix)) + data);

        } else {

            ProxyServer.getInstance().getPlayer(playerName).sendMessage(String.valueOf(String.valueOf(SocketConfig.prefix)) + data);

        }

    }

}

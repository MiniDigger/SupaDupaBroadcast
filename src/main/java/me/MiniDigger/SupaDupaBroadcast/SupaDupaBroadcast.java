package me.MiniDigger.SupaDupaBroadcast;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SupaDupaBroadcast extends JavaPlugin {

    private List<String> messages;
    private long interval;
    private BukkitRunnable task;
    private int index = 0;

    private List<UUID> dontBugMe;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        loadMessages();

        restartBroadcast();
    }

    private void restartBroadcast() {
        index = 0;

        if ( task != null ) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                String msg = messages.get( index );
                if ( msg.startsWith( "json:" ) ) {
                    broadCastJson( msg );
                } else {
                    String fmsg = parseMsg( msg );
                    Bukkit.getOnlinePlayers().stream().filter( p -> !dontBugMe.contains( p.getUniqueId() ) ).forEach( p -> {
                        p.sendMessage( fmsg.replace( "%player", p.getName() ) );
                    });

                    if (getConfig().getBoolean("console-output")) {
                        Bukkit.getConsoleSender().sendMessage( msg.replace( "%player", "Console" ) );
                    }
                }

                index++;
                if ( index >= messages.size() ) {
                    index = 0;
                }
            }
        };
        task.runTaskTimer( this, interval, interval );
    }

    private void broadCastJson( String msg ) {
        msg = msg.replace( "json:", "" );

        String finalMsg = msg;
        Bukkit.getOnlinePlayers().stream().filter(p -> !dontBugMe.contains( p.getUniqueId() ) ).forEach(p -> {
            BaseComponent[] bc = ComponentSerializer.parse( finalMsg.replace("%player", p.getName() ) );
            p.spigot().sendMessage( bc );
        });

        BaseComponent[] bc = ComponentSerializer.parse( msg );
        if (getConfig().getBoolean("console-output")) {
            StringBuilder sb = new StringBuilder();
            for ( BaseComponent b : bc ) {
                sb.append( b.toLegacyText() );
            }
            
            Bukkit.getConsoleSender().sendMessage( sb.toString().replace( "%player", "Console" ) );
        }
    }

    private void loadMessages() {
        messages = getConfig().getStringList( "messages" );
        if ( messages == null || messages.size() == 0 ) {
            getLogger().warning( "Could not load messages from config, setting default one" );
            messages = new ArrayList<>();
            messages.add( "Default message: You can change this message this plugins config.yml" );
            getConfig().set( "messages", messages );
            saveConfig();
        }

        interval = getConfig().getInt( "interval" );
        if ( interval == 0 ) {
            getLogger().warning( "Could not load interval from config, setting default one (" + 20 * 60 + ")" );
            interval = 20 * 60;
            getConfig().set( "interval", interval );
            saveConfig();
        }

        dontBugMe = new ArrayList<>();
        dontBugMe.addAll( getConfig().getStringList( "dont-bug-me" ).stream().map( UUID::fromString ).collect( Collectors.toList() ) );
    }


    private String parseMsg( String msg ) {
        msg = ChatColor.translateAlternateColorCodes( '&', msg );

        return msg;
    }

    @Override
    public boolean onCommand( CommandSender sender, Command command, String label, String[] args ) {
        switch ( command.getName() ) {
            case "sdbadd":
                if ( args.length == 1 ) {
                    messages.add( args[0] );
                } else if ( args.length > 1 ) {
                    StringBuilder sb = new StringBuilder();
                    for ( String s : args ) {
                        sb.append( s );
                        sb.append( " " );
                    }

                    messages.add( sb.toString() );
                } else {
                    sender.sendMessage( ChatColor.RED + "You need to provide at least on argument!" );
                    return true;
                }

                getConfig().set( "messages", messages );
                saveConfig();

                sender.sendMessage( ChatColor.GREEN + "Message was added and config was saved!" );
                return true;
            case "sdbrl":
                reloadConfig();
                loadMessages();
                restartBroadcast();

                sender.sendMessage( ChatColor.GREEN + "Reloaded!" );
                return true;
            case "sdbint":
                if ( args.length != 1 ) {
                    sender.sendMessage( ChatColor.RED + "You need to specify a new interval!" );
                    return true;
                }

                try {
                    interval = Integer.parseInt( args[0] );
                } catch ( NumberFormatException e ) {
                    sender.sendMessage( ChatColor.RED + "The new interval needs to be a number!" );
                    return true;
                }

                getConfig().set( "interval", interval );
                saveConfig();
                restartBroadcast();

                sender.sendMessage( ChatColor.GREEN + "Interval was set to " + interval + " and config was saved!" );
                return true;
            case "sbignore":
                if ( !( sender instanceof Player ) ) {
                    sender.sendMessage( ChatColor.RED + "This command can only be executed as player!" );
                    return true;
                }

                Player player = (Player) sender;

                if ( dontBugMe.contains( player.getUniqueId() ) ) {
                    dontBugMe.remove( player.getUniqueId() );
                    sender.sendMessage( ChatColor.GREEN + "You will now get broadcasts again!" );
                } else {
                    dontBugMe.add( player.getUniqueId() );
                    sender.sendMessage( ChatColor.GREEN + "You will not get any broadcast now!" );
                }

                List<String> l = dontBugMe.stream().map( UUID::toString ).collect( Collectors.toList() );
                getConfig().set( "dont-bug-me", l );
                saveConfig();

                return true;
            default:
                return false;
        }
    }
}

package com.eerussianguy.blazemap.feature.waypoints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.lib.Helpers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class WaypointSharing {

    public static void onChatReceive(ClientChatReceivedEvent event){
        Component incomingMessage = event.getMessage();
        
        Component msg = parseChatMessage(incomingMessage); // could be one line.
        event.setMessage(msg);
    }

    public static void shareWaypoint(Waypoint waypoint) {
        BlockPos pos = waypoint.getPosition();
        ResourceKey<Level> dimension = waypoint.getDimension();
        String name = waypoint.getName();
        
        String format = "[name:%1$s, x:%2$d, y:%3$d, z:%4$d, dim:%5$s]";

        String msg = String.format(format, name, pos.getX(), pos.getY(), pos.getZ(), dimension.getRegistryName());

        // Minecraft.getInstance().setScreen(new ChatScreen(msg));
        Helpers.getPlayer().chat(msg);
    }

    
    /** 
     * Returns the same message from incomingMessage with waypoints highlighted.
     * If there's no waypoint then the same message is sent back
     * 
     * @param incomingMessage A Minecraft Component
     * @return Component
     */
    private static Component parseChatMessage(Component incomingMessage) {
        String chatMessage = incomingMessage.getString();

        // Check if a waypoint might be in chat
        Pattern pattern = Pattern.compile("\\[[^\\[\\]]*,[^\\[\\]]*\\]"); // this doesn't guarentee that a waypoint exists int the message. Just that it's likely to exits
        Matcher matcher = pattern.matcher(chatMessage);
        boolean matchFound = matcher.find();

        // Return original message if no waypoint is found
        if (! matchFound) {
            return incomingMessage;
        }
        
        // Seperatae the waypoint text from
        BaseComponent parsedChat = new TextComponent("");
        int previousEnd = 0;
        do {
            parsedChat.append(chatMessage.substring(previousEnd, matcher.start())); // Add the message before the waypoint
            previousEnd = matcher.end();
            parsedChat.append(parseWaypoint(matcher.group()));
            matchFound = matcher.find();
        } while (matchFound);
        parsedChat.append(chatMessage.substring(previousEnd, chatMessage.length()));

        return parsedChat;
    }

    
    /**
     * Checks if the given string is in a waypoint format. Applies formatting to the text if it is and empty text if it's not. 
     * @param input The string to be checked. Should be wrapped in square brackets
     * @return TextComponent The given string with appropriate formatting
     */
    private static TextComponent parseWaypoint(String input) { // This should probably return a waypoint instead but I don't know if the API will allow partially formed Waypoints.
        String workingString = input.substring(1, input.length()-1); // remove the []  from the beginning and end.
        Pattern pattern = Pattern.compile("(?<parameter>\\w*):(?<value>[^,\\n\\r]*)");
        Matcher matcher = pattern.matcher(workingString);
        boolean matchFound = matcher.find();

        TextComponent output = new TextComponent(input);

        // return original message unmodified if no match is found
        if (!matchFound) {
            return output;
        }

        // parse the message and get all the valid fields.
        Integer x, y, z; // Maybe use Options here
        x = y = z = null;
        String name, dimension; // TODO change dimension to Resource Key level
        String parameter, value;

        do {
            parameter = matcher.group("parameter").toLowerCase();
            value = matcher.group("value").trim().toLowerCase();
            try {
                switch (parameter) {
                    case "name":
                        name = value;
                        break;
                    case "x":
                        x = Integer.parseInt(value);
                        break;
                    case "y":
                        y = Integer.parseInt(value);
                        break;
                    case "z":
                        z = Integer.parseInt(value);
                        break;
                    case "dim":
                        dimension = value;
                        break;
                    default:
                        BlazeMap.LOGGER.info("Unsupported parameter :'%s'", parameter);
                        break;
                }
            } catch (NumberFormatException exception) {
                BlazeMap.LOGGER.error(String.format("Can't parse '%s' to integer", value), exception);
            } 

            matchFound = matcher.find();
        } while (matchFound);

        
        if (x == null || z == null) {
            return output;
        }

        output.withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.AQUA);

        return output;
    }

    
}

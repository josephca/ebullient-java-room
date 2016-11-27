/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.sample;

import java.util.Locale;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.websocket.Session;

import org.gameontext.sample.map.client.MapClient;
import org.gameontext.sample.protocol.Message;
import org.gameontext.sample.protocol.RoomEndpoint;
import org.gameontext.sample.rest.HealthEndpoint;
import org.gameontext.sample.rest.RestApplication;

/**
 * Here is where your room implementation lives. 
 * 
 * <p>
 * A JAX-RS application is defined in {@link RestApplication}, 
 * with a basic health check endpoint in {@link HealthEndpoint}.
 * <p>
 * The WebSocket endpoint is defined in {@link RoomEndpoint}, 
 * with {@link Message} as the text-based payload being sent on the wire.
 * <p>
 * This is an ApplicationScoped CDI bean, which means it will be started
 * when the server/application starts, and stopped when it stops.
 */
@ApplicationScoped
public class RoomImplementation {

    /** The id of the room: you can retrieve this from the room editing view in the UI */
    public static final String ROOM_ID = "TheGeneratedIdOfThisRoom";
    public static final String LOOK_UNKNOWN = "It doesn't look interesting";
    public static final String UNKNOWN_COMMAND = "This room is a basic model. It doesn't understand `%s`";
    public static final String UNSPECIFIED_DIRECTION = "You didn't say which way you wanted to go.";
    public static final String UNKNOWN_DIRECTION = "There isn't a door in that direction (%s)";
    public static final String GO_FORTH = "You head %s";
    public static final String HELLO_ALL = "%s is here";
    public static final String HELLO_USER = "Welcome!";
    public static final String GOODBYE_ALL = "%s has gone";
    public static final String GOODBYE_USER = "Bye!";

    /**
     * The room id: this is translated from the ROOM_ID environment variable into
     * a JNDI value by server.xml (Liberty)
     */
    @Resource(lookup = "roomId")
    protected String roomId;

    @Inject
    protected MapClient mapClient;

    protected RoomDescription roomDescription = new RoomDescription();

    @PostConstruct
    protected void postConstruct() {

        if ( roomId == null || roomId.contains("ROOM_ID") ) {
            // The room id was not set by the environment; make one up.
            roomId = ROOM_ID;
        } else {
            // we have a custom room id! let's see what the map thinks.
            mapClient.updateRoom(roomId, roomDescription);
        }

        // Customize the room
    	
        roomDescription.addCommand("/use", "Take, hold, or deploy (something) as a means of accomplishing or achieving something");

        roomDescription.addItem("red teddy bear");
        roomDescription.addItem("big pile of mud");
        roomDescription.addItem("moon diagram");
        roomDescription.addItem("bookshelf");

        Log.log(Level.INFO, this, "Room initialized: {0}", roomDescription);
    }

    @PreDestroy
    protected void preDestroy() {
        Log.log(Level.FINE, this, "Room to be destroyed");
    }

    public void handleMessage(Session session, Message message, RoomEndpoint endpoint) {

        // If this message isn't for this room, TOSS IT!
//        if ( !roomId.equals(message.getTargetId()) ) {
//            Log.log(Level.FINEST, this, "Received message for the wrong room ({0}): {1}", message.getTargetId(), message);
//            return;
//        }

        // Fetch the userId and the username of the sender.
        // The username can change overtime, so always use the sent username when
        // constructing messages
        JsonObject messageBody = message.getParsedBody();
        String userId = messageBody.getString(Message.USER_ID);
        String username = messageBody.getString(Message.USERNAME);

        Log.log(Level.FINEST, this, "Received message from {0}({1}): {2}", username, userId, messageBody);

        // Who doesn't love switch on strings in Java 8?
        switch(message.getTarget()) {

        case roomHello:
            //		roomHello,<roomId>,{
            //		    "username": "username",
            //		    "userId": "<userId>",
            //		    "version": 1|2
            //		}
            // See RoomImplementationTest#testRoomHello*

            // Send location message
            endpoint.sendMessage(session, Message.createLocationMessage(userId, roomDescription));

            // Say hello to a new person in the room
            endpoint.sendMessage(session,
                    Message.createBroadcastEvent(
                            String.format(HELLO_ALL, username),
                            userId, HELLO_USER));
            break;

        case roomJoin:
            //		roomJoin,<roomId>,{
            //		    "username": "username",
            //		    "userId": "<userId>",
            //		    "version": 2
            //		}
            // See RoomImplementationTest#testRoomJoin

            // Send location message
            endpoint.sendMessage(session, Message.createLocationMessage(userId, roomDescription));

            break;

        case roomGoodbye:
            //		roomGoodbye,<roomId>,{
            //		    "username": "username",
            //		    "userId": "<userId>"
            //		}
            // See RoomImplementationTest#testRoomGoodbye

            // Say goodbye to person leaving the room
            endpoint.sendMessage(session,
                    Message.createBroadcastEvent(
                            String.format(GOODBYE_ALL, username),
                            userId, GOODBYE_USER));
            break;

        case roomPart:
            //		room,<roomId>,{
            //		    "username": "username",
            //		    "userId": "<userId>"
            //		}
            // See RoomImplementationTest#testRoomPart

            break;

        case room:
            //		room,<roomId>,{
            //		    "username": "username",
            //		    "userId": "<userId>"
            //		    "content": "<message>"
            //		}
            String content = messageBody.getString(Message.CONTENT);

            if ( content.charAt(0) == '/' ) {
                // command
                processCommand(userId, username, content, endpoint, session);
            } else {
                // See RoomImplementationTest#testHandleChatMessage

                // echo back the chat message
                endpoint.sendMessage(session,
                        Message.createChatMessage(username, content));
            }
            break;

        default:
            // unknown message type. discard, don't break.
            break;
        }
    }

    private void processCommand(String userId, String username, String content, RoomEndpoint endpoint, Session session) {
        // Work mostly off of lower case.
        String contentToLower = content.toLowerCase(Locale.ENGLISH).trim();

        String firstWord;
        String remainder;

        int firstSpace = contentToLower.indexOf(' '); // find the first space
        if ( firstSpace < 0 || contentToLower.length() <= firstSpace ) {
            firstWord = contentToLower;
            remainder = null;
        } else {
            firstWord = contentToLower.substring(0, firstSpace);
            remainder = contentToLower.substring(firstSpace+1);
        }

        switch(firstWord) {
            case "/go":
                // See RoomCommandsTest#testHandle*Go*
                // Always process the /go command.
                String exitId = getExitId(remainder);

                if ( exitId == null ) {
                    // Send error only to source session
                    if ( remainder == null ) {
                        endpoint.sendMessage(session,
                                Message.createSpecificEvent(userId, UNSPECIFIED_DIRECTION));
                    } else {
                        endpoint.sendMessage(session,
                                Message.createSpecificEvent(userId, String.format(UNKNOWN_DIRECTION, remainder)));
                    }
                } else {
                    // Allow the exit
                    endpoint.sendMessage(session,
                            Message.createExitMessage(userId, exitId, String.format(GO_FORTH, prettyDirection(exitId))));
                }
                break;

            case "/look":
            case "/examine":
                // See RoomCommandsTest#testHandle*Look*
            	if ( remainder == null ||  remainder.contains("room") ) {
                    // This is looking at or examining the entire room. Send the player location message,
                    // which includes the room description and inventory
                    endpoint.sendMessage(session, Message.createLocationMessage(userId, roomDescription));

            	} else if ( remainder.contains("moon diagram") ) {
                    endpoint.sendMessage(session, 
                            Message.createBroadcastEvent(username + " picks up the moon diagram and looks at it fondly before dropping it again", 
                            		userId, "You pick it up, read it, and love it for no reason. You put it down."));

            	} else if ( remainder.contains("mud") ) {
                    endpoint.sendMessage(session, 
                            Message.createBroadcastEvent(username + " is disgusted by mud on the floor", 
                            		userId, "It looks awful. You look away."));

            	} else if ( remainder.contains("teddy") ) {
                    endpoint.sendMessage(session, 
                            Message.createBroadcastEvent("The teddy bear burps, 'Hello'"));
 
            	} else if ( remainder.contains("books") ) {
                    endpoint.sendMessage(session, 
                            Message.createBroadcastEvent(username + " is confused by the bookshelf", 
                            		userId, "It's a bit odd"));

            	} else {
                    endpoint.sendMessage(session, Message.createSpecificEvent(userId, LOOK_UNKNOWN));
                }
                break;

            case "/use":
                // Custom command! 

                if ( remainder != null ) {
                	if ( remainder.contains("teddy") ) {
                        endpoint.sendMessage(session, 
                                Message.createBroadcastEvent("The teddy bear squeaks! " + username + " looks around sheepishly, and sets the teddy back down.",
                                		userId, "You pick up the teddy and put it in your mouth. It squeaks!! You quickly put it back down."));
                        break;
                	} else if ( remainder.contains("mud") ) {
                        endpoint.sendMessage(session, 
	                        Message.createBroadcastEvent(username + " has very dirty hands.",
	                        		userId, "You pat the big pile of mud. It's very sticky, and now it's all over your hands!"));
                        break;
                	} else if ( remainder.contains("moon diagram") ) {
                        endpoint.sendMessage(session, 
    	                        Message.createBroadcastEvent(username + " picks up the moon diagram, and scrunches it into a ball! After a brief nod, " + username + " smooths it out again, and lets it float back to the floor",
    	                        		userId, "You grab the moon diagram and crumple it into a ball. Hey! That looks like a moon! How satisfying! You unfold it, and let it go."));
                        break;
                	} else if ( remainder.contains("books") ) {
                        endpoint.sendMessage(session,
                                Message.createExitMessage(userId, "W", 
                                		"You take a book down from the shelf, but it vanishes in your hand. Hey.. what? .. You're going West!"));
                        break;
                	}
                }
                endpoint.sendMessage(session, Message.createSpecificEvent(userId, "You have no idea how to use that"));

                break;

            default:
                endpoint.sendMessage(session,
                        Message.createSpecificEvent(userId, String.format(UNKNOWN_COMMAND, content)));
                break;
        }
    }


    /**
     * Given a lower case string describing the direction someone wants
     * to go (/go N, or /go North), filter or transform that into a recognizable
     * id that can be used as an index into a known list of exits. Always valid
     * are n, s, e, w. If the string doesn't match a known exit direction,
     * return null.
     *
     * @param lowerDirection String read from the provided message
     * @return exit id or null
     */
    protected String getExitId(String lowerDirection) {
        if (lowerDirection == null) {
            return null;
        }

        switch(lowerDirection) {
            case "north" :
            case "south" :
            case "east" :
            case "west" :
                return lowerDirection.substring(0,1);

            case "n" :
            case "s" :
            case "e" :
            case "w" :
                // Assume N/S/E/W are managed by the map service.
                return lowerDirection;

            default  :
                // Otherwise unknown direction
                return null;
        }
    }

    /**
     * From the direction we used as a key
     * @param exitId The exitId in lower case
     * @return A pretty version of the direction for use in the exit message.
     */
    protected String prettyDirection(String exitId) {
        switch(exitId) {
            case "n" : return "North";
            case "s" : return "South";
            case "e" : return "East";
            case "w" : return "West";

            default  : return exitId;
        }
    }

    public boolean ok() {
        return mapClient.ok();
    }
}

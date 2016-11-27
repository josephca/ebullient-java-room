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

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

import org.gameontext.sample.protocol.Message;
import org.gameontext.sample.protocol.RoomEndpoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class RoomCommandsTest {

    public static final String TEST_ID = "testId";
    public static final String TEST_USERNAME = "testUser";

    @Rule
    public TestName testName = new TestName();

    private RoomImplementation roomImpl;
    private RoomDescription roomDescription;

    @Before
    public void before() {
        System.out.println(" ===== " + testName.getMethodName());

        roomImpl = new RoomImplementation();

        // Might need a mock of map client for postConstruct..

        roomImpl.postConstruct();
        roomDescription = roomImpl.roomDescription;
    }


    @Test
    public void testHandleGoMessageNoDirection(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"content\":{\"testId\":\"" ));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }

    @Test
    public void testHandleGoMessageCardinalDirection(@Mocked Session session, @Mocked RoomEndpoint endpoint) {

        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go N");
        roomImpl.handleMessage(session, message, endpoint);

        message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go north");
        roomImpl.handleMessage(session, message, endpoint);

        message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go East");
        roomImpl.handleMessage(session, message, endpoint);

        message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go e");
        roomImpl.handleMessage(session, message, endpoint);

        message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go s");
        roomImpl.handleMessage(session, message, endpoint);

        message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go w");
        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            List<Message> m = new ArrayList<>();
            endpoint.sendMessage(session, withCapture(m)); times = 6;

            String s = m.get(0).toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to specific user, and of type exit: " + s,
                    s.startsWith("playerLocation,testId,{\"type\":\"exit\""));

            Assert.assertTrue("exitId should be n: " + s,
                    s.contains("\"exitId\":\"n\""));

            Assert.assertTrue("content should explain going North: " + s,
                    s.contains("\"content\":\"" + String.format(RoomImplementation.GO_FORTH, "North")));


            s = m.get(1).toString();
            System.out.println(s);

            Assert.assertTrue("exitId should be n: " + s,
                    s.contains("\"exitId\":\"n\""));

            Assert.assertTrue("content should explain going North: " + s,
                    s.contains("\"content\":\"" + String.format(RoomImplementation.GO_FORTH, "North")));

            s = m.get(2).toString();
            System.out.println(s);

            Assert.assertTrue("exitId should be n: " + s,
                    s.contains("\"exitId\":\"e\""));

            Assert.assertTrue("content should explain going North: " + s,
                    s.contains("\"content\":\"" + String.format(RoomImplementation.GO_FORTH, "East")));

            s = m.get(3).toString();
            System.out.println(s);

            Assert.assertTrue("exitId should be n: " + s,
                    s.contains("\"exitId\":\"e\""));

            Assert.assertTrue("content should explain going North: " + s,
                    s.contains("\"content\":\"" + String.format(RoomImplementation.GO_FORTH, "East")));

            s = m.get(4).toString();
            System.out.println(s);

            Assert.assertTrue("exitId should be n: " + s,
                    s.contains("\"exitId\":\"s\""));

            Assert.assertTrue("content should explain going North: " + s,
                    s.contains("\"content\":\"" + String.format(RoomImplementation.GO_FORTH, "South")));

            s = m.get(5).toString();
            System.out.println(s);

            Assert.assertTrue("exitId should be n: " + s,
                    s.contains("\"exitId\":\"w\""));

            Assert.assertTrue("content should explain going North: " + s,
                    s.contains("\"content\":\"" + String.format(RoomImplementation.GO_FORTH, "West")));
        }};
    }

    @Test
    public void testHandleGoMessageOtherDirection(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/go Up");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            // the request to exit to an unknown direction results in a regular event,
            // specific to the user, rather than a playerLocation message with type=exit
            Assert.assertTrue("Message should be directed to specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should explain there is no door in the requested direction (lowercase): " + s,
                    s.contains("\"content\":{\"testId\":\""+ String.format(RoomImplementation.UNKNOWN_DIRECTION, "up")));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }

    @Test
    public void testHandleAlmostGo(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/goe Stuff");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            // unknown command
            Assert.assertTrue("Message should be directed to specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"content\":{\"testId\":\""));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }

    @Test
    public void testHandleLookRoom(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/look");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            RoomImplementationTest.assertLocationResponse(s, roomDescription);
        }};
    }

    @Test
    public void testHandleExamineRoom(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/examine");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            RoomImplementationTest.assertLocationResponse(s, roomDescription);
        }};
    }

    @Test
    public void testHandleLookOther(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/look stuff");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"content\":{\"testId\":\""));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }

    @Test
    public void testHandleAlmostLook(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/looks stuff");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            // unknown command
            Assert.assertTrue("Message should be directed to specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"content\":{\"testId\":\"" ));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }

    @Test
    public void testHandleUse(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/use");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to a specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"testId\":\"You have no idea how to use that"));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }

    @Test
    public void testHandleUseBooks(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/use books");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to specific player, and of type exit: " + s,
                    s.startsWith("playerLocation,testId,{\"type\":\"exit\""));

            Assert.assertTrue("Message should contain message about taking a book: " + s,
                    s.contains("You take a book down from the shelf"));

            Assert.assertTrue("Message should contain message about going west: " + s,
                    s.contains("\"exitId\":\"W"));
        }};
    }
    
    @Test
    public void testHandleUseTeddy(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/use red teddy bear");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to all users, and of type event: " + s,
                    s.startsWith("player,*,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"testId\":\"You pick up the teddy"));

            Assert.assertTrue("Message should contain content for everyone else: " + s,
                    s.contains("\"*\":\"The teddy bear squeaks!"));
        }};
    }
    
    @Test
    public void testHandleUseMud(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/use mud");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to all users, and of type event: " + s,
                    s.startsWith("player,*,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"testId\":\"You pat the big pile of mud."));

            Assert.assertTrue("Message should contain content for everyone else: " + s,
                    s.contains("\"*\":\"testUser has very dirty hands"));
        }};
    }
    
    @Test
    public void testHandleUseMoonDiagram(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/use moon diagram");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            Assert.assertTrue("Message should be directed to all users, and of type event: " + s,
                    s.startsWith("player,*,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"testId\":\"You grab the moon diagram"));

            Assert.assertTrue("Message should contain content for everyone else: " + s,
                    s.contains("\"*\":\"testUser picks up the moon diagram"));
        }};
    }

    @Test
    public void testHandleUseMoon(@Mocked Session session, @Mocked RoomEndpoint endpoint) {
        Message message = Message.createRoomMessage(roomImpl.roomId, TEST_ID, TEST_USERNAME, "/use moon");

        roomImpl.handleMessage(session, message, endpoint);

        new Verifications() {{
            Message m1;
            endpoint.sendMessage(session, m1 = withCapture()); times = 1;

            String s = m1.toString();
            System.out.println(s);

            // unknown command
            Assert.assertTrue("Message should be directed to specific user, and of type event: " + s,
                    s.startsWith("player,testId,{\"type\":\"event\""));

            Assert.assertTrue("Message should contain content for the specific user: " + s,
                    s.contains("\"content\":{\"testId\":\"" ));

            Assert.assertFalse("Message should not contain wildcard: " + s,
                    s.contains("*"));
        }};
    }
}

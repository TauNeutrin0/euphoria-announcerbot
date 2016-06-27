import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.Bot;
import euphoria.FileIO;
import euphoria.RoomNotConnectedException;
import euphoria.events.ConnectMessageEventListener;
import euphoria.events.ConsoleEventListener;
import euphoria.events.MessageEvent;
import euphoria.events.MessageEventListener;
import euphoria.events.PacketEvent;
import euphoria.events.PacketEventListener;
import euphoria.events.ReplyEventListener;
import euphoria.events.StandardEventListener;
import euphoria.packets.commands.Send;
import euphoria.packets.events.SendEvent;
import euphoria.packets.replies.SendReply;


public class AnnouncerBot extends Bot {
    Map<String, Integer> map      = new HashMap<String, Integer>();
    FileIO               dataFile;
    int                  msgLimit = 100;
    String               announcement = "";
    String               room     = "bots";

    public AnnouncerBot() {
        dataFile = new FileIO("exampleBot_data");
        useCookies(dataFile);

        listeners.add(PacketEventListener.class,
                      new StandardEventListener(this, "AnnouncerBot",
                                                "This is a bot made by TauNeutrin0 that will announce a message at a set frequency."));

        ConnectMessageEventListener cMEL = new ConnectMessageEventListener("AnnouncerBot", this, dataFile).connectAll();
        addConsoleListener(cMEL);

        addConsoleListener(new ConsoleEventListener() {

            @Override
            public void onCommand(String command) {
                if (command.matches("^!announcement [\\s\\S]+$")) {
                    Pattern r = Pattern.compile("^!announcement ([\\s\\S]+)$");
                    Matcher m = r.matcher(command);
                    if (m.find()) {
                        announcement = m.group(1);
                        System.out.println("Set announcement to:\n" + m.group(1));
                    }
                } else if (command.matches("^!messages [0-9]+$")) {
                    Pattern r = Pattern.compile("^!messages ([0-9]+)$");
                    Matcher m = r.matcher(command);
                    if (m.find()) {
                        msgLimit = Integer.parseInt(m.group(1));
                        System.out.println("Set number of messages to " + m.group(1) + ".");
                    }
                } else if (command.matches("^!room &[\\S]+$")) {
                    Pattern r = Pattern.compile("^!room &([\\S]+)$");
                    Matcher m = r.matcher(command);
                    if (m.find()) {
                        room = m.group(1);
                        System.out.println("Set room to " + m.group(1) + ".");
                    }
                } else if (command.matches("^!status$")) {
                    System.out.println("\n"+(map.size()-1)+"/"+msgLimit+" messages in &"+room+".");
                    System.out.println("Announcement: \n"+announcement+"\n");
                }
            }
        });

        listeners.add(PacketEventListener.class, cMEL);
        listeners.add(PacketEventListener.class, new MessageEventListener() {
            @Override
            public void onSendEvent(MessageEvent evt) {
                if (evt.getRoomConnection().getRoom().equals(room)) {
                    String parentId = ((SendEvent)evt.getPacket().getData()).getParent();
                    if (parentId == null) {
                        try {
                            map.put(evt.getId(), 0);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        if (map.size()-1 >= msgLimit) {
                            evt.getRoomConnection().sendMessage(announcement);
                            map.clear();
                        }
                    } else {
                        if (map.containsKey(parentId)) {
                            if (map.get(parentId) < 5) {
                                map.put(evt.getId(), 0);
                                map.replace(parentId, map.get(parentId) + 1);
                                if (map.size()-1 >= msgLimit) {
                                    announce();
                                    map.clear();
                                }
                            }
                        }
                    }
                }
            }
        });

        System.out.println("\n#####\nUse \"!announcement [message]\" to change the announcement.\n"
                           + "Use \"!messages [number]\" to change the number of messages after which to trigger.\n"
                           + "Make sure you set the room using \"!room &[room]\"\n"
                           + "\"!status gives the current state of the bot.\"\n#####\n");
    }
    
    public void announce() {
        try {
            getRoomConnection(room).sendPacket(new Send(announcement).createPacket(), new ReplyEventListener() {
                @Override
                public void onReplySuccess(PacketEvent evt) {
                    map.put(((SendReply)evt.getPacket().getData()).getId(), 0);
                    System.out.println("Announced in "+room+".");
                }
                @Override
                public void onReplyFail(PacketEvent evt) {
                    System.out.println("Message error - could not announce in "+room+".\nEuphoria returned error "+evt.getPacket().getError()+".");
                }
                public void onReplyEvent(PacketEvent evt) {}
            });
        } catch (RoomNotConnectedException e) {
            System.out.println("Connection error - not connected to "+room+".");
        }
    }

    public static void main(String[] args) {
        new AnnouncerBot();
    }
}
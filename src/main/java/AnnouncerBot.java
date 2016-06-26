import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.Bot;
import euphoria.FileIO;
import euphoria.events.ConnectMessageEventListener;
import euphoria.events.ConsoleEventListener;
import euphoria.events.MessageEvent;
import euphoria.events.MessageEventListener;
import euphoria.events.PacketEventListener;
import euphoria.events.StandardEventListener;
import euphoria.packets.events.SendEvent;


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

                        if (map.size() >= msgLimit) {
                            evt.getRoomConnection().sendMessage(announcement);
                            map.clear();
                        }
                    } else {
                        if (map.containsKey(parentId)) {
                            if (map.get(parentId) < 5) {
                                map.put(evt.getId(), 0);
                                map.replace(parentId, map.get(parentId) + 1);
                                if (map.size() >= msgLimit) {
                                    evt.getRoomConnection().sendMessage(announcement);
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
                           + "Make sure you set the room using \"!room &[room]\"\n#####\n");
    }

    public static void main(String[] args) {
        new AnnouncerBot();
    }
}
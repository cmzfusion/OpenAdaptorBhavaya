package org.bhavaya.util;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Allows clients to support Terminal backdoors for support purposes.
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class TerminalService {
    private static final Log log = Log.getCategory(TerminalService.class);

    public static abstract class Action {
        private String name;

        public Action(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        };

        public abstract String performAction();
    }

    private String mainMenu;
    private Action[] mainActions;

    private ServerSocket serverSocket;
    private Thread serverThread;

    public TerminalService(String mainMenu, Action[] mainActions, int portNumber) throws IOException {
        this.mainMenu = mainMenu;
        this.mainActions = mainActions;

        this.serverSocket = new ServerSocket(portNumber);
        this.serverThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        startTerminalSession(serverSocket.accept());
                    }
                } catch (IOException e) {
                    log.error("Error waiting for socket connection", e);
                }
            }
        }, "TerminalService");
        this.serverThread.setDaemon(true);
        this.serverThread.start();

        log.info("Terminal Service open for business on localhost:" + portNumber);
    }

    private void startTerminalSession(final Socket clientSocket) {
        log.info("Starting Terminal Service session with " + clientSocket.getInetAddress());
        Thread clientThread = new Thread(new Runnable() {
            public void run() {
                try {
                    BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "ISO-8859-1"));
                    PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));

                    final MutableBoolean exit = new MutableBoolean(false);
                    Action exitAction = new Action("Exit") {
                        public String performAction() {
                            exit.value = true;
                            return "Exiting...";
                        }
                    };
                    final Action[] mergedActions = Utilities.unionArrays(mainActions, new Action[]{exitAction});

                    while (!exit.value) {
                        output.println("\r\n" + mainMenu);
                        output.println(Utilities.pad("", mainMenu.length(), '-'));
                        for (int i = 1; i <= mergedActions.length; i++) {
                            Action action = mergedActions[i - 1];
                            output.println("" + i + ". " + action.getName());
                        }
                        output.print("\n$: ");
                        output.flush();

                        String inputString = echoReadLine(input, output);
                        try {
                            int selectedOption = Integer.parseInt(inputString);
                            if (selectedOption < 1 || selectedOption > mergedActions.length) throw new RuntimeException("Invalid option.");
                            Action action = mergedActions[selectedOption - 1];
                            output.print("\n>> " + action.performAction() + "\n");
                        } catch (Exception e) {
                            output.print("\n>> Error: " + e.getMessage() + "\n");
                        } finally {
                            output.flush();
                        }
                    }
                } catch (IOException e) {
                    log.error("Error during client session with client " + clientSocket.getInetAddress(), e);
                } finally {
                    log.info("Client disconnecting: " + clientSocket.getInetAddress());
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                    }
                }
            }
        }, "TerminalSession:" + clientSocket.getInetAddress());
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private String echoReadLine(BufferedReader input, PrintWriter output) throws IOException {
        StringBuffer returnBuffer = new StringBuffer();
        while (true) {
            int ch = input.read();
            output.print((char) ch);

            // I hate windows
            if (ch == '\r') continue;

            // Newline
            if (ch == '\n' || ch == -1) return returnBuffer.toString();

            // Backspace
            if (ch == 8 && returnBuffer.length() > 0) {
                returnBuffer.setLength(returnBuffer.length() - 1);
            } else {
                returnBuffer.append((char) ch);
            }

            output.flush();
        }
    }
}

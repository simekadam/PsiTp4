/*
 * Klient pro UDP protokol PsiTp4
 *
 * AUTORI: Jan Kolarik, Adam Simek, Tomas Velechovsky
 *
 * Pouzite zdroje informaci:
 * - http://javaprojects4u.blogspot.com/2008/03/sliding-window-protocol-major.html
 * - http://en.wikipedia.org/wiki/Sliding_window_protocol
 * - http://www.genmay.com/showthread.php?t=777164
 * - http://developerweb.net/?f=70
 * - http://en.wikipedia.org/wiki/Berkeley_sockets
 * - http://www2.rad.com/networks/2004/sliding_window/demo.html
 * - Projekty UDP do predmetu Y36PJV z minuleho semestru
 *
 */
package psitp4;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *
 * @author Opaquit
 */
public class Main {
    // Staticka promenna se sitovym pripojenim, pouziva se napric celym programem:

    public static NetworkConnection networkConnection = new NetworkConnection();
    // Konstanty s vychozimi hodnotami vstupnich parametru:
    public static final String DEFAULT_REMOTE_FILE = "a.txt";
    public static final String DEFAULT_LOCAL_FILE = "stazenySoubor";
    public static final String DEFAULT_IP = "dsnlab1.felk.cvut.cz";
    public static final int DEFAULT_PORT = 3556;
    // Urci, zda ma probihat prenos, ci ne:
    boolean isRunning = true;
    // Inicializace adresy a UDP socketu:
    InetAddress inetAddress;
    DatagramSocket udpSocket;
    // Promenne pro cely program, nastavuji se pri spusteni bud z parametru prikazove radky, nebo dotazu v konzoli:
    public static String whatToDownload;
    public static String whereToStore;
    public static String connectToIP;
    public static int connectToPort;
    // Kontrolni promenne:
    public static String newestPacketContents;
    public static int numberOfPacketsOut = 0;

    // ----------------------------------------------
    // ZACATEK HLAVNI PROCEDURY, ZDE PROGRAM STARTUJE
    // ----------------------------------------------
    public static void main(String[] args) {

        // "Vytisknuti" uvodniho "splash screenu" do konzole:
        System.out.println("=======================================================");
        System.out.println("========= welcome to ==================================");
        System.out.println("========= SUPER DUPER ENHANCED PSITP4 CLIENT ==========");
        System.out.println("=======================================================");
        System.out.println("=======================================================");

        if (args.length < 4) {
            // Kdyz nebyl zadan dostatecny pocet parametru prikazove radky:

            // IP adresa serveru:
            System.out.println("Zadejte prosim adresu serveru:");
            Scanner sc = new Scanner(System.in);
            connectToIP = sc.next();

            // Cislo portu:
            System.out.println("Zadejte prosim cislo portu:");
            connectToPort = sc.nextInt();

            // Nazev stahovaneho souboru:
            System.out.println("Zadejte prosim nazev souboru, ktery si prejete stahnout:");
            whatToDownload = sc.next();

            // Nazev, pod kterym se ma ulozit stahovany soubor:
            System.out.println("Zadejte prosim nazev, pod kterym se ma stazeny soubor ulozit:");
            whereToStore = sc.next();

        } else {
            // Kdyz byl progam spusten se spravnym poctem parametru:
            connectToIP = args[0];
            connectToPort = Integer.parseInt(args[1]);
            whatToDownload = args[2];
            whereToStore = args[3];
        }

        // Alespon castecna kontrola spravnosti vstupnich udaju:
        if (connectToIP.length() < 7) {
            connectToIP = DEFAULT_IP;
        }
        if ((connectToPort < 1) || (connectToPort > 65535)) {
            connectToPort = DEFAULT_PORT;
        }
        if (whatToDownload.length() < 1) {
            whatToDownload = DEFAULT_REMOTE_FILE;
        }
        if (whereToStore.length() < 1) {
            whereToStore = DEFAULT_LOCAL_FILE;
        }

        // Zavolani konstruktoru hlavni tridy celeho programu:
        new Main();

    }

    // -------------------
    // METODY ZACINAJI ZDE
    // -------------------
    UdpPacket getPacket(UdpPacket input) {
        // Ziska prvni nebo druhy paket
        if (input == null) {
            // Prvni paket:
            UdpPacket packet1 = new UdpPacket(0);
            packet1.flag = FlagType.FLAG_SYN;
            packet1.intSeq = 42;
            return packet1;
        } else {
            // Druhy paket:
            UdpPacket packet2 = new UdpPacket(networkConnection);
            // Nazev vzdaleneho souboru:
            packet2.packetContents = "GET" + whatToDownload;
            packet2.intSeq = input.intAck;
            packet2.intAck = input.intSeq + 1;
            return packet2;
        }
    }

    UdpPacket serverReply(FlagType input) {
        // Vygenerovani paketu podle prijateho:
        UdpPacket packetToSend = new UdpPacket(networkConnection.identifier);
        packetToSend.intSeq = networkConnection.intSeqFromClient;
        packetToSend.flag = input;
        return packetToSend;
    }

    Main() {
        // Zahajeni komunikace:
        try {
            inetAddress = InetAddress.getByName(connectToIP);
            udpSocket = new DatagramSocket();

            UdpPacket workingPacket;

            // Ziska prvni paket:
            workingPacket = getPacket(null);

            // Vytvoreni spojeni:
            workingPacket = networkRequest(workingPacket);


            if (packetInspectionIsOK(workingPacket)) {
                // Prvni paket je v poradku:
                System.out.println("OK");
            } else {
                // Prvni paket je spatny:
                System.out.println("Chyba.");
                return;
            }

            // Zjisteni identifikatoru prvniho paketu
            networkConnection.identifier = workingPacket.identifier;

            // Zjisteni druheho paketu:
            workingPacket = getPacket(workingPacket);

            // Hlavni smycka cele komunikace:
            while (true) {
                workingPacket = networkRequest(workingPacket);

                if (receivedPacketIsOK(workingPacket)) {
                    // OK
                    System.out.println("Prijat paket.");
                } else {
                    // Ukonceni cyklu, prijaty paket neni v poradku:
                    break;
                }

                // Mohou nastat ctyri ruzne moznosti:
                if (workingPacket.isEmpty()) {
                    if (workingPacket.flag.equals(FlagType.FLAG_FIN)) {
                        // Byl prijat flag FIN:
                        UdpPacket finish = new UdpPacket(networkConnection);

                        finish.intSeq = workingPacket.intAck - 1;
                        finish.intAck = workingPacket.intSeq + 1;
                        finish.flag = FlagType.FLAG_FIN;
                        isRunning = false;
                        networkRequest(finish);

                        // Vypsani zpravy o uspesnem stazeni a nasledne ukonceni smycky:
                        System.out.println("=======================================================");
                        System.out.println("=======================================================");
                        System.out.println("Stahovani souboru " + whatToDownload + " bylo uspesne dokonceno.");
                        break;

                    } else {
                        // Byl prijat potvrzujici paket:
                        UdpPacket prompt = new UdpPacket(networkConnection);

                        prompt.intAck = workingPacket.intSeq;
                        prompt.intSeq = workingPacket.intAck;

                        prompt.flag = FlagType.FLAG_FIN;
                        workingPacket = prompt;
                    }
                } else {
                    // Prijaty paket obsahuje data:
                    if (workingPacket.intSeq == networkConnection.requiredIntSeq & (workingPacket.intAck == networkConnection.requiredIntAck | workingPacket.intAck == networkConnection.requiredIntAck + 1)) {//je to aktualni paket
                        // Overeni:
                        UdpPacket verification = new UdpPacket(networkConnection);
                        verification.intSeq = workingPacket.intAck;
                        verification.intAck = networkConnection.requiredData(workingPacket.intSeq, workingPacket.packetContents.getBytes());

                        networkConnection.requiredIntSeq = verification.intAck;
                        networkConnection.requiredIntAck = verification.intSeq;

                        workingPacket = verification;

                        // Dalsi prubeh cyklu:
                        continue;

                    } else { // Narazilo se na predcasny paket
                        networkConnection.requiredData(workingPacket.intSeq, workingPacket.packetContents.getBytes());
                        UdpPacket reminder = new UdpPacket(networkConnection);
                        reminder.intSeq = networkConnection.requiredIntAck;
                        reminder.intAck = networkConnection.requiredIntSeq;
                        reminder.flag = FlagType.FLAG_FIN;
                        workingPacket = reminder;
                    }
                }

            }

            // Vypsani dalsich udaju o uspechu stazeni:
            System.out.println("Velikost stazeneho souboru: " + networkConnection.readStoredData().length() + " B");
            System.out.println("Soubor byl ulozen jako " + whereToStore + ".");

            if (whereToStore != null) {
                // Ulozeni stazenych dat lokalne do souboru:

                // Vytvoreni file writeru a print writeru:
                FileWriter fileWriter = new FileWriter(whereToStore);
                PrintWriter printWriter = new PrintWriter(fileWriter);

                // Ulozeni samotnych dat:
                printWriter.print(networkConnection.readStoredData());

                // Zavreni souboru:
                printWriter.close();
                fileWriter.close();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Chyba.");
        }
    }

    UdpPacket networkRequest(UdpPacket outgoingPacket) {
        try {
            // Procedura odeslani zadaneho paketu na vzdaleny server

            byte[] packetToSend = outgoingPacket.generateByteStream();
            // Vytvori UDP paket se spravnou IP a portem
            DatagramPacket incoming = new DatagramPacket(packetToSend, packetToSend.length, inetAddress, connectToPort);
            DatagramPacket incoming2 = new DatagramPacket(new byte[264], 264);

            networkConnection.requiredIntSeq = outgoingPacket.intAck;

            if (outgoingPacket.flag.equals(FlagType.FLAG_NULL)) {
                networkConnection.requiredIntAck = outgoingPacket.intSeq + outgoingPacket.getContentLength();
            } else {
                {
                    if ((outgoingPacket.flag.equals(FlagType.FLAG_FIN)) == false) {
                        // Priznak neni NULL ani FIN, SEQ se musi zvysit o 1:
                        networkConnection.requiredIntAck = outgoingPacket.intSeq + 1;
                    }
                }
            }

            // Pokud server neodpovida 5 sekund, povazuje se za neaktivni
            udpSocket.setSoTimeout(5000);

            // Zda server odpovidal, nebo ne:
            int gotNoReply = 0;

            for (int i = 0; i <= 9; i++) {
                System.out.println("ODESILA SE - " + outgoingPacket.readableInfo());
                // Odeslani samotneho paketu:
                udpSocket.send(incoming);


                if (gotNoReply > 0) {
                    System.out.println("CHYBA: Nelze se pripojit k serveru");
                    // Predani priznaku, aby program uz dale nebezel
                    isRunning = false;
                }

                // Zjisteni, zda vse skoncilo:
                if (!(isRunning)) {
                    // Pokud je nastaven priznak tak, aby ukoncil program, zde se ukonci jeho hlavni cyklus:
                    break;
                }

                try {

                    udpSocket.receive(incoming2);

                    // Prijme chtena prichozi data:
                    byte[] incomingData = new byte[incoming2.getLength()];
                    System.arraycopy(incoming2.getData(), 0, incomingData, 0, incoming2.getLength());
                    UdpPacket incomingPacket = new UdpPacket(incomingData);

                    // Vypis o prijmutych datech:
                    System.out.println("PRIJIMA SE - " + incomingPacket.readableInfo());

                    // Jako navratova hodnota je prijaty paket, kdyz neni prazdny:
                    return incomingPacket;

                } catch (Exception ex) {
                    // Predevsim kdyz vyprsel casovy limit spojeni:
                    System.out.println(ex.getMessage());
                    System.out.println("Chyba.");
                }
            }
            // V pripade potreby vrati prazdny objekt:
            return null;

        } catch (Exception e) {
            // Osetreni vyjimek, predevsim IO vyjimek:
            System.out.println(e.getMessage());
            System.out.println("Chyba.");
            return null;
        }
    }

    boolean packetInspectionIsOK(UdpPacket packet) {
        try {
            if (packet.flag.equals(FlagType.FLAG_SYN)) {
                // Paket je v poradku:
                System.out.println("Inspekce paketu: OK");
                return true;
            } else {
                // Vynuti "restart":
                networkRequest(serverReply(FlagType.FLAG_RST));
                System.out.println("Inspekce paketu: CHYBA");
                return false;
            }
        } catch (Exception e) {
            // Doslo k IO vyjimce:
            System.out.println(e.getMessage());
            System.out.println("Chyba.");
            return false;
        }
    }

    boolean receivedPacketIsOK(UdpPacket input) {
        try {

            // Zjisti spravnost prijateho paketu a zda se ma ukoncit komunikace:
            if (input == null) {
                // Rovnou se ukoncuje:
                return false;
            }

            if (input.flag.equals(FlagType.FLAG_SYN)) {
                // Vynuti se "reset":
                networkRequest(serverReply(FlagType.FLAG_RST));
                return false;
            }

            if (input.flag.equals(FlagType.FLAG_RST)) {
                // Od serveru byl prijat flag RST:
                return false;
            }

            if (!(input.identifier == networkConnection.identifier)) {
                // Kdyz se neshoduji identifikatory paketu, vynuti se "reset":
                networkRequest(serverReply(FlagType.FLAG_RST));
                System.out.println("Nesouhlasi identifikatory paketu, ukoncuje se komunikace.");
                return false;
            }

            // Komunikace se neukonci:
            return true;

        } catch (Exception e) {
            // Doslo k vyjimce:
            System.out.println("Chyba pri komunikaci.");
            return false;
        }
    }
}

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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tomas
 */
public class NetworkConnection {
    // Cela komunikace po siti a prace s prijatymi daty:

    // Inicializace statickych promennych:
    public static int identifier;
    public static int intSeqFromClient;
    public static int requiredIntSeq = 0;
    public static int requiredIntAck = 0;
    // Mapa se stazenymi daty:
    private static Map<Integer, byte[]> allData = new HashMap<Integer, byte[]>();
    private static byte[] storedData = new byte[0];

    // Vraci data v uzivatelsky citelne podobe:
    public static String readStoredData() {
        return new String(storedData);
    }

    public static int requiredData(int inputSeq, byte[] inputData) {
        // Vraci pozadovana data ze serveru
        dataFromServer(inputSeq, inputData);

        int id;
        for (int z = 0; z < allData.size(); z++) {
            id = (int) allData.keySet().toArray(new Integer[0])[z];
            if (id <= requiredIntSeq && id > (requiredIntSeq - 2048)) { //nasli jsme usek, ktery nas muze obohatit, je v intervalu seqWanted - velikost okna do seqWanted


                int fragment = allData.get(id).length - (requiredIntSeq - id);
                if (fragment < 1) {
                    // Odstraneni ziskaneho fragmentu:
                    allData.remove(id);
                    continue;
                } else {
                    byte[] overflowingFragment = new byte[fragment]; //ziskat kus useku, ktery je novy
                    System.arraycopy(allData.remove(id), (requiredIntSeq - id), overflowingFragment, 0, overflowingFragment.length);
                    storedData = Conversions.mergeTwoByteArrays(storedData, overflowingFragment);
                    requiredIntSeq = requiredIntSeq + overflowingFragment.length;
                    // Projiti vseho zase od zacatku:
                    z = 0;
                }
            }
        }

        if (requiredIntSeq > 65535) {
            requiredIntSeq = (requiredIntSeq - 65536);
        }
        return requiredIntSeq;
    }

    public static void dataFromServer(int intSeq, byte[] contents) {
        System.out.println("Prijata nova data.");
        allData.put(intSeq, contents);
    }
}

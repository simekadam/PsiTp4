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

/**
 *
 * @author Opaquit
 */
public enum FlagType {

    // Definuje datovy typ pro priznaky/flags:
    // Nulovy priznak:
    FLAG_NULL((byte) 0x00),
    FLAG_SYN((byte) 0x01),
    FLAG_RST((byte) 0x04),
    FLAG_FIN((byte) 0x02);
    public byte flagContents;

    // Konstruktor s pocatecnim obsahem:
    FlagType(byte initialContents) {
        flagContents = initialContents;
    }
}

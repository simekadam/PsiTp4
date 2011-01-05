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
// Datovy typ samotneho paketu:
public class UdpPacket {
    // Zakladni promenne paketu:

    public int identifier;
    public FlagType flag = FlagType.FLAG_NULL;
    public int intAck;
    public int intSeq;
    // Datovy obsah paketu:
    public String packetContents;

    public boolean isEmpty() {
        // Zjisti, zda je soucasny paket prazdny:
        boolean temp;
        if (packetContents == null || packetContents.getBytes() == null) {
            temp = false;
        } else {
            temp = (packetContents.getBytes().length > 0);
        }
        return (!(temp));
    }

    public int getContentLength() {
        // Zjisteni delky obsahu paketu v bajtech:
        if (isEmpty()) {
            return 0;
        }

        int length = packetContents.getBytes().length;
        return length;
    }

    UdpPacket(NetworkConnection inputNetworkConnectionLink) {
        // Prvni varianta pretizeneho konstruktoru, nezname parametr identifikatoru, zname pouze sitove pripojeni, ze ktereho se pokusime zjistit jeho identifikator:
        this.identifier = inputNetworkConnectionLink.identifier;
    }

    UdpPacket(int identifier) {
        // Druha varianta pretizeneho konstruktoru, mame k disozici identifikator jako parametr:
        this.identifier = identifier;
    }

    UdpPacket(byte[] inputBytes) {
        // Treti varianta pretizeneho konstruktoru, zname vstupni data paketu jako pole bajtu:
        int[] input = new int[inputBytes.length];

        for (int i = 0; i < inputBytes.length; i++) {
            input[i] = Conversions.convertByteToInt(inputBytes[i]);
        }

        this.intSeq = input[4] * 256 + input[5];
        this.intAck = input[6] * 256 + input[7];

        identifier = (input[0]) + (input[1] * 256) + +(input[2] * 65536) + (input[3] * 16777216);

        // Urceni prislusneho flagu:
        switch (input[8]) {
            case 4:
                this.flag = FlagType.FLAG_RST;
                break;
            case 2:
                this.flag = FlagType.FLAG_FIN;
                break;
            case 1:
                this.flag = FlagType.FLAG_SYN;
        }

        // Generovani vystupu:
        byte[] output = new byte[inputBytes.length - 9];
        System.arraycopy(inputBytes, 9, output, 0, inputBytes.length - 9);

        // Vrati z pole bajtu retezec:
        packetContents = new String(output);

    }

    public String readableInfo() {
        // Vrati informace o paketu v textove podobe:
        if (packetContents == null) {
            return "Identifikator: " + this.identifier + "; Priznaky: " + flag.flagContents + "; Velikost dat: 0 B";
        } else {
            return "Identifikator: " + this.identifier + "; Priznaky: " + flag.flagContents + "; Velikost dat: " + packetContents.length() + " B";
        }
    }

    public byte[] generateByteStream() {

        // Vygeneruje pole bajtu, ktere odesleme na server:
        byte[] bytesFromInteger = Conversions.convertIntToByte(identifier, 4);

        byte[] output = new byte[4];

        output[3] = bytesFromInteger[0];
        output[2] = bytesFromInteger[1];
        output[1] = bytesFromInteger[2];
        output[0] = bytesFromInteger[3];

        output = Conversions.mergeTwoByteArrays(output, Conversions.convertIntToByte(this.intSeq, 2));
        output = Conversions.mergeTwoByteArrays(output, Conversions.convertIntToByte(this.intAck, 2));
        output = Conversions.mergeTwoByteArrays(output, Conversions.convertIntToByte(this.flag.flagContents * 1, 1));

        if (!(packetContents == null)) {
            output = Conversions.mergeTwoByteArrays(output, this.packetContents.getBytes());
        }

        return output;
    }
}

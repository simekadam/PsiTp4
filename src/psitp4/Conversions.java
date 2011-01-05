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
 * @author simekadam
 */
public class Conversions {

    // Staticke metody vyuzivane v celem programu, pouzivaji se pro konverzi ruznych datovych typu.
    public static byte[] mergeTwoByteArrays(byte[] firstArray, byte[] secondArray) {
        // Slouci dve pole elementu datoveho typu byte:

        int secondArrayLen = secondArray.length;
        int firstArrayLen = firstArray.length;

        byte[] tempReturn = new byte[(firstArrayLen + secondArrayLen)];

        System.arraycopy(firstArray, 0, tempReturn, 0, firstArrayLen);
        System.arraycopy(secondArray, 0, tempReturn, firstArrayLen, secondArrayLen);

        return tempReturn;
    }

    public static int convertByteToInt(byte input) {
        // Prevede bajt na cislo typu integer:
        int tempOutput;
        if (input > (-1)) {
            tempOutput = (int) input;
            return tempOutput;
        } else {
            tempOutput = (input + 256);
            return tempOutput;
        }

    }

    public static byte[] convertIntToByte(int number, int numberOfBytes) {
        // Prevede hodnotu promenne typu int na bajt podle poctu pozadovanych bajtu:

        switch (numberOfBytes) {
            case 2:
                return new byte[]{(byte) (number >>> 8), (byte) number};
            case 3:
                return new byte[]{(byte) (number >>> 16), (byte) (number >>> 8), (byte) number};
            case 4:
                return new byte[]{(byte) (number >>> 24), (byte) (number >>> 16), (byte) (number >>> 8), (byte) number};
            default:
                return new byte[]{(byte) number};
        }
    }
}

package correcter;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

enum Mode {
    ENCODE, SEND, DECODE;
}

class InterferenceGenerator {

    private final static char BEGIN_RANGE = 32;
    private final static char END_RANGE = 59;
    public final static int INTERFERENCE_PERIOD = 3;


    //Generates a random noise character
    private char getNoise() {
        Random random = new Random();
        return (char) (random.nextInt(END_RANGE) + BEGIN_RANGE);
    }

    /**
     * Introduces noise into the signal on the specified period
     *
     * @param signal to be interfered
     * @return interfered signal
     */
    public String interfere(String signal) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (char currentChar : signal.toCharArray()) {
            if (i == INTERFERENCE_PERIOD) {
                char noise = getNoise();
                while (noise == currentChar) {
                    noise = getNoise();
                }
                currentChar = noise;
                i = 0;
            }

            sb.append(currentChar);
            i++;
        }

        return sb.toString();
    }

    public static byte[] interfere(byte[] signal) {
        for (int i = 0; i < signal.length; i++) {
            signal[i] ^= 1 << INTERFERENCE_PERIOD;
        }
        return signal;
    }
}

interface Coder {

    byte EIGHTH_BIT_MASK = (byte) 0x80; //128 0xFF
    byte SEVENTH_BIT_MASK = 64;
    byte SIXTH_BIT_MASK = 32;
    byte FIFTH_BIT_MASK = 16;
    byte FOURTH_BIT_MASK = 8;
    byte THIRD_BIT_MASK = 4;
    byte SECOND_BIT_MASK = 2;
    byte FIRST_BIT_MASK = 1;

    byte FOURTH_DUPLE_MASK = (byte) 0xB0;
    byte THIRD_DUPLE_MASK = 48;
    byte SECOND_DUPLE_MASK = 12;
    byte FIRST_DUPLE_MASK = 3;

    int BYTE_PARITY_AMOUNT = 3;
    int BYTE_BITS_AMOUNT = 8;

    default byte getEighthBit(byte bite) {
        return (byte) (bite & EIGHTH_BIT_MASK);
    }

    default byte getSeventhBit(byte bite) {
        return (byte) (bite & SEVENTH_BIT_MASK);
    }

    default byte getSixthBit(byte bite) {
        return (byte) (bite & SIXTH_BIT_MASK);
    }

    default byte getFifthBit(byte bite) {
        return (byte) (bite & FIFTH_BIT_MASK);
    }

    default byte getFourthBit(byte bite) {
        return (byte) (bite & FOURTH_BIT_MASK);
    }

    default byte getThirdBit(byte bite) {
        return (byte) (bite & THIRD_BIT_MASK);
    }

    default byte getSecondBit(byte bite) {
        return (byte) (bite & SECOND_BIT_MASK);
    }

    default byte getFirstBit(byte bite) {
        return (byte) (bite & FIRST_BIT_MASK);
    }

    default byte getFourthDupleBits(byte bite) {
        return (byte) (bite & FOURTH_DUPLE_MASK);
    }

    default byte getThirdDupleBits(byte bite) {
        return (byte) (bite & THIRD_DUPLE_MASK);
    }

    default byte getSecondDupleBits(byte bite) {
        return (byte) (bite & SECOND_DUPLE_MASK);
    }

    default byte getFirstDupleBits(byte bite) {
        return (byte) (bite & FIRST_DUPLE_MASK);
    }


    /**
     * Gets the bit from the requested position
     *
     * @param bite        byte
     * @param bitPosition bit index
     * @return 00X0 0000 where X is the bit from the bitPosition from the byte
     */
    static byte getBit(byte bite, int bitPosition) {
        return (byte) (bite & (1 << bitPosition));
    }

    static byte getParity(byte bite) {
        byte bit1 = (byte) (Coder.getBit(bite, 6) >> 6);
        byte bit2 = (byte) (Coder.getBit(bite, 4) >> 4);
        byte bit3 = (byte) (Coder.getBit(bite, 2) >> 2);

        byte parity = (byte) (bit1 ^ bit2 ^ bit3);
        return (byte) (parity | (parity << 1));
    }

    static byte shiftBit(byte bit, int originIndex, int destinationIndex) {
        byte result = 0;
        int shifts = destinationIndex - originIndex;

        if (shifts > 0) {
            result = (byte) (bit << shifts);
        } else {
            result = (byte) (bit >> Math.abs(shifts));
        }

        return result;
    }

    static byte getBitDuple(byte bit) {
        return (byte) (bit | (bit << 1));
    }

    /**
     * returns the first nibble of the bite in this format 0000 XXXX
     *
     * @param bite
     * @return
     */
    public static byte getFirstNibble(byte bite) {
        return (byte) (bite >>> HammingCoder.SIGNIFICANT_BITS);
    }

    /**
     * returns the second nibble of the bite in this format 0000 XXXX
     *
     * @param bite
     * @return
     */
    public static byte getSecondNibble(byte bite) {
        return (byte) (bite & HammingCoder.SECOND_NIBBLE);
    }

}

class Decoder implements Coder {

    /**
     * Evaluates by repetition which character is the right one, The most prevalent is the valid data
     */
    public static String decode(String interferencedSignal) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < interferencedSignal.length(); i = i + InterferenceGenerator.INTERFERENCE_PERIOD) {

            int[] subSet = new int[InterferenceGenerator.INTERFERENCE_PERIOD];
            Arrays.fill(subSet, 0);

            int bound = InterferenceGenerator.INTERFERENCE_PERIOD + i;

            for (int j = i; j < bound; j++) {
                for (int k = i; k < bound; k++) {
                    if (interferencedSignal.charAt(j) == interferencedSignal.charAt(k)) {
                        subSet[j - i]++;
                    }
                }
            }
            int prevalentIndex = 0;

            for (int j = 0; j < subSet.length; j++) {
                if (subSet[prevalentIndex] <= subSet[j]) {
                    prevalentIndex = j;
                }
            }

            sb.append(interferencedSignal.charAt(i + prevalentIndex));
        }
        return sb.toString();
    }

    public static byte[] decode(byte[] encodedSignal) {
        int pairedBitsAmount = encodedSignal.length * 2;
        int totalBitsAmount = encodedSignal.length * Coder.BYTE_BITS_AMOUNT;

        int decodedBitsAmount = (totalBitsAmount - pairedBitsAmount) / 2;
        int decodedSignalLength = decodedBitsAmount / Coder.BYTE_BITS_AMOUNT;
        byte[] decodedSignal = new byte[decodedSignalLength];

        int bitIndex = 0;
        int decodedIndex = 0;
        byte rawByte = 0;
        byte fixedByte = 0;

        for (int i = 0; i < encodedSignal.length; i++) {

            bitIndex = bitIndex + Coder.BYTE_PARITY_AMOUNT;

            int shifts = Coder.BYTE_BITS_AMOUNT - bitIndex;

            fixedByte = Decoder.bitsDecoder(encodedSignal[i]);


            if (shifts >= 0) {
                rawByte |= (fixedByte << shifts);

            } else {
                rawByte |= (byte) (fixedByte >>> Math.abs(shifts));

                decodedSignal[decodedIndex] = rawByte;

                rawByte = (byte) (fixedByte << (Coder.BYTE_BITS_AMOUNT + shifts));

                decodedIndex++;
                bitIndex = Math.abs(shifts);
            }
        }

        if (decodedIndex < decodedSignalLength) {
            decodedSignal[decodedIndex] = rawByte;
        }

        return decodedSignal;
    }

    /**
     * Returns the byte fixed without the parity.
     *
     * @param bite
     * @return returns the byte with the fixed bit in this format: 0000 0XXX
     */

    private static byte bitsDecoder(byte bite) {
        byte decodedByte = 0;
        byte partialXOR = 0;
        int dupleErrorIndex = 0;
        int decodedBitsShifts = 0;

        for (int i = Coder.BYTE_BITS_AMOUNT - 1; i > 2; i = i - 2) {

            byte firstBit = Coder.getBit(bite, i);
            byte secondBit = Coder.getBit(bite, i - 1);
            // Here it is coming -1 for the -128 number, thus bringing all ones at the beginning instead of zeros
            firstBit = Coder.getBit((byte) (firstBit >>> i), 0);
            secondBit = (byte) (secondBit >>> (i - 1));

            if (firstBit != secondBit) {
                dupleErrorIndex = ((i - 1) / 2);
            } else {
                partialXOR ^= firstBit;
                decodedByte |= (byte) (firstBit << (2 - decodedBitsShifts));
            }

            decodedBitsShifts++;
        }

        if (dupleErrorIndex != 0) {
            decodedByte |= (getFixedBit(partialXOR, Coder.getBit(bite, 0)) << (dupleErrorIndex - 1));
        }

        return decodedByte;
    }

    protected static byte getFixedBit(byte partialXor, byte parity) {
        byte fixedByte = 0;

        if (partialXor == 1) {
            fixedByte = (byte) (parity == 0 ? 1 : 0);
        } else {
            fixedByte = parity;
        }

        return fixedByte;
    }
}

interface HammingCoder extends Coder {
    int SIGNIFICANT_BITS = 4;
    int[] PARITY_INDEXES = {7, 6, 4};
    int[] SIGNIFICANT_INDEXES = {5, 3, 2, 1};
    int ENCODED_BITS_LENGTH = 7;
    byte FIRST_NIBBLE = (byte) 0xF0;
    byte SECOND_NIBBLE = 0x0F;

    static byte getParity(byte bite, int parityIndex, int jump) {

        byte parity = 0;
        boolean check = true;
        int parityPosition = BYTE_BITS_AMOUNT - 1 - parityIndex;//FIXME I had added the jump

        //Minus one because the index 7 is dismissed
        for (int j = parityPosition; j < BYTE_BITS_AMOUNT - 1; j = j + jump) {

            if (check) {
                for (int k = 0; k < jump; k++) {
                    int bytePosition = BYTE_BITS_AMOUNT - 1 - j - k;
                    int parityBytePosition = BYTE_BITS_AMOUNT - 1 - parityPosition;
                   // parity ^= ((byte) (Coder.getBit(bite, bytePosition)) >>> bytePosition);//FIXME check
                    if (bytePosition != parityBytePosition) {
                        parity ^= ((Coder.getBit(bite, bytePosition) & 0xFF) >>> bytePosition);
                    }
                }
            }

            check = !check;
        }

        return parity;
    }
}


abstract class Encoder implements Coder {

    /**
     * Encodes the input signal multiplying each data for the interference period
     *
     * @param signal
     * @return encoded signal
     */
    public static String encode(String signal) {

        StringBuilder sb = new StringBuilder();

        for (char currentChar : signal.toCharArray()) {
            for (int i = 0; i < InterferenceGenerator.INTERFERENCE_PERIOD; i++) {
                sb.append(currentChar);
            }
        }

        return sb.toString();
    }

    public static byte[] encode(byte[] signal) {
        // get length without remainder
        int bitsLength = signal.length * Coder.BYTE_BITS_AMOUNT;
        int integerTripleBitsLength = (int) Math.ceil((double) bitsLength / Coder.BYTE_PARITY_AMOUNT);

        int parityBitsAmount = integerTripleBitsLength;
        int resultLength = (int) Math.ceil((double) ((bitsLength + parityBitsAmount) * 2) / Coder.BYTE_BITS_AMOUNT);
        byte[] encodedSignal = new byte[resultLength];

        int encodedSignalIndex = 0;
        byte encodedByte = 0;
        int tripleCount = 3;

        for (int i = 0; i < signal.length; i++) {
            for (int j = 0; j < Coder.BYTE_BITS_AMOUNT; j++) {
                int bitIndex = Coder.BYTE_BITS_AMOUNT - 1 - j;
                int destinationDupleIndex = tripleCount * 2;
                byte bit = Coder.getBit(signal[i], bitIndex);

                bit = Coder.shiftBit(bit, bitIndex, destinationDupleIndex);
                bit = Coder.getBitDuple(bit);
                encodedByte |= bit;

                if (tripleCount == 1) {

                    encodedSignal[encodedSignalIndex] = (byte) (encodedByte | Coder.getParity(encodedByte));

                    encodedSignalIndex++;
                    encodedByte = 0;
                    tripleCount = 4;
                }

                tripleCount--;
            }
        }

        // Creates the new bytes for the complete byte sequence
        if (tripleCount != 3) {
            encodedSignal[encodedSignalIndex] = (byte) (encodedByte | Coder.getParity(encodedByte));//Coder.getParity(encodedByte);
        }

        return encodedSignal;
    }


}

class HammingEncoder extends Encoder implements HammingCoder {

    public static byte[] encode(byte[] signal) {
        int encodedSignalLength = signal.length * 2;
        byte[] encodedSignal = new byte[encodedSignalLength];


        for (int i = 0; i < signal.length; i++) {
            byte currentNibble = Coder.getFirstNibble(signal[i]);
            int index = i * 2;
            encodedSignal[index] = HammingEncoder.encodeNibble(currentNibble);

            currentNibble = Coder.getSecondNibble(signal[i]);
            encodedSignal[index + 1] = HammingEncoder.encodeNibble(currentNibble);
        }

        return encodedSignal;
    }

    /**
     * Encodes the provided nibble
     *
     * @param nibble this is the bibble format 0000 XXXX
     * @return returns the nibble encoded
     */
    private static byte encodeNibble(byte nibble) {
        byte result;

        result = distributeSignificantDigits(nibble);
        result = distributeParity(result);

        return result;
    }

    /**
     * distribute the significant bits into
     *
     * @param bite receives the bite in this format 0000 XXXX
     * @return
     */
    private static byte distributeSignificantDigits(byte bite) {
        byte significantByte = 0;
        int j = 0;
        for (int i = SIGNIFICANT_BITS - 1; i >= 0; i--) {
            byte bit = (byte) (Coder.getBit(bite, i) >>> i);
            bit = (byte) (bit << SIGNIFICANT_INDEXES[j]);

            significantByte |= bit;

            j++;
        }

        return significantByte;
    }

    /**
     * completes the byte with the lacking parity
     *
     * @param bite
     * @return
     */
    private static byte distributeParity(byte bite) {

        for (int i = 0; i < PARITY_INDEXES.length; i++) {
            int jump = (int) Math.pow(2, i);

            byte parity = HammingCoder.getParity(bite, PARITY_INDEXES[i], jump);

            bite |= parity << PARITY_INDEXES[i];
        }

        return bite;
    }


}

class HammingDecoder extends Decoder implements HammingCoder {

    public static byte[] decode(byte[] encodedSignal) {
        int signalLength = encodedSignal.length / 2;
        byte[] signal = new byte[signalLength];

        for (int i = 0; i < encodedSignal.length; i = i + 2) {
            byte firstNibble = HammingDecoder.decodeNibble(encodedSignal[i]);
            firstNibble = (byte) (firstNibble << HammingCoder.SIGNIFICANT_BITS);

            byte secondNibble = HammingDecoder.decodeNibble(encodedSignal[i + 1]);

            int signalIndex = i / 2;
            signal[signalIndex] = (byte) (firstNibble | secondNibble);
        }

        return signal;
    }

    /**
     * Decodes the provided encoded byte
     *
     * @param encodedByte the encoded nibble
     * @return the decoded byte as a nibble in this format 0000 XXXX
     */
    private static byte decodeNibble(byte encodedByte) {
        byte decodedByte = fixEncodedByte(encodedByte);

        byte nibble = 0;

        for (int i = 0; i < HammingCoder.SIGNIFICANT_INDEXES.length; i++) {
            nibble = (byte) (nibble << 1);
            nibble |= (byte) (Coder.getBit(decodedByte, HammingCoder.SIGNIFICANT_INDEXES[i]) >>> HammingCoder.SIGNIFICANT_INDEXES[i]);
        }

        return nibble;
    }

    /**
     * It looks for the wrong bit and corrects it
     *
     * @param encodedByte
     * @return ppXp xxx-
     */
    private static byte fixEncodedByte(byte encodedByte) {

        int errorIndex = errorDetection(encodedByte);
        int bitPosition = Coder.BYTE_BITS_AMOUNT - 1 - errorIndex;
/*
        byte interferedBit = Coder.getBit(encodedByte, bitPosition);
        byte fixedBit = (byte) (((interferedBit >>> bitPosition) ^ 1) << bitPosition);
        return (byte) (encodedByte | fixedBit);*///FIXME check on this right shift
        byte bitCorrect = (byte) (1 << bitPosition);

        return (byte) (encodedByte ^ bitCorrect);
    }

    /**
     * Detects the error index of the interfered bit in the byte
     *
     * @param encodedByte
     * @return returns teh index starting from 0 at the left of the byte
     */
    private static int errorDetection(byte encodedByte) {

        int errorIndex = 0;

        for (int i = 0; i < PARITY_INDEXES.length; i++) {

            int jump = (int) Math.pow(2, i);
          //  boolean check = true;
           int parityPosition = BYTE_BITS_AMOUNT - 1 - PARITY_INDEXES[i];
           /* byte partialXOR = 0;

            //Minus one because the index 7 is dismissed
            for (int j = parityPosition; j < BYTE_BITS_AMOUNT - 1; j = j + jump) {

                if (check) {
                    for (int k = 0; k < jump; k++) {
                        int bytePosition = BYTE_BITS_AMOUNT - 1 - j - k;
                        partialXOR ^= Coder.getBit(encodedByte, bytePosition) >>> bytePosition;
                    }
                }
                check = !check;
            }*/

            //byte parity = Coder.getBit(encodedByte, parityPosition);PARITY_INDEXES[i]

            byte partialXOR = HammingCoder.getParity(encodedByte, PARITY_INDEXES[i], jump);

            byte parity = (byte) ((Coder.getBit(encodedByte, PARITY_INDEXES[i]) & 0xFF) >>> PARITY_INDEXES[i]);
            //byte parity = (byte) ((byte) (Coder.getBit(encodedByte, PARITY_INDEXES[i]) >>> PARITY_INDEXES[i]);
            if (parity != partialXOR) {
                errorIndex += parityPosition;
            }

        }

        return errorIndex + 1;
    }

}

public class Main {

    private final static String SENT_FILE = "send.txt";
    private final static String ENCODED_FILE = "encoded.txt";
    private final static String RECEIVED_FILE = "received.txt";
    private final static String DECODED_FILE = "decoded.txt";

    public static void main(String[] args) {
        Main main = new Main();
        InterferenceGenerator ie = new InterferenceGenerator();

        Mode mode = main.getMode();

        try {
            byte[] signal = main.getSignal();


            switch (mode) {
                case ENCODE:
                    main.encode();
                    break;

                case SEND:
                    main.sendSignal();
                    break;
                case DECODE:
                    main.decode();
                    break;
            }
        } catch (IOException e) {
            System.out.println(e.getCause());
            e.printStackTrace();
        }
    }

    /**
     * Get the signal from the file.
     *
     * @return returns the input signal
     */
    private byte[] getSignal() throws IOException {
        return Files.readAllBytes(Paths.get(SENT_FILE));
    }


    public Mode getMode() {
        Scanner scanner = new Scanner(System.in);
        return Mode.valueOf(scanner.nextLine().toUpperCase());
    }

    /**
     * If the mode is encode then you need to take the text from send.txt,
     * convert it to ready-to-send form (where you have three significant bits per byte)
     * and save the resulted bytes into the file named encoded.txt.
     */
    public void encode() throws IOException {
        byte[] signal = Files.readAllBytes(Paths.get(SENT_FILE));

        signal = HammingEncoder.encode(signal);

        OutputStream writer = new FileOutputStream(ENCODED_FILE, false);
        writer.write(signal);
        writer.close();
    }


    /**
     * you should take the file from encoded.txt
     * and simulate the errors in its bytes (1 bit per byte) and save
     * the resulted bytes into the file named received.txt.
     */
    private void sendSignal() throws IOException {

        byte[] signal = Files.readAllBytes(Paths.get(ENCODED_FILE));

        signal = InterferenceGenerator.interfere(signal);

        OutputStream writer = new FileOutputStream(RECEIVED_FILE, false);
        writer.write(signal);
        writer.close();
    }

    /**
     * you should take the file from received.txt and decode it to the normal text.
     * Save the text into the file named decoded.txt.
     */
    public void decode() throws IOException {
        byte[] signal = Files.readAllBytes(Paths.get(RECEIVED_FILE));

        signal = HammingDecoder.decode(signal);

        OutputStream writer = new FileOutputStream(DECODED_FILE, false);
        writer.write(signal);
        writer.close();
    }

}

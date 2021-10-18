import java.io.CharArrayWriter;
import java.io.FileWriter;
import java.io.IOException;

class Converter {
    public static char[] convert(String[] words) throws IOException {
        // implement the method
        CharArrayWriter writer = new CharArrayWriter();
        for (String word: words) {
            writer.write(word);
        }
        char[] result = writer.toCharArray();
        writer.close();
        FileWriter w = new FileWriter()
        return result;
    }
}
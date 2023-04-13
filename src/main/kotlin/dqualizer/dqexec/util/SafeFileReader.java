package dqualizer.dqexec.util;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Helps to read a local file and get the content as a string
 * ( Handle the try-catch-block )
 */
@Service
public class SafeFileReader {

    public String readFile(String path) {
        String text;
        try { text = Files.readString(Paths.get(path)); }
        catch (IOException e) { throw new RuntimeException(e); }
        return text;
    }
}
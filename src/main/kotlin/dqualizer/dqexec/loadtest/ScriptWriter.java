package dqualizer.dqexec.loadtest;

import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Writes a k6-script
 */
@Component
public class ScriptWriter {

    /**
     * Write a k6-script
     * @param script A list of strings, which should be written inside a file
     * @param scriptPath The path where the file should be created
     * @throws IOException
     */
    public void write(List<String> script, String scriptPath) throws IOException {
        FileWriter writer = new FileWriter(scriptPath);

        for(String line: script) writer.write(line);
        writer.close();
    }
}
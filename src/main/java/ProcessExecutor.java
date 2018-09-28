import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;

public class ProcessExecutor {

    public static Process run(String puttyPath) {
        Console console = System.console();
        if(console == null && !GraphicsEnvironment.isHeadless()) {
            try {
                ProcessBuilder builder = new ProcessBuilder(puttyPath + "\\putty.exe -ssh -load \"RaspiSSH\" pi@192.168.0.14 -pw raspberry");
                return builder.start();
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else {
            //your program code...
        }
        return null;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Process process = run("C:\\Users\\Stephan\\Desktop\\PuTTY");
        if (process == null) return;

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread.sleep(1000);
        while (reader.ready()) System.out.println(reader.readLine());
    }
}

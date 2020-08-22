package de.codemakers.jdownloadproxy;

import io.micronaut.runtime.Micronaut;

import java.io.File;

public class Application {
    
    public static final File APPLICATION_FOLDER = new File("JDownloadProxy");
    
    static {
        APPLICATION_FOLDER.mkdirs();
    }
    
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
        if (args.length == 1) {
            new Thread(() -> {
                try {
                    final int millis = Integer.parseInt(args[0]);
                    System.out.printf("Sleeping %d ms%n", millis);
                    Thread.sleep(millis);
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }).start();
        }
    }
    
}

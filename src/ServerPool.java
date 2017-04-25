import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by Yorkson on 2017/4/24.
 */
public class ServerPool {
    private static final Logger logger = Logger.getLogger(HttpServer.class.getCanonicalName());
    private static final int NUM_THREADS = 10;
    private static final String INDEX_FILE = "Lena.png";

    private final File rootDirectory;
    private final int port;
    private final byte[] cache;
    public ServerPool(File rootDirectory,int port) throws IOException {
        if (!rootDirectory.isDirectory()){
            throw new IOException(rootDirectory+" does not exit as a directory");
        }
        this.cache =  Files.readAllBytes((new File(rootDirectory,INDEX_FILE)).toPath());
        this.rootDirectory = rootDirectory;
        this.port = port;
    }

    private void start(){
        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        try (ServerSocket serverSocket = new ServerSocket(port)){
            logger.info("Accepting connections on port:"+serverSocket.getLocalPort());
            logger.info("Document root:"+rootDirectory);

            while (true){
                try {
                    Socket request = serverSocket.accept();
                    Runnable r  = new RqProcessor(rootDirectory,INDEX_FILE,request,cache);
                    pool.submit(r);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File docroot;
        try {
            docroot = new File("/Users/Yorkson/IdeaProjects/Threadpool_Java_Server/");
        }catch (ArrayIndexOutOfBoundsException ex){
            return;
        }
        try {
            ServerPool server = new ServerPool(docroot,8080);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Created by Yorkson on 2017/4/24.
 */
public class RqProcessor implements Runnable{
    private final static Logger logger = Logger.getLogger(RqProcessor.class.getCanonicalName());
    private File rootDirectory;
    private String INDEX_FILE = "Lena.png";
    private Socket connection;
    private byte[] cache;


    public RqProcessor(File rootDirectory, String indexFile, Socket request,byte[] cache) {
        if (rootDirectory.isFile()){
            throw new IllegalArgumentException("rootDirectory must be a directory, not a file");
        }
        try {
            rootDirectory = rootDirectory.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.rootDirectory = rootDirectory;
        this.connection = request;
        this.cache = cache;
        if (INDEX_FILE==null)
            this.INDEX_FILE = indexFile;
    }
    private void sendHeader(Writer out, String responseCode, String contentType, int length) throws IOException {
        out.write(responseCode+"\r\n");
        Date now = new Date();
        out.write("Date:"+now+"\r\n");
        out.write("Server:JHTTP 1.0\r\n");
        out.write("Content-length:"+length+"\r\n");
        out.write("Content-Type:"+contentType+"\r\n");
        out.write("\r\n");
        out.flush();
    }
    private void sendHeader(Writer out, String responseCode) throws IOException {
        out.write(responseCode+"\r\n");
        Date now = new Date();
        out.write("Date:"+now+"\r\n");
        out.write("Server:JHTTP 1.0\r\n");
        out.write("\r\n");
        out.flush();
    }
    @Override
    public void run(){
        String root = rootDirectory.getPath();
        try {
            OutputStream raw = new BufferedOutputStream(connection.getOutputStream());
            Writer out = new OutputStreamWriter(raw);
            Reader in  = new InputStreamReader(new BufferedInputStream((connection.getInputStream())),"ASCII");
            StringBuilder request = new StringBuilder(80);
            while(true) {
                int c = in.read();
                if (c=='\r'||c=='\n'||c == -1) {
                    break;
                }
                request.append((char)c);
            }
            String get = request.toString();
            logger.info(connection.getRemoteSocketAddress()+" "+get);
            StringTokenizer st = new StringTokenizer(get);
            String method=st.nextToken();
            String version = "";

            if (method.equals("GET")){
                String fileName = st.nextToken();
                if (fileName.endsWith("/")) fileName += INDEX_FILE;
                String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
                if(st.hasMoreTokens()){
                    version=st.nextToken();
                }
                File theFile = new File(rootDirectory,fileName.substring(1,fileName.length()));
                System.out.println(theFile);
                if (!theFile.exists()){
                    sendHeader(out,"HTTP/1.1 404 Not Found");
//                    TODO
//                    theFile = new File(rootDirectory,"errorPage.html");
//                    byte[] theData = Files.readAllBytes(theFile.toPath());
//                    raw.write(theData);
//                    raw.flush();
                }else if (!theFile.canRead()||!theFile.getCanonicalPath().startsWith(root)){
                    sendHeader(out,"HTTP/1.1 403 NO READ PERMISSIONS");
                }
                else {//检测所请求文件是否超出根目录
                    if(theFile.toPath().toString().equals(rootDirectory.toPath()+"/"+INDEX_FILE)){
                        System.out.printf("OK");
                        sendHeader(out,"HTTP/1.1 200 OK",contentType,cache.length);
                        raw.write(cache);
                        raw.flush();
                    }else {
                        byte[] theData = Files.readAllBytes(theFile.toPath());
                        if (theData == null){
                            sendHeader(out,"500 INTERNAL SERVER ERROR");
                        }else if (version.startsWith("HTTP/")){
                            System.out.printf("OOK");
                            sendHeader(out,"HTTP/1.1 200 OK",contentType,theData.length);
                            raw.write(theData);
                            raw.flush();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
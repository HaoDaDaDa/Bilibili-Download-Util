import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * @program: Java代码练习
 * @author: Abel Lee
 * @create: 2019-02-13 04:47
 **/

public class BilibiliVideoDownloadUtil {

    private static String fileUrl = "";

    private static String cookie = "";

    private static String avNum = "";

    private static String pNum = "1";

    private static  String title = "";


    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("请输入要下载的av号");
        avNum = scanner.nextLine();

        System.out.println("请输入要下载的p号，不输入则默认为p1");
        if(!"".equals(scanner.nextLine()) && scanner.nextLine() != null){
            pNum = scanner.nextLine();
        }

        System.out.println("开始下载");

        BilibiliVideoDownloadUtil bilibiliVideoDownloadUtil = new BilibiliVideoDownloadUtil();

        bilibiliVideoDownloadUtil.initConfig();

        String htmlContext;
        try {
            htmlContext = bilibiliVideoDownloadUtil.getDownAddressByAvNumAndPNum(avNum, pNum, cookie);
        } catch (IOException e) {
            throw new RuntimeException("获取htmlContext过程失败");
        }

        // 获取下载视频流地址
        String downloadUrl = bilibiliVideoDownloadUtil.reGexMatch(htmlContext);

        // 获取下载的title
        bilibiliVideoDownloadUtil.getTitleFromWebsiteHtml(htmlContext, pNum);

        try {
            bilibiliVideoDownloadUtil.downloadVideo(downloadUrl, fileUrl);
        } catch (IOException e) {
            throw new RuntimeException("下载视频过程失败");
        }

    }

    /**
     * 下载视频
     * @param videoUrl 视频流的具体地址
     */
    private void downloadVideo(String videoUrl, String fileUrl) throws IOException {
        URL url = new URL(videoUrl);

        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        httpURLConnection.setRequestProperty("Accept","*/*");
        httpURLConnection.setRequestProperty("Origin","https://www.bilibili.com");
        httpURLConnection.setRequestProperty("Host","upos-hz-mirrorkodou.acgvideo.com");
        httpURLConnection.setRequestProperty("Accept-Encoding","br, gzip, deflate");
        httpURLConnection.setRequestProperty("Accept-Language","zh-cn");
        httpURLConnection.setRequestProperty("Referer","https://www.bilibili.com/");
        httpURLConnection.setRequestProperty("Connection","keep-alive");
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0.1 Safari/605.1.15");

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream()); BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileUrl+title+".flv"))) {
            byte[] b = new byte[1024];
            int n;
            long flag = 0L;

            while ((n = bufferedInputStream.read(b)) != -1) {
                bufferedOutputStream.write(b, 0, n);
                flag++;
                // 进度条不准
                if (flag % 1024 == 0){
                    System.out.println("已经下载了" + flag/1024 + "MB");
                }
            }

            bufferedOutputStream.flush();
        }

    }


    /**
     * 通过输入av和p号获取html文档
     * @param avNum 输入的av号
     * @param pNum 输入的p号，如果为null或为空字符串，则默认为1
     * @return 返回html
     * @throws IOException 异常
     */
    private String getDownAddressByAvNumAndPNum(String avNum, String pNum,String cookie) throws IOException {
        if (pNum == null || "".equals(pNum.trim())){
            pNum = "1";
        }

        URL url = null;
        try {
            url = new URL("https://www.bilibili.com/video/av"+ avNum +"/?p="+ pNum);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection httpURLConnection = null;
        try {
            assert url != null;
            httpURLConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert httpURLConnection != null;
        httpURLConnection.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpURLConnection.setRequestProperty("Host","www.bilibili.com");
        httpURLConnection.setRequestProperty("Accept-Encoding","gzip, deflate, br");
        httpURLConnection.setRequestProperty("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8");
        httpURLConnection.setRequestProperty("Connection","keep-alive");
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0.1 Safari/605.1.15");
        httpURLConnection.setRequestProperty("Cookie", cookie);

        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(httpURLConnection.getInputStream())))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }


    /**
     * 通过正则表达式和字符串处理返回我们需要的视频流地址
     * @param context 从b站主页截取的文本
     * @return 返回我们需要的视频流地址
     */
    private String reGexMatch(String context){

        String reGex = "url\":\"http://upos-hz.+\",\"back";

        Pattern pattern = Pattern.compile(reGex);

        Matcher matcher = pattern.matcher(context);

        String result = "";

        while (matcher.find()){
            result  = matcher.group();
        }

        return result.substring(6, result.length() - 7);
    }


    /**
     * 根据传入的p号和html文本 得到标题 赋值给title
     * @param context 传入的网页html文件
     * @param pNum p号
     */
    private void getTitleFromWebsiteHtml(String context, String pNum){

        String reGex = "\"page\":" + pNum + ",\"from\":\"vupload\",\"part\":\"[^\"]+";

        Pattern pattern = Pattern.compile(reGex);

        Matcher matcher = pattern.matcher(context);

        String result = "";

        while (matcher.find()){
            result  = matcher.group();
        }

        title = result.substring(34);
    }


    private void initConfig(){
        String thisDir = System.getProperty("user.dir");
        String systemName = System.getProperty("os.name");
        String prefix = "/";
        String windows = "Windows";

        if (windows.equals(systemName)){
            prefix = "\\";
        }

        try (FileInputStream in = new FileInputStream(thisDir + prefix + "config.properties")) {
            Properties prop = new Properties();
            try {
                prop.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileUrl = prop.getProperty("fileUrl");
            cookie = prop.getProperty("cookie");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

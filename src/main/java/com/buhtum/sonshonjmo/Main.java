package com.buhtum.sonshonjmo;

import com.google.common.base.Strings;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * A simple class that implements a few features that we can write unit tests for. It uses Google Guava for the sole
 * reason that we have an external compile-time dependency in this bootstrap project.
 * <p>
 * To make the example code simpler this class does not follow pure OO style. (If it did, we would create a Painting
 * object from painting elements and let our painter paint that painting object, for instance.)
 * <p>
 * "And from all of us here I'd like to wish you happy painting and God bless, my friend."
 */
public class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);
    private final static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    public static final String URL = "http://appd-bg.org/bg/level_bg.php";

    public static void main(String[] args) throws IOException {
        final Twitter twitter = getInstance();
        final String screenName;
        try {
            screenName = twitter.getScreenName();
            log.debug(String.valueOf(screenName));
        } catch (TwitterException e) {
            throw new RuntimeException();
        }

        Timer timer = new Timer();
        final DateTime start = new DateTime().withTimeAtStartOfDay().plusDays(1).withHourOfDay(15);
        log.info("Next update at: " + start);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    twitter.updateStatus(buildTweet());
                } catch (TwitterException | IOException e) {
                    log.error("Failed to tweet", e);
                }
            }
        }, start.toDate(), TimeUnit.DAYS.toMillis(1));
    }

    private static String buildTweet() throws IOException {
        final HttpUrl url = HttpUrl.parse(URL).newBuilder().build();
        final Request request = new Request.Builder().url(url).build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        final String body = response.body().string();
        Document doc = Jsoup.parse(body, URL);


        final Elements rows = doc.select("table.local tbody tr");
        StringBuilder tweetBuilder = new StringBuilder();
        for (Element row : rows) {
            final Elements columns = row.select("td");
            final String station = columns.get(1).text();
            final String value = columns.get(2).select("a").text();
            final String diff = columns.get(3).text();
            tweetBuilder.append(station).append(" ").append(value).append("/").append(diff).append("\n");
        }
        log.debug("Tweet ({}): {}", tweetBuilder.length(), tweetBuilder);
        return tweetBuilder.toString();
    }

    public static Twitter getInstance() {

        try {
            final String configFile = System.getProperty("auth");
            Properties twitterConfig = loadConfig(configFile);

            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(
                    twitterConfig.getProperty("twitter4j.oauth.consumerKey"),
                    twitterConfig.getProperty("twitter4j.oauth.consumerSecret"));

            final String accessTokenStr = twitterConfig.getProperty("twitter4j.oauth.accessToken");
            final String accessTokenSecret = twitterConfig.getProperty("twitter4j.oauth.accessToken.secret");

            if (Strings.isNullOrEmpty(accessTokenStr) || Strings.isNullOrEmpty(accessTokenSecret)) {
                AccessToken accessToken;
                accessToken = createAccessToken(twitter);
                twitterConfig.setProperty("twitter4j.oauth.accessToken", String.valueOf(accessToken.getToken()));
                twitterConfig.setProperty("twitter4j.oauth.accessToken.secret", String.valueOf(accessToken.getTokenSecret()));
                saveConfig(twitterConfig, configFile);
            } else {
                twitter.setOAuthAccessToken(new AccessToken(accessTokenStr, accessTokenSecret));
            }

            return twitter;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void saveConfig(Properties twitterConfig, String configFile) {
        try (final OutputStream outputStream = Files.newOutputStream(Paths.get(configFile), StandardOpenOption.CREATE)) {
            twitterConfig.store(outputStream, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties loadConfig(String configFile) throws IOException {
        Properties twitterConfig = new Properties();
        try (final InputStream inStream = Files.newInputStream(Paths.get(configFile), StandardOpenOption.READ)) {
            twitterConfig.load(inStream);
        }
        return twitterConfig;
    }

    private static AccessToken createAccessToken(Twitter twitter) {
        try {
            // get request token.
            // this will throw IllegalStateException if access token is already available
            RequestToken requestToken = twitter.getOAuthRequestToken();
            System.out.println("Got request token.");
            System.out.println("Request token: " + requestToken.getToken());
            System.out.println("Request token secret: " + requestToken.getTokenSecret());

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            AccessToken accessToken = null;
            while (null == accessToken) {
                System.out.println("Open the following URL and grant access to your account:");
                System.out.println(requestToken.getAuthorizationURL());
                System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
                String pin = br.readLine();
                try {
                    if (pin.length() > 0) {
                        accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                    } else {
                        accessToken = twitter.getOAuthAccessToken(requestToken);
                    }
                } catch (TwitterException te) {
                    throw new RuntimeException(te);
                }
            }
            System.out.println("Got access token.");
            System.out.println("Access token: " + accessToken.getToken());
            System.out.println("Access token secret: " + accessToken.getTokenSecret());
            return accessToken;
        } catch (IllegalStateException ie) {
            // access token is already available, or consumer key/secret is not set.
            if (!twitter.getAuthorization().isEnabled()) {
                throw new RuntimeException("OAuth consumer key/secret is not set.");
            }
        } catch (TwitterException | IOException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Cannot get access token");
    }
}

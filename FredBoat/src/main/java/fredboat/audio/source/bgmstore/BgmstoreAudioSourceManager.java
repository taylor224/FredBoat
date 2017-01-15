package fredboat.audio.source.bgmstore;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import com.sedmelluq.discord.lavaplayer.container.MediaContainer;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import org.apache.http.HttpStatus;
import fredboat.audio.source.bgmstore.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import java.net.URISyntaxException;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects BGMStore tracks by URL.
 */
public class BgmstoreAudioSourceManager implements AudioSourceManager {
  private static final String TRACK_URL_REGEX = "^(?:http://|https://|)(?:www\\\\.|)bgmstore.net/view/([a-zA-Z0-9_-]{5})(?:\\\\?.*|&.*|)$";
  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

  private final HttpClientBuilder httpClientBuilder;

  /**
   * Create an instance.
   */
  public BgmstoreAudioSourceManager() {
    httpClientBuilder = HttpClientTools.createSharedCookiesHttpBuilder();
    httpClientBuilder.setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy());
  }

  @Override
  public String getSourceName() {
    return "bgmstore";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    if (!trackUrlPattern.matcher(reference.identifier).matches()) {
      return null;
    }

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      AudioItem loadedItem = loadFromTrackPage(httpClient, reference.identifier);

      if (loadedItem == null) {
        return AudioReference.NO_TRACK;
      }

      return loadedItem;
    } catch (IOException e) {
      throw new FriendlyException("BGMStore 의 트랙 정보를 불러오는데에 실패하였습니다.", SUSPICIOUS, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    // Nothing special to encode
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    return new BgmstoreAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }

  /**
   * @return A new HttpClient instance. All instances returned from this method use the same cookie jar.
   */
  public CloseableHttpClient createHttpClient() {
    return httpClientBuilder.build();
  }

  private AudioItem loadFromTrackPage(CloseableHttpClient httpClient, String trackUrl) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(trackUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 404) {
        return AudioReference.NO_TRACK;
      } else if (statusCode != 200) {
        throw new FriendlyException("서버가 처리되지 않은 에러코드를 응답했습니다.", SUSPICIOUS,
            new IllegalStateException("에러코드 : " + statusCode));
      }

      return loadTrackFromPageContent(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8), trackUrl);
    }
  }

  public AudioTrack loadTrackFromPageContent(String content, String trackUrl) throws IOException {
    Document doc = Jsoup.parse(content);

    String checktitle = doc.select("head").select("meta[property=og:title]").attr("content");

    if (checktitle == null || checktitle.equals("") || checktitle.equals("404 Not Found")) {
      return null;
    }

    String title = doc.select("div.titleBox").first().ownText();

    String[] durationtext = doc.select("div.titleBox").select("span").first().text().replace("[", "").replace("]", "").split(":");
    Double duration = 0.0;

    if (Integer.parseInt(durationtext[0]) > 0) {
      duration += Double.parseDouble(durationtext[0]) * 60;
    }
    if (Integer.parseInt(durationtext[1]) > 0) {
      duration += Integer.parseInt(durationtext[1]);
    }

    Matcher matcher = trackUrlPattern.matcher(trackUrl);
    matcher.matches();

    return new BgmstoreAudioTrack(new AudioTrackInfo(
        title,
        "BGM Store",
        (long) (duration * 1000.0),
            matcher.group(1),
        false
    ), this);
  }
}

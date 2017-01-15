package fredboat.audio.source.bgmstore;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track that handles processing Vimeo tracks.
 */
public class BgmstoreAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(BgmstoreAudioTrack.class);

  private final BgmstoreAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public BgmstoreAudioTrack(AudioTrackInfo trackInfo, BgmstoreAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (CloseableHttpClient httpClient = sourceManager.createHttpClient()) {
      String playbackUrl = loadPlaybackUrl(httpClient, trackInfo.identifier);

      log.debug("Starting BGMStore track from URL: {}", playbackUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(httpClient, new URI(playbackUrl), null)) {
        processDelegate(new Mp3AudioTrack(trackInfo, stream), localExecutor);
      }
    }
  }

  private String loadPlaybackUrl(CloseableHttpClient httpClient, String identifier) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://bgmstore.net/view/" + trackInfo.identifier))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code for player config is " + statusCode));
      }

      Document doc = Jsoup.parse(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
      String url = doc.select(".bgmInfo").select("ul.dropdown-menu").select("a.btnDownloadInner").get(1).attr("href");

      if (url == null) {
        throw new FriendlyException("트랙 정보를 찾을수 없습니다.", SUSPICIOUS, null);
      }

      return url;
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new BgmstoreAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}

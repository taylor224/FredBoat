/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat;

import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.util.DiscordUtil;
import fredboat.util.DistributionEnum;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    
    public static Config CONFIG = null;

    public static String DEFAULT_PREFIX = ";;";

    private final DistributionEnum distribution;
    private final String botToken;
    private String oauthSecret;
    private final String jdbcUrl;
    private final int numShards;
    private String mashapeKey;
    private String malPassword;
    private int scope;
    private List<String> googleKeys = new ArrayList<>();
    private final String[] lavaplayerNodes;
    private final boolean lavaplayerNodesEnabled;
    private String carbonKey;
    private String cbUser;
    private String cbKey;
    private String prefix = DEFAULT_PREFIX;
    private boolean restServerEnabled = true;

    public Config(File credentialsFile, File configFile, int scope) {
        try {
            this.scope = scope;
            JSONObject creds = new JSONObject(FileUtils.readFileToString(credentialsFile, "UTF-8"));
            JSONObject config = new JSONObject(FileUtils.readFileToString(configFile, "UTF-8"));

            // Determine distribution
            if (config.optBoolean("patron")) {
                distribution = DistributionEnum.PATRON;
            } else if (config.optBoolean("development")) {//Determine distribution
                distribution = DistributionEnum.DEVELOPMENT;
            } else {
                distribution = DiscordUtil.isMainBot(this) ? DistributionEnum.MAIN : DistributionEnum.MUSIC;
            }

            log.info("Determined distribution: " + distribution);

            prefix = config.optString("prefix", prefix);
            restServerEnabled = config.optBoolean("restServerEnabled", restServerEnabled);

            log.info("Using prefix: " + prefix);

            mashapeKey = creds.optString("mashapeKey");
            malPassword = creds.optString("malPassword");
            carbonKey = creds.optString("carbonKey");
            cbUser = creds.optString("cbUser");
            cbKey = creds.optString("cbKey");
            botToken = creds.getJSONObject("token").getString(distribution.getId());
            cbKey = creds.optString("cbKey");
            if(creds.has("oauthSecret")){
                oauthSecret = creds.getJSONObject("oauthSecret").optString(distribution.getId());
            }
            if(creds.has("jdbcUrl")){
                jdbcUrl = creds.getString("jdbcUrl");
            } else {
                jdbcUrl = "";
            }

            JSONArray gkeys = creds.optJSONArray("googleServerKeys");
            if (gkeys != null) {
                gkeys.forEach((Object str) -> googleKeys.add((String) str));
            }

            JSONArray nodesArray = creds.optJSONArray("lavaplayerNodes");
            if(nodesArray != null) {
                lavaplayerNodesEnabled = true;
                lavaplayerNodes = new String[nodesArray.length()];
                log.info("Using lavaplayer nodes");
                Iterator<Object> itr = nodesArray.iterator();
                int i = 0;
                while(itr.hasNext()) {
                    lavaplayerNodes[i] = (String) itr.next();
                    i++;
                }
            } else {
                lavaplayerNodesEnabled = false;
                lavaplayerNodes = new String[0];
                log.info("Not using lavaplayer nodes. Audio playback will be processed locally.");
            }

            if(getDistribution() == DistributionEnum.DEVELOPMENT) {
                log.info("Development distribution; forcing 2 shards");
                numShards = 2;
            } else {
                numShards = DiscordUtil.getRecommendedShardCount(getBotToken());
                log.info("Discord recommends " + numShards + " shard(s)");
            }

        } catch (IOException | UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRandomGoogleKey() {
        return getGoogleKeys().get((int) Math.floor(Math.random() * getGoogleKeys().size()));
    }

    public DistributionEnum getDistribution() {
        return distribution;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getOauthSecret() {
        return oauthSecret;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public int getNumShards() {
        return numShards;
    }

    public String getMashapeKey() {
        return mashapeKey;
    }

    public String getMalPassword() {
        return malPassword;
    }

    public int getScope() {
        return scope;
    }

    public List<String> getGoogleKeys() {
        return googleKeys;
    }

    public String[] getLavaplayerNodes() {
        return lavaplayerNodes;
    }

    public boolean isLavaplayerNodesEnabled() {
        return lavaplayerNodesEnabled;
    }

    public String getCarbonKey() {
        return carbonKey;
    }

    public String getCbUser() {
        return cbUser;
    }

    public String getCbKey() {
        return cbKey;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isRestServerEnabled() {
        return restServerEnabled;
    }
}

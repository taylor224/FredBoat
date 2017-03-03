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

package fredboat.command.util;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.Command;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MALCommand extends Command {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MALCommand.class);
    private static Pattern regex = Pattern.compile("^\\S+\\s+([\\W\\w]*)");

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        Matcher matcher = regex.matcher(message.getContent());
        try {
            matcher.find();
        } catch (IllegalStateException e) {
            throw new MessagingException(I18n.get(guild).getString("malUsage").replace(Config.DEFAULT_PREFIX, Config.CONFIG.getPrefix()));
        }
        String term = matcher.group(1).replace(' ', '+').trim();
        log.debug("TERM:"+term);

        try {
            String body = Unirest.get("https://myanimelist.net/api/anime/search.xml?q=" + term).basicAuth("FredBoat", Config.CONFIG.getMalPassword()).asString().getBody();
            if (body != null && body.length() > 0) {
                if(handleAnime(channel, invoker, term, body)){
                    return;
                }
            }

            body = Unirest.get("http://myanimelist.net/search/prefix.json?type=user&keyword=" + term).basicAuth("FredBoat", Config.CONFIG.getMalPassword()).asString().getBody();
            handleUser(channel, invoker, body);
        } catch (UnirestException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean handleAnime(TextChannel channel, Member invoker, String terms, String body) {
        String msg = MessageFormat.format(I18n.get(channel.getGuild()).getString("malRevealAnime"), invoker.getEffectiveName());

        //Read JSON
        log.info(body);
        JSONObject root = XML.toJSONObject(body);
        JSONObject data;
        try {
            data = root.getJSONObject("anime").getJSONArray("entry").getJSONObject(0);
        } catch (JSONException ex) {
            data = root.getJSONObject("anime").getJSONObject("entry");
        }
        
        ArrayList<String> titles = new ArrayList<>();
        titles.add(data.getString("title"));
        
        if(data.has("synonyms")){
            titles.addAll(Arrays.asList(data.getString("synonyms").split(";")));
        }
        
        if(data.has("english")){
            titles.add(data.getString("english"));
        }
        
        int minDeviation = Integer.MAX_VALUE;
        for(String str : titles){
            str = str.replace(' ', '+').trim();
            int deviation = str.compareToIgnoreCase(terms);
            deviation = deviation - Math.abs(str.length() - terms.length());
            if(deviation < minDeviation){
                minDeviation = deviation;
            }
        }
        
        
        log.debug("Anime search deviation: " + minDeviation);
        
        if(minDeviation > 3){
            return false;
        }
        
        msg = data.has("title") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malTitle"), msg, data.get("title")) : msg;
        msg = data.has("english") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malEnglishTitle"), msg, data.get("english")) : msg;
        msg = data.has("synonyms") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malSynonyms"), msg, data.get("synonyms")) : msg;
        msg = data.has("episodes") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malEpisodes"), msg, data.get("episodes")) : msg;
        msg = data.has("score") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malScore"), msg, data.get("score")) : msg;
        msg = data.has("type") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malType"), msg, data.get("type")) : msg;
        msg = data.has("status") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malStatus"), msg, data.get("status")) : msg;
        msg = data.has("start_date") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malStartDate"), msg, data.get("start_date")) : msg;
        msg = data.has("end_date") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malEndDate"), msg, data.get("end_date")) + "\n" : msg;
        
        if(data.has("synopsis")){
            Matcher m = Pattern.compile("^[^\\n\\r<]+").matcher(StringEscapeUtils.unescapeHtml4(data.getString("synopsis")));
            m.find();
            msg = data.has("synopsis") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malSynopsis"), msg, m.group(0)) : msg;
        }

        msg = data.has("id") ? msg + "http://myanimelist.net/anime/" + data.get("id") + "/" : msg;
        
        channel.sendMessage(msg).queue();
        return true;
    }

    private boolean handleUser(TextChannel channel, Member invoker, String body) {
        String msg = MessageFormat.format(I18n.get(channel.getGuild()).getString("malUserReveal"), invoker.getEffectiveName());

        //Read JSON
        JSONObject root = new JSONObject(body);
        JSONArray items = root.getJSONArray("categories").getJSONObject(0).getJSONArray("items");
        if(items.length() == 0){
            channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("malNoResults"), invoker.getEffectiveName())).queue();
            return false;
        }
        
        JSONObject data = items.getJSONObject(0);
        
        msg = data.has("name") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malUserName"), msg, data.get("name")) : msg;
        msg = data.has("url") ? MessageFormat.format(I18n.get(channel.getGuild()).getString("malUrl"), msg, data.get("url")) : msg;
        msg = data.has("image_url") ? msg + data.get("image_url") : msg;

        log.debug(msg);

        channel.sendMessage(msg).queue();
        return true;
    }

}

/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.command.music.control;

import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.VideoSelection;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.util.YoutubeAPI;
import fredboat.util.YoutubeVideo;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Dictionary;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayCommand extends Command implements IMusicCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PlayCommand.class);

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if (!message.getAttachments().isEmpty()) {
            GuildPlayer player = PlayerRegistry.get(guild);
            player.setCurrentTC(channel);
            
            for (Attachment atc : message.getAttachments()) {
                player.queue(atc.getUrl(), channel, invoker);
            }
            
            player.setPause(false);
            
            return;
        }

        String radioChannel = null;
        Map<String, String> radioUrl = new HashMap<>();


        try {
            FileInputStream is = new FileInputStream(new File("config.json"));
            Scanner scanner = new Scanner(is);
            JSONObject config = new JSONObject(scanner.useDelimiter("\\A").next());
            scanner.close();

            radioChannel = config.optString("radio");

            radioUrl.put("nhk_r1", config.optString("nhk_r1"));
            radioUrl.put("nhk_r2", config.optString("nhk_r2"));
            radioUrl.put("nhk_r3", config.optString("nhk_r3"));
            radioUrl.put("mbc_fm4u", config.optString("mbc_fm4u"));
            radioUrl.put("mbc_fm", config.optString("mbc_fm"));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Config file not found", e);
        }

        String streamUrl = "";

        switch (radioChannel) {
            case "nhk_r1"     :   streamUrl = radioUrl.get("nhk_r1"); break;
            case "nhk_r2"     :   streamUrl = radioUrl.get("nhk_r2"); break;
            case "nhk_r3"     :   streamUrl = radioUrl.get("nhk_r3"); break;
            case "mbc_fm4u"   :   streamUrl = radioUrl.get("mbc_fm4u"); break;
            case "mbc_fm"     :   streamUrl = radioUrl.get("mbc_fm"); break;
            default           :   streamUrl = radioUrl.get("nhk_r1"); break;
        }

        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);

        player.queue(streamUrl, channel, invoker);
        player.setPause(false);

        try {
            message.deleteMessage().queue();
        } catch (Exception ignored) {

        }
    }

    private void handleNoArguments(Guild guild, TextChannel channel, Member invoker, Message message) {
        GuildPlayer player = PlayerRegistry.get(guild);
        if (player.isQueueEmpty()) {
            channel.sendMessage("현재 플레이어가 재생할수 있는 곡이 없습니다. 다음 명령어를 통해 곡을 추가해주세요. \n;;play <주소 혹은 검색어>").queue();
        } else if (player.isPlaying()) {
            channel.sendMessage("이미 재생 중입니다.").queue();
        } else if (player.getUsersInVC().isEmpty()) {
            channel.sendMessage("현재 음성채널에 접속해 있지 않습니다. 플레이를 위해서는 먼저 음성채널에 접속해야 합니다.").queue();
        } else {
            player.play();
            channel.sendMessage("잠시후 재생이 시작됩니다.").queue();
        }
    }

}

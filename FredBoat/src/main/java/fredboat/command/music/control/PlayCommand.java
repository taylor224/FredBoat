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
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

        if (args.length < 2) {
            handleNoArguments(guild, channel, invoker, message);
            return;
        }

        //What if we want to select a selection instead?
        if (args.length == 2 && StringUtils.isNumeric(args[1])){
            SelectCommand.select(guild, channel, invoker, message, args);
            return;
        }

        //Search youtube for videos and let the user select a video
        if (!args[1].startsWith("http")) {
            try {
                searchForVideos(guild, channel, invoker, message, args);
            } catch (RateLimitedException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);

        player.queue(args[1], channel, invoker);
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

    private void searchForVideos(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) throws RateLimitedException {
        Matcher m = Pattern.compile("\\S+\\s+(.*)").matcher(message.getRawContent());
        m.find();
        String query = m.group(1);
        
        //Now remove all punctuation
        query = query.replaceAll("[.,/#!$%\\^&*;:{}=\\-_`~()]", "");

        Message outMsg = channel.sendMessage("`{q}` 을 검색하고 있습니다...".replace("{q}", query)).complete(true);

        ArrayList<YoutubeVideo> vids = null;
        try {
            vids = YoutubeAPI.searchForVideos(query);
        } catch (JSONException e) {
            channel.sendMessage("유튜브 곡 검색 중 에러가 발생하였습니다. 유튜브 주소 입력을 통해서도 곡 추가가 가능합니다.\n```\n;;play <유튜브 주소>```").queue();
            log.debug("YouTube search exception", e);
            return;
        }

        if (vids.isEmpty()) {
            outMsg.editMessage("`{q}` 에 대한 검색결과가 없습니다.".replace("{q}", query)).queue();
        } else {
            //Clean up any last search by this user
            GuildPlayer player = PlayerRegistry.get(guild);

            VideoSelection oldSelection = player.selections.get(invoker.getUser().getId());
            if(oldSelection != null) {
                oldSelection.getOutMsg().deleteMessage().queue();
            }

            MessageBuilder builder = new MessageBuilder();
            builder.append("**`;;play 숫자` 명령어를 통해 곡을 선택해주세요**");

            int i = 1;
            for (YoutubeVideo vid : vids) {
                builder.append("\n**")
                        .append(String.valueOf(i))
                        .append(":** ")
                        .append(vid.getName())
                        .append(" (")
                        .append(vid.getDurationFormatted())
                        .append(")");
                i++;
            }

            outMsg.editMessage(builder.build().getRawContent()).queue();

            player.setCurrentTC(channel);

            player.selections.put(invoker.getUser().getId(), new VideoSelection(vids, outMsg));
        }
    }

}

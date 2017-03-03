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
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.TextChannel;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CenaCommand extends Command implements IMusicCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CenaCommand.class);

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

        String streamUrl = "https://youtu.be/cW7n6GpCsPM";

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

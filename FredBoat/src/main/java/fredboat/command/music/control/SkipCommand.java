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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

public class SkipCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (player.isQueueEmpty()) {
            channel.sendMessage("재생 큐가 비어있습니다.").queue();
        }

        if(args.length == 1){
            skipNext(guild, channel, invoker, message, args);
        } else if (args.length == 2 && StringUtils.isNumeric(args[1])) {
            int givenIndex = Integer.parseInt(args[1]);

            if(givenIndex == 1){
                skipNext(guild, channel, invoker, message, args);
                return;
            }

            if(player.getRemainingTracks().size() < givenIndex){
                channel.sendMessage("" + givenIndex + "번 트랙을 스킵할수 없습니다. 현재 재생 큐에 " + player.getRemainingTracks().size() + " 트랙 밖에 없습니다.").queue();
                return;
            } else if (givenIndex < 1){
                channel.sendMessage("스킵 선택 번호는 0 보다는 커야합니다.").queue();
                return;
            }

            AudioTrackContext atc = player.getAudioTrackProvider().removeAt(givenIndex - 2);
            channel.sendMessage("트랙번호 #" + givenIndex + " 가 스킵되었습니다. \n**" + atc.getTrack().getInfo().title + "**").queue();
        } else {
            channel.sendMessage("잘못된 명령입니다.\n사용방법 : ```\n;;skip\n;;skip <트랙번호>```").queue();
        }
    }

    private void skipNext(Guild guild, TextChannel channel, Member invoker, Message message, String[] args){
        GuildPlayer player = PlayerRegistry.get(guild);
        AudioTrackContext atc = player.getPlayingTrack();
        player.skip();
        if(atc == null) {
            channel.sendMessage("스킵할 트랙을 찾을수 없습니다.").queue();
        } else {
            channel.sendMessage("트랙번호 #1 가 스킵되었습니다.\n**" + atc.getTrack().getInfo().title + "**").queue();
        }
    }

}

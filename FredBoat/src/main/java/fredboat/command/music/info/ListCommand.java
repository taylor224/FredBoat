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

package fredboat.command.music.info;

import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.LoggerFactory;

public class ListCommand extends Command implements IMusicCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ListCommand.class);

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (!player.isQueueEmpty()) {
            MessageBuilder mb = new MessageBuilder();

            int numberLength = 2;
            /*if(player.isShuffle()) {
                numberLength = Integer.toString(player.getSongCount()).length();
                numberLength = Math.max(2, numberLength);
            } else {
                numberLength = 2;
            }*/

            int i = 0;

            if(player.isShuffle()){
                mb.append("셔플(랜덤) 기능이 적용된 재생 큐 입니다.\n\n");
            }

            for (AudioTrackContext atc : player.getRemainingTracksOrdered()) {
                if (i == 0) {
                    String status = player.isPlaying() ? " :arrow_forward: " : " :pause_button: "; //Escaped play and pause emojis
                    mb.append("[" +
                            forceNDigits(i + 1, numberLength)
                            + "]", MessageBuilder.Formatting.BOLD)
                            .append(status)
                            .append(atc.getTrack().getInfo().title)
                            .append("\n");
                } else {
                    mb.append("[" +
                            forceNDigits(i + 1, numberLength)
                            + "]", MessageBuilder.Formatting.BOLD)
                            .append(" " + atc.getTrack().getInfo().title)
                            .append("\n");
                    if (i == 10) {
                        break;
                    }
                }
                i++;
            }

            //Now add a timestamp for how much is remaining
            long t = player.getTotalRemainingMusicTimeSeconds();
            String timestamp = TextUtils.formatTime(t * 1000L);

            int tracks = player.getRemainingTracks().size() - player.getLiveTracks().size();
            int streams = player.getLiveTracks().size();

            String desc;

            if (tracks == 0) {
                //We are only listening to streams
                desc = "총 **" + streams +
                        "** 개의 라이브가 재생 큐에 있습니다.";
            } else {

                desc = "총 **" + tracks
                        + "** 개, 총 시간 **[" + timestamp + "]** 의 트랙이"
                        + (streams == 0 ? "" : ", **" + streams + "** 개의 라이브가") + " 재생 큐에 있습니다.";

            }
            
            mb.append("\n" + desc);

            channel.sendMessage(mb.build()).queue();
        } else {
            channel.sendMessage("재생 큐가 비어있습니다.").queue();
        }
    }

    private String forceNDigits(int i, int n) {
        String str = Integer.toString(i);

        while (str.length() < n) {
            str = "0" + str;
        }

        return str;
    }

}

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

package fredboat.command.music.control;

import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.text.MessageFormat;
import java.util.List;

public class StopCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        List<AudioTrackContext> tracks = player.getRemainingTracks();

        Pair<Boolean, String> pair = player.skipTracksForMemberPerms(channel, invoker, tracks);

        if(pair.getLeft()) {
            switch (tracks.size()) {
                case 0:
                    channel.sendMessage(I18n.get(guild).getString("stopAlreadyEmpty")).queue();
                    break;
                case 1:
                    channel.sendMessage(I18n.get(guild).getString("stopEmptyOne")).queue();
                    break;
                default:
                    channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("stopEmptySeveral"), tracks.size())).queue();
                    break;
            }
            player.leaveVoiceChannelRequest(channel, true);
        }
    }

}

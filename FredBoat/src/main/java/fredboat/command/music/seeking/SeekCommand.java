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

package fredboat.command.music.seeking;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.Config;
import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;import java.text.MessageFormat;

public class SeekCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.getExisting(guild);

        if(player == null || player.isQueueEmpty()) {
            TextUtils.replyWithName(channel, invoker, I18n.get(guild).getString("queueEmpty"));
            return;
        }

        if(args.length == 1) {
            TextUtils.replyWithName(channel, invoker, I18n.get(guild).getString("seekUsage").replace(Config.DEFAULT_PREFIX, Config.CONFIG.getPrefix()));
            return;
        }

        long t;
        try {
            t = TextUtils.parseTimeString(args[1]);
        } catch (IllegalStateException e){
            TextUtils.replyWithName(channel, invoker, I18n.get(guild).getString("seekUsage"));
            return;
        }

        AudioTrackContext atc = player.getPlayingTrack();
        AudioTrack at = atc.getTrack();

        //Ensure bounds
        t = Math.max(0, t);
        t = Math.min(atc.getEffectiveDuration(), t);

        at.setPosition(atc.getStartPosition() + t);
        channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("seekSuccess"), atc.getEffectiveTitle(), TextUtils.formatTime(t))).queue();
    }

}

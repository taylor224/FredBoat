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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.VideoSelection;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.text.MessageFormat;

public class SelectCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        select(guild, channel, invoker, message, args);
    }

    static void select(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (player.selections.containsKey(invoker.getUser().getId())) {
            VideoSelection selection = player.selections.get(invoker.getUser().getId());
            try {
                int i = Integer.valueOf(args[1]);
                if (selection.getChoices().size() < i || i < 1) {
                    throw new NumberFormatException();
                } else {
                    AudioTrack selected = selection.getChoices().get(i - 1);
                    player.selections.remove(invoker.getUser().getId());
                    String msg = MessageFormat.format(I18n.get(guild).getString("selectSuccess"), i, selected.getInfo().title, TextUtils.formatTime(selected.getInfo().length));
                    selection.getOutMsg().editMessage(msg).complete(true);
                    player.queue(new AudioTrackContext(selected, invoker));
                    player.setPause(false);
                    try {
                        message.delete().queue();
                    } catch (PermissionException ignored) {

                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("selectInterval"), selection.getChoices().size())).queue();
            } catch (RateLimitedException e) {
                throw new RuntimeException(e);
            }
        } else {
            channel.sendMessage(I18n.get(guild).getString("selectSelectionNotGiven")).queue();
        }
    }

}

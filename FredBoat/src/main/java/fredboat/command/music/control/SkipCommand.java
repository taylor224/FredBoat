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

import fredboat.Config;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkipCommand extends Command implements IMusicCommand {
    private static final String TRACK_RANGE_REGEX = "^(0?\\d+)-(0?\\d+)$";
    private static final Pattern trackRangePattern = Pattern.compile(TRACK_RANGE_REGEX);

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (player.isQueueEmpty()) {
            channel.sendMessage(I18n.get(guild).getString("skipEmpty")).queue();
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
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("skipOutOfBounds"), givenIndex, player.getRemainingTracks().size())).queue();
                return;
            } else if (givenIndex < 1){
                channel.sendMessage(I18n.get(guild).getString("skipNumberTooLow")).queue();
                return;
            }

            AudioTrackContext atc = player.getAudioTrackProvider().getAsListOrdered().get(givenIndex - 2);
            player.skipTracksForMemberPerms(channel, invoker, atc);

            Pair<Boolean, String> result = player.skipTracksForMemberPerms(channel, invoker, atc);
            if(result.getLeft()) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("skipSuccess"), givenIndex, atc.getEffectiveTitle())).queue();
            }
        } else if (args.length == 2 && trackRangePattern.matcher(args[1]).matches()){
            Matcher trackMatch = trackRangePattern.matcher(args[1]);

            int startTrackIndex = Integer.parseInt(trackMatch.group(1));
            int endTrackIndex = Integer.parseInt(trackMatch.group(2));

            if (startTrackIndex < 1) {
                channel.sendMessage(I18n.get(guild).getString("skipNumberTooLow")).queue();
                return;
            } else if (endTrackIndex <= startTrackIndex) {
                channel.sendMessage(I18n.get(guild).getString("skipRangeInvalid")).queue();
                return;
            } else if (player.getRemainingTracks().size() < endTrackIndex) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("skipOutOfBounds"), endTrackIndex, player.getRemainingTracks().size())).queue();
                return;
            }

            List<AudioTrackContext> tracks = new ArrayList<>();
            if (startTrackIndex == 1) {
                //Add the currently playing track
                tracks.add(player.getPlayingTrack());
            }
            tracks.addAll(player.getAudioTrackProvider().getInRange(startTrackIndex - 1, endTrackIndex - 2));

            Pair<Boolean, String> pair = player.skipTracksForMemberPerms(channel, invoker, tracks);

            if(pair.getLeft()) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("skipRangeSuccess"), startTrackIndex, endTrackIndex)).queue();
            }
        } else {
            channel.sendMessage(I18n.get(guild).getString("skipInvalidArgCount").replace(Config.DEFAULT_PREFIX, Config.CONFIG.getPrefix())).queue();
        }
    }

    private void skipNext(Guild guild, TextChannel channel, Member invoker, Message message, String[] args){
        GuildPlayer player = PlayerRegistry.get(guild);
        AudioTrackContext atc = player.getPlayingTrack();
        if(atc == null) {
            channel.sendMessage(I18n.get(guild).getString("skipTrackNotFound")).queue();
        } else {
            Pair<Boolean, String> result = player.skipTracksForMemberPerms(channel, invoker, atc);
            if(result.getLeft()) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("skipSuccess"), 1, atc.getEffectiveTitle())).queue();
            }
        }
    }

}

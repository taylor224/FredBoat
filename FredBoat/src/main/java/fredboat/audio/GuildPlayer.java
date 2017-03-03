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

package fredboat.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.queue.AudioLoader;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.IdentifierContext;
import fredboat.audio.queue.SimpleTrackProvider;
import fredboat.commandmeta.MessagingException;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.MemberImpl;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildPlayer extends AbstractPlayer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    public final JDA jda;
    final String guildId;
    public final Map<String, VideoSelection> selections = new HashMap<>();
    private TextChannel currentTC;

    private final AudioLoader audioLoader;

    @SuppressWarnings("LeakingThisInConstructor")
    public GuildPlayer(JDA jda, Guild guild) {
        this.jda = jda;
        this.guildId = guild.getId();

        AudioManager manager = guild.getAudioManager();
        manager.setSendingHandler(this);
        audioTrackProvider = new SimpleTrackProvider();
        audioLoader = new AudioLoader(audioTrackProvider, getPlayerManager(), this);
        player.addListener(new PlayerEventListener(this));
    }

    public void joinChannel(Member usr) throws MessagingException {
        VoiceChannel targetChannel = getUserCurrentVoiceChannel(usr);
        joinChannel(targetChannel);
    }

    public void joinChannel(VoiceChannel targetChannel) throws MessagingException {
        if (targetChannel == null) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerUserNotInChannel"));
        }

        if (!PermissionUtil.checkPermission(targetChannel, targetChannel.getGuild().getSelfMember(), Permission.VOICE_CONNECT)
                && !targetChannel.getMembers().contains(getGuild().getSelfMember())) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinConnectDenied"));
        }

        if (!PermissionUtil.checkPermission(targetChannel, targetChannel.getGuild().getSelfMember(), Permission.VOICE_SPEAK)) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinSpeakDenied"));
        }

        AudioManager manager = getGuild().getAudioManager();

        manager.openAudioConnection(targetChannel);

        log.info("Connected to voice channel " + targetChannel);
    }

    public void leaveVoiceChannelRequest(TextChannel channel, boolean silent) {
        AudioManager manager = getGuild().getAudioManager();
        if (!silent) {
            if (manager.getConnectedChannel() == null) {
                channel.sendMessage(I18n.get(getGuild()).getString("playerNotInChannel")).queue();
            } else {
                channel.sendMessage(MessageFormat.format(I18n.get(getGuild()).getString("playerLeftChannel"), getChannel().getName())).queue();
            }
        }
        manager.closeAudioConnection();
    }

    public VoiceChannel getUserCurrentVoiceChannel(User usr) {
        return getUserCurrentVoiceChannel(new MemberImpl(getGuild(), usr));
    }

    public VoiceChannel getUserCurrentVoiceChannel(Member member) {
        for (VoiceChannel chn : getGuild().getVoiceChannels()) {
            for (Member memberInChannel : chn.getMembers()) {
                if (member.getUser().getId().equals(memberInChannel.getUser().getId())) {
                    return chn;
                }
            }
        }
        return null;
    }

    public void queue(String identifier, TextChannel channel) {
        queue(identifier, channel, null);
    }

    public void queue(String identifier, TextChannel channel, Member invoker) {
        IdentifierContext ic = new IdentifierContext(identifier, channel, invoker);

        if (invoker != null) {
            joinChannel(invoker);
        }

        audioLoader.loadAsync(ic);
    }

    public void queue(IdentifierContext ic) {
        if (ic.member != null) {
            joinChannel(ic.member);
        }

        audioLoader.loadAsync(ic);
    }

    public void queue(AudioTrackContext atc){
        if(atc.getMember() != null) {
            joinChannel(atc.getMember());
        }
        audioTrackProvider.add(atc);
        play();
    }

    public int getSongCount() {
        return getRemainingTracks().size();
    }

    public long getTotalRemainingMusicTimeSeconds() {
        //Live streams are considered to have a length of 0
        long millis = 0;
        for (AudioTrackContext atc : getQueuedTracks()) {
            if (!atc.getTrack().getInfo().isStream) {
                millis += atc.getEffectiveDuration();
            }
        }

        AudioTrackContext atc = getPlayingTrack();
        if (atc != null && !atc.getTrack().getInfo().isStream) {
            millis += Math.max(0, atc.getEffectiveDuration() - atc.getEffectivePosition());
        }

        return millis / 1000;
    }
    
    public List<AudioTrack> getLiveTracks() {
        ArrayList<AudioTrack> l = new ArrayList<>();
        
        for(AudioTrackContext atc : getRemainingTracks()){
            if(atc.getTrack().getInfo().isStream){
                l.add(atc.getTrack());
            }
        }
        
        return l;
    }

    public VoiceChannel getChannel() {
        return getUserCurrentVoiceChannel(getGuild().getSelfMember());
    }

    public TextChannel getActiveTextChannel() {
        if (currentTC != null) {
            return currentTC;
        } else {
            log.warn("No currentTC in " + getGuild() + "! Returning public channel...");
            return getGuild().getPublicChannel();
        }

    }

    /**
     * @return Users who are not bots
     */
    public List<Member> getUsersInVC() {
        VoiceChannel vc = getChannel();
        if (vc == null) {
            return new ArrayList<>();
        }

        List<Member> members = vc.getMembers();
        ArrayList<Member> nonBots = new ArrayList<>();
        for (Member member : members) {
            if (!member.getUser().isBot()) {
                nonBots.add(member);
            }
        }
        return nonBots;
    }

    @Override
    public String toString() {
        return "[GP:" + getGuild().getId() + "]";
    }

    public Guild getGuild() {
        return jda.getGuildById(guildId);
    }

    public boolean isRepeat() {
        return audioTrackProvider instanceof SimpleTrackProvider && ((SimpleTrackProvider) audioTrackProvider).isRepeat();
    }

    public boolean isShuffle() {
        return audioTrackProvider instanceof SimpleTrackProvider && ((SimpleTrackProvider) audioTrackProvider).isShuffle();
    }

    public void setRepeat(boolean repeat) {
        if (audioTrackProvider instanceof SimpleTrackProvider) {
            ((SimpleTrackProvider) audioTrackProvider).setRepeat(repeat);
        } else {
            throw new UnsupportedOperationException("Can't repeat or shuffle " + audioTrackProvider.getClass());
        }
    }

    public void setShuffle(boolean shuffle) {
        if (audioTrackProvider instanceof SimpleTrackProvider) {
            ((SimpleTrackProvider) audioTrackProvider).setShuffle(shuffle);
        } else {
            throw new UnsupportedOperationException("Can't repeat or shuffle " + audioTrackProvider.getClass());
        }
    }

    public void setCurrentTC(TextChannel currentTC) {
        this.currentTC = currentTC;
    }

    public TextChannel getCurrentTC() {
        return currentTC;
    }

    //Success, fail message
    private Pair<Boolean, String> canMemberSkipTracks(Member member, List<AudioTrackContext> list) {
        if(PermissionUtil.checkPermission(getGuild(), member, Permission.MESSAGE_MANAGE)){
            return new ImmutablePair<>(true, null);
        } else {
            //We are not a mod
            int otherPeoplesTracks = 0;

            for (AudioTrackContext atc : list) {
                if(!atc.getMember().equals(member)) otherPeoplesTracks++;
            }

            if (otherPeoplesTracks > 1) {
                return new ImmutablePair<>(false, I18n.get(getGuild()).getString("skipDeniedTooManyTracks"));
            } else {
                return new ImmutablePair<>(true, null);
            }
        }
    }

    public Pair<Boolean, String> skipTracksForMemberPerms(TextChannel channel, Member member, AudioTrackContext atc) {
        List<AudioTrackContext> list = new ArrayList<>();
        list.add(atc);
        return skipTracksForMemberPerms(channel, member, list);
    }

    public Pair<Boolean, String> skipTracksForMemberPerms(TextChannel channel, Member member, List<AudioTrackContext> list) {
        Pair<Boolean, String> pair = canMemberSkipTracks(member, list);

        if (pair.getLeft()) {
            skipTracks(list);
        } else {
            TextUtils.replyWithName(channel, member, pair.getRight());
        }

        return pair;
    }

    private void skipTracks(List<AudioTrackContext> list) {
        boolean skipCurrentTrack = false;

        for (AudioTrackContext atc : list) {
            if(atc.equals(getPlayingTrack())){
                //Should be skipped last, in respect to PlayerEventListener
                skipCurrentTrack = true;
            } else {
                skipTrack(atc);
            }
        }

        if(skipCurrentTrack) {
            skip();
        }
    }

    private void skipTrack(AudioTrackContext atc) {
        if(getPlayingTrack().equals(atc)) {
            skip();
        } else {
            audioTrackProvider.remove(atc);
        }
    }

}

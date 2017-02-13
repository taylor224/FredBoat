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
package fredboat.event;

import fredboat.FredBoat;
import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.command.fun.TalkCommand;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.util.BotConstants;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.regex.Matcher;

public class EventListenerBoat extends AbstractScopedEventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListenerBoat.class);

    public static HashMap<String, Message> messagesToDeleteIfIdDeleted = new HashMap<>();
    public User lastUserToReceiveHelp;

    public static int messagesReceived = 0;

    public EventListenerBoat(int scope, String defaultPrefix) {
        super(scope, defaultPrefix);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        /*log.info(event.getJDA().getSelfInfo().getUsername());
        log.info(event);
        log.info(event.getAuthor());
        log.info(event.getAuthor().getId());*/

        messagesReceived++;

        if (event.getPrivateChannel() != null) {
            log.info("PRIVATE" + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            return;
        }

        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            return;
        }

        if (event.getMessage().getContent().length() < defaultPrefix.length()) {
            return;
        }

        if (event.getMessage().getContent().substring(0, defaultPrefix.length()).equals(defaultPrefix)) {
            Command invoked = null;
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            Matcher matcher = COMMAND_NAME_PREFIX.matcher(event.getMessage().getContent());

            if(matcher.find()) {
                String cmdName = matcher.group();
                CommandRegistry.CommandEntry entry = CommandRegistry.getCommandFromScope(scope, cmdName);
                if(entry != null) {
                    invoked = entry.command;
                } else {
                    log.info("알수없는 명령어 입니다 :", cmdName);
                }
            }

            if (invoked == null) {
                return;
            }

            CommandManager.prefixCalled(invoked, event.getGuild(), event.getTextChannel(), event.getMember(), event.getMessage());
        } else if (event.getMessage().getRawContent().startsWith("<@" + event.getJDA().getSelfUser().getId() + ">")) {
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            CommandManager.commandsExecuted++;
            TalkCommand.talk(event.getMember(), event.getTextChannel(), event.getMessage().getRawContent().substring(event.getJDA().getSelfUser().getAsMention().length() + 1));
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (messagesToDeleteIfIdDeleted.containsKey(event.getMessageId())) {
            Message msg = messagesToDeleteIfIdDeleted.remove(event.getMessageId());
            if (msg.getJDA() == event.getJDA()) {
                msg.deleteMessage().queue();
            }
        }
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor() == lastUserToReceiveHelp) {
            //Ignore, they just got help! Stops any bot chain reactions
            return;
        }

        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            //Don't reply to ourselves
            return;
        }

        event.getChannel().sendMessage(BotConstants.HELP_TEXT).queue();
        lastUserToReceiveHelp = event.getAuthor();
    }

    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
        event.getJDA().getPresence().setGame(Game.of("[" + FredBoat.getInstance(event.getJDA()).getShardInfo().getShardId() + "] 유루유리"));
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        event.getJDA().getPresence().setGame(Game.of("[" + FredBoat.getInstance(event.getJDA()).getShardInfo().getShardId() + "] 유루유리"));
    }

    /* music related */
    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        GuildPlayer player = PlayerRegistry.getExisting(event.getGuild());

        if (player == null) {
            return;
        }

        if (player.getUsersInVC().isEmpty()
                && player.getUserCurrentVoiceChannel(event.getGuild().getSelfMember()) == event.getChannelLeft()
                && !player.isPaused()) {
            player.pause();
            player.getActiveTextChannel().sendMessage("모든 유저가 음성채널에서 떠났습니다. 플레이어를 일시정지 할겁니다. (삐짐)").queue();
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        GuildPlayer player = PlayerRegistry.getExisting(event.getGuild());

        if (player == null) {
            return;
        }

        if (!player.getUsersInVC().isEmpty()
                && player.getUserCurrentVoiceChannel(event.getGuild().getSelfMember()) == event.getChannelJoined()
                && player.isPaused()) {
            player.setPause(false);
            player.getActiveTextChannel().sendMessage("유저가 들어왔다 메우 일한다 메우! (활짝)").queue();
        }
    }


    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        PlayerRegistry.destroyPlayer(event.getGuild());
    }

}

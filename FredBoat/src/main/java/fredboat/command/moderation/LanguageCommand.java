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

package fredboat.command.moderation;

import fredboat.Config;
import fredboat.commandmeta.abs.Command;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.text.MessageFormat;import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LanguageCommand extends Command {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if(args.length != 2) {
            handleNoArgs(guild, channel, invoker, message, args);
            return;
        }

        //Assume proper usage and that we are about to set a new language
        try {
            I18n.set(guild, args[1]);
        } catch (I18n.LanguageNotSupportedException e) {
            TextUtils.replyWithName(channel, invoker, MessageFormat.format(I18n.get(guild).getString("langInvalidCode"), args[1]));
            return;
        }

        TextUtils.replyWithName(channel, invoker, MessageFormat.format(I18n.get(guild).getString("langSuccess"), I18n.getLocale(guild).getNativeName()));
    }

    private void handleNoArgs(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        MessageBuilder mb = new MessageBuilder()
                .append(I18n.get(guild).getString("langInfo").replace(Config.DEFAULT_PREFIX, Config.CONFIG.getPrefix()))
                .append("\n\n");

        List<String> keys = new ArrayList<>(I18n.LANGS.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            I18n.FredBoatLocale loc = I18n.LANGS.get(key);
            mb.append("__`" + loc.getCode() + "`__ - " + loc.getNativeName());
            mb.append("\n");
        }

        mb.append("\n");
        mb.append(I18n.get(guild).getString("langDisclaimer"));

        channel.sendMessage(mb.build()).queue();
    }
}

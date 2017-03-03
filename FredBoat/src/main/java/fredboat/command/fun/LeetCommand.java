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

package fredboat.command.fun;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommand;
import fredboat.event.EventListenerBoat;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author frederik
 */
public class LeetCommand extends Command implements ICommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        String res = "";
        channel.sendTyping().queue();

        if(args.length < 2) {
            channel.sendMessage("Proper usage: " + Config.CONFIG.getPrefix() + "leet <text>").queue();
            return;
        }

        for (int i = 1; i < args.length; i++) {
            res = res+" "+args[i];
        }
        res = res.substring(1);
        try {
            res = Unirest.get("https://montanaflynn-l33t-sp34k.p.mashape.com/encode?text=" + URLEncoder.encode(res, "UTF-8").replace("+", "%20")).header("X-Mashape-Key", Config.CONFIG.getMashapeKey()).asString().getBody();
        } catch (UnirestException ex) {
            Message myMsg = TextUtils.replyWithName(channel, invoker, " Could not connect to API! "+ex.getMessage());
            return;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        Message myMsg = null;
        try {
            myMsg = channel.sendMessage(res).complete(true);
        } catch (RateLimitedException e) {
            throw new RuntimeException(e);
        }

        EventListenerBoat.messagesToDeleteIfIdDeleted.put(message.getId(), myMsg);
    }
    
}

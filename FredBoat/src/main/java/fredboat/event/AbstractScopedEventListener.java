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

package fredboat.event;

import fredboat.FredBoat;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class AbstractScopedEventListener extends ListenerAdapter {

    public final int scope;
    static final Pattern COMMAND_NAME_PREFIX = Pattern.compile("(\\w+)");
    private final HashMap<String, UserListener> userListener = new HashMap<>();

    public static int messagesReceived = 0;

    AbstractScopedEventListener(int scope) {
        this.scope = scope;
    }

    @Override
    public void onReady(ReadyEvent event) {
        FredBoat.onInit(event);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        UserListener listener = userListener.get(event.getAuthor().getId());
        if (listener != null) {
            try{
            listener.onGuildMessageReceived(event);
            } catch(Exception ex){
                TextUtils.handleException(ex, event.getChannel(), event.getMember());
            }
        }
    }

    public void putListener(String id, UserListener listener) {
        userListener.put(id, listener);
    }

    public void removeListener(String id) {
        userListener.remove(id);
    }
}

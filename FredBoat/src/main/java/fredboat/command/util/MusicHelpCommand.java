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

package fredboat.command.util;

import fredboat.command.fun.TextCommand;

public class MusicHelpCommand extends TextCommand {

    public static final String MUSIC
            ="```md\n" +
            "< 아카리 음악 명령어 >\n" +
            ";;play <url>\n" +
            "#해당 url 의 음악을 재생합니다.\n" +
            ";;list\n" +
            "#현재 재생 큐를 보여줍니다.\n" +
            ";;nowplaying / " +
            ";;np\n" +
            "#현재 재생중인 곡의 정보를 보여줍니다.\n" +
            ";;skip / ;;skip [n]\n" +
            "#현재 재생중인 곡 혹은 해당 리스트 트랙 번호의 곡을 스킵합니다.\n" +
            ";;stop\n" +
            "#재생을 멈추고 재생 큐를 초기화 합니다.\n" +
            ";;pause\n" +
            "#플레이어를 일시정지 합니다.\n" +
            ";;unpause\n" +
            "#플레이어 일시정지를 해제합니다..\n" +
            ";;join\n" +
            "#현재 접속된 음성채널로 봇을 초대합니다.\n" +
            ";;leave\n" +
            "#봇을 쫒아냅니다.\n" +
            ";;repeat\n" +
            "#현재 재생 곡을 반복재생 합니다.\n" +
            ";;shuffle\n" +
            "#셔플(랜덤) 재생 모드를 활성화 합니다.\n" +
            ";;volume <vol>\n" +
            "#볼륨을 조절합니다. 0 ~ 150 사이. 기본 볼륨은 100 입니다.\n" +
            ";;export\n" +
            "#현재 재생 큐 목록을 hastebin 으로 출력합니다.\n" +
            ";;gr\n" +
            "#gensokyoradio.net 를 위한 특별 embed 를 포스트 합니다.```";

    public MusicHelpCommand() {
        super(MUSIC);
    }
}

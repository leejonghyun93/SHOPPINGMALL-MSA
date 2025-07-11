package org.kosa.livestreamingservice.util;

import java.util.List;
import java.util.regex.Pattern;

public class ChatFilterUtil {

    // 욕설 목록은 확장 가능
    private static final List<String> BAD_WORDS = List.of(
            "fuck", "shit", "병신", "개새끼", "좆", "지랄" // 실제 환경에 맞게 관리
    );

    public static String filterBadWords(String input) {
        if (input == null) return null;

        String result = input;
        for (String badWord : BAD_WORDS) {
            Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(badWord));
            result = pattern.matcher(result).replaceAll("★".repeat(badWord.length()));
        }

        return result;
    }

}
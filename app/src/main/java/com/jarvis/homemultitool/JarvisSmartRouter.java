package com.jarvis.homemultitool;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Local natural-language layer. It converts varied Russian phrasing into a
 * small set of semantic intent markers before the action engine runs.
 * It is intentionally deterministic, offline-safe and learnable through
 * JarvisMemory; it is not presented as a replacement for a large language model.
 */
public final class JarvisSmartRouter {
    private JarvisSmartRouter() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(new Locale("ru"));
        s = s.replace('ё','е').replaceAll("\\s+"," ").trim();
        // Conversational filler: preserve the semantic payload.
        s = s.replaceAll("\\b(пожалуйста|пожалуйстааа|можешь|можно|давай|давай-ка|будь добр|будь добра|если не сложно)\\b", " ");
        s = s.replaceAll("\\s+", " ").trim();
        // Media paraphrases.
        s = s.replaceAll("\\b(поставь на паузу|поставить на паузу|приостанови|останови музыку|останови воспроизведение|замри)\\b", "пауза");
        s = s.replaceAll("\\b(возобнови|возобновляй|продолжай|продолжи|включи обратно|сними с паузы|снова играй)\\b", "продолжай");
        s = s.replaceAll("\\b(поставь|поставить|вруби|включай|запусти|запускай|проиграй|проигрывай|сыграй)\\b", "включи");
        s = s.replaceAll("\\b(следующий трек|следующую песню|следующую композицию|переключи песню|переключи трек)\\b", "дальше");
        s = s.replaceAll("\\b(предыдущий трек|предыдущую песню|предыдущую композицию|верни предыдущую)\\b", "назад");
        s = s.replaceAll("\\b(возобнови воспроизведение|возобнови проигрывание|снова включи|продолжи проигрывание|продолжи воспроизведение)\\b", "продолжай");
        s = s.replaceAll("\\b(следующую|следующий|переключись на следующий|вперед|вперёд|перемотай вперед)\\b", "дальше");
        s = s.replaceAll("\\b(предыдущую|предыдущий|переключись на предыдущий|влево|назад к прошлой)\\b", "назад");
        s = s.replaceAll("\\b(приглуши|сделай потише|уменьши звук|убавь звук)\\b", "тише");
        s = s.replaceAll("\\b(прибавь звук|сделай погромче|увеличь звук|повысь громкость)\\b", "громче");
        // Music collections and categories.
        s = s.replaceAll("\\b(понравившиеся|понравившуюся|лайкнутые|лайкнутую|сохраненные|сохраненное|избранные|избранное|любимые|любимую|моя музыка|мои песни)\\b", "любимое");
        s = s.replaceAll("\\b(рандомно|случайно|в случайном порядке|перемешай|перемешать)\\b", "вперемешку");
        // Common wake-name transcription variants.
        s = s.replaceAll("\\b(жарвис|дарвис|джавиз|джавис)\\b", "джарвис");
        return s.replaceAll("\\s+", " ").trim();
    }

    public static boolean isMediaControl(String s) {
        return s.matches(".*\\b(пауза|продолжай|дальше|назад|следующ(ий|ую)|предыдущ(ий|ую)|включи обратно|останови)\\b.*");
    }

    public static boolean isMusicRequest(String s) {
        return s.contains("музык") || s.contains("песн") || s.contains("трек") || s.contains("плейлист") || s.contains("любимое") || s.contains("вперемешку");
    }

    public static boolean isSettingsRequest(String s) {
        return Pattern.compile("(настройк|параметр|разрешени|доступ)").matcher(s).find();
    }
}

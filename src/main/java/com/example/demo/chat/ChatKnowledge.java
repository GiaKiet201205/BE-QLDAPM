package com.example.demo.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

@Component
public class ChatKnowledge {
    public static final Set<String> ROLES = Set.of("ADMIN", "TC", "CM", "TEACHER", "SALE", "CS");
    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "minh", "ban", "lam", "nao", "nhu", "the", "la", "va", "co", "khong", "duoc",
            "cho", "cua", "de", "voi", "gi", "mot", "nhung", "can", "muon", "hay", "huong", "dan", "cach", "xin", "chao");

    public record Guide(String id, String title, List<String> roles, Map<String, String> paths,
                        String question, String keywords, String content) {
        public ChatDtos.Source source(String role) {
            return new ChatDtos.Source(id, title, "/" + role.toLowerCase(Locale.ROOT) + paths.get(role));
        }
    }

    private final List<Guide> guides;

    public ChatKnowledge(ObjectMapper mapper) throws IOException {
        try (var stream = new ClassPathResource("knowledge/guides.json").getInputStream()) {
            guides = List.copyOf(mapper.readValue(stream, new TypeReference<List<Guide>>() {}));
        }
        Set<String> ids = new HashSet<>();
        for (Guide guide : guides) {
            if (!ids.add(guide.id()) || !ROLES.containsAll(guide.roles())
                    || !guide.paths().keySet().equals(new HashSet<>(guide.roles()))
                    || guide.paths().values().stream().anyMatch(p -> !p.startsWith("/") || p.startsWith("//") || p.contains(".."))) {
                throw new IllegalStateException("Invalid chat knowledge: " + guide.id());
            }
        }
    }

    public List<Guide> available(String role) {
        return guides.stream().filter(g -> g.roles().contains(role)).toList();
    }

    public List<Guide> search(String role, String query) {
        Set<String> terms = terms(expand(query));
        return available(role).stream()
                .map(g -> Map.entry(g, score(g, terms)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Guide, Integer>comparingByValue().reversed())
                .limit(4).map(Map.Entry::getKey).toList();
    }

    boolean outsideRole(String role, String query) {
        if ("ADMIN".equals(role)) return false;
        Set<String> queryTerms = terms(expand(query));
        int allowed = available(role).stream().mapToInt(g -> score(g, queryTerms)).max().orElse(0);
        int forbidden = guides.stream().filter(g -> !g.roles().contains(role))
                .mapToInt(g -> score(g, queryTerms)).max().orElse(0);
        return forbidden > allowed;
    }

    private int score(Guide guide, Set<String> query) {
        Set<String> title = terms(expand(guide.title() + " " + guide.question() + " " + guide.keywords()));
        Set<String> body = terms(expand(guide.content()));
        int weighted = query.stream().mapToInt(t -> title.contains(t) ? 5 : body.contains(t) ? 1 : 0).sum();
        // Prefer documents that cover the full question, rather than one generic shared word.
        return weighted;
    }

    private static String expand(String text) {
        String q = normalize(text);
        for (var synonyms : List.of(
                List.of("sinh vien", "hoc sinh", "hoc vien", "student"),
                List.of("giang vien", "giao vien", "teacher"),
                List.of("nhan su", "nhan vien", "employee"),
                List.of("bao cao", "thong ke", "report"),
                List.of("cham cong", "diem danh", "attendance"),
                List.of("hoc phi", "thanh toan", "payment"),
                List.of("dang nhap", "login"),
                List.of("don mua", "don hang", "order"))) {
            if (synonyms.stream().anyMatch(s -> (" " + normalize(text) + " ").contains(" " + s + " "))) q += " " + String.join(" ", synonyms);
        }
        return q;
    }

    static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    private static Set<String> terms(String text) {
        Set<String> result = new HashSet<>(Arrays.asList(normalize(text).split("[^a-z0-9]+")));
        result.removeAll(STOP_WORDS);
        result.removeIf(word -> word.length() < 2);
        return result;
    }
}

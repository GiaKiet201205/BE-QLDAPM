package com.example.demo.chat;

import com.example.demo.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/** Public website instructions only. No admin knowledge, DB tools, customer records or provider calls. */
@Service
public class StorefrontChatService {
    static final Set<String> PATHS = Set.of("/courses", "/schedule", "/cart", "/user/login", "/user/register",
            "/user/profile", "/user/forgot-password", "/contact", "/about");
    record Guide(String id, String title, String path, String question, List<String> keywords, String content) {}
    private final List<Guide> guides;
    private final Map<String, Window> limits = new HashMap<>();
    private record Window(long start, int count) {}

    public StorefrontChatService(ObjectMapper mapper) throws IOException {
        try (var stream = new ClassPathResource("knowledge/storefront-guides.json").getInputStream()) {
            guides = List.copyOf(mapper.readValue(stream, new TypeReference<List<Guide>>() {}));
        }
        Set<String> ids = new HashSet<>();
        for (var guide : guides) {
            if (!ids.add(guide.id()) || !PATHS.contains(guide.path()) || guide.content().isBlank() || guide.keywords().isEmpty()) {
                throw new IllegalStateException("Invalid public website guide: " + guide.id());
            }
        }
    }

    public ChatDtos.Status status() {
        return new ChatDtos.Status(false, guides.stream().limit(5).map(g -> new ChatDtos.Suggestion(g.question())).toList());
    }

    public ChatDtos.Response reply(String clientAddress, ChatDtos.Request request) {
        limit(clientAddress);
        String query = request.message();
        String normalized = ChatKnowledge.normalize(query);
        if (normalized.matches("(?s).*(buoc tiep|sau do|noi ro|chi tiet|giai thich them|tiep theo).*")) {
            var history = request.history() == null ? List.<ChatDtos.Message>of() : request.history();
            for (int i = history.size() - 1; i >= 0; i--) if (history.get(i).role().equals("user")) {
                query += " " + history.get(i).content();
                break;
            }
        }
        if (restricted(query)) return response("Mình chỉ hướng dẫn sử dụng website dành cho khách hàng. Mình không hỗ trợ chức năng quản trị, thống kê nội bộ, dữ liệu người khác hoặc thông tin bảo mật. Bạn có thể hỏi cách tìm khóa, xem lịch, đăng ký và thanh toán.", List.of());
        String search = " " + ChatKnowledge.normalize(query).replaceAll("[^a-z0-9]+", " ").strip() + " ";
        var matches = guides.stream().map(g -> Map.entry(g, score(g, search)))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Guide, Integer>comparingByValue().reversed()).limit(2).toList();
        if (matches.isEmpty()) return response("Mình chưa có hướng dẫn phù hợp. Bạn có thể hỏi: ‘Tôi mới bắt đầu, đăng ký học thế nào?’, ‘Cách chọn lớp’, ‘Giỏ hàng dùng thế nào?’ hoặc ‘Thanh toán chưa cập nhật thì làm sao?’. Nếu cần tư vấn lộ trình hoặc chính sách riêng, hãy mở Liên hệ để gửi yêu cầu cho trung tâm.",
                List.of(new ChatDtos.Source("contact", "Liên hệ trung tâm", "/contact")));
        var selected = matches.stream().filter(e -> e.getValue() * 4 >= matches.get(0).getValue() * 3).toList();
        return response(String.join("\n\n", selected.stream().map(e -> e.getKey().title() + "\n" + e.getKey().content()).toList()),
                selected.stream().map(e -> new ChatDtos.Source(e.getKey().id(), e.getKey().title(), e.getKey().path())).toList());
    }

    private static ChatDtos.Response response(String answer, List<ChatDtos.Source> sources) {
        return new ChatDtos.Response(answer, "guide", "Hướng dẫn tự động từ nội dung website; không đọc đơn hàng hoặc thông tin tài khoản của bạn.", sources);
    }

    private static int score(Guide guide, String query) {
        return guide.keywords().stream().map(ChatKnowledge::normalize)
                .filter(term -> query.contains(" " + term + " ")).mapToInt(term -> term.split(" ").length * 3).sum();
    }

    private static boolean restricted(String query) {
        String q = " " + ChatKnowledge.normalize(query).replaceAll("[^a-z0-9]+", " ").strip() + " ";
        return List.of("admin", "quan tri", "phan quyen", "thong ke", "doanh thu", "loi nhuan", "bao cao noi bo",
                "bang luong", "cong luong", "phan cong giao vien", "diem danh giao vien", "quan ly nhan su",
                "api key", "secret", "token", "jwt", "database", "co so du lieu", "sql", "system prompt",
                "mat khau nguoi khac", "tai khoan nguoi khac", "don hang nguoi khac", "bo qua huong dan", "bo qua quyen")
                .stream().anyMatch(term -> q.contains(" " + term + " "));
    }

    private void limit(String clientAddress) {
        long now = Instant.now().getEpochSecond();
        synchronized (limits) {
            limits.entrySet().removeIf(e -> now - e.getValue().start() >= 60);
            String key = Objects.requireNonNullElse(clientAddress, "unknown");
            Window window = limits.get(key);
            if ((window == null && limits.size() >= 10000) || (window != null && window.count() >= 12)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn hỏi hơi nhanh. Vui lòng chờ một chút rồi gửi lại (tối đa 12 câu/phút).");
            }
            limits.put(key, window == null ? new Window(now, 1) : new Window(window.start(), window.count() + 1));
        }
    }
}

package com.example.demo.chat;

import com.example.demo.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;

@Service
public class ChatService {
    private final ChatKnowledge knowledge;
    private final ChatAiClient ai;
    private final ChatDataTools data;
    private final Map<String, Window> limits = new HashMap<>();
    private static class Window {
        long start = Instant.now().getEpochSecond();
        int count;
        boolean busy;
    }

    public ChatService(ChatKnowledge knowledge, ChatAiClient ai) {
        this(knowledge, ai, (ChatDataTools) null);
    }

    @Autowired
    public ChatService(ChatKnowledge knowledge, ChatAiClient ai, ObjectProvider<ChatDataTools> data) {
        this(knowledge, ai, data.getIfAvailable());
    }

    ChatService(ChatKnowledge knowledge, ChatAiClient ai, ChatDataTools data) {
        this.knowledge = knowledge;
        this.ai = ai;
        this.data = data;
    }

    String role(Authentication actor) {
        if (actor == null || !actor.isAuthenticated() || "anonymousUser".equals(actor.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để sử dụng trợ lý.");
        }
        return actor.getAuthorities().stream().map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .filter(ChatKnowledge.ROLES::contains).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Vai trò chưa được hỗ trợ."));
    }

    public ChatDtos.Status status(Authentication actor) {
        String role = role(actor);
        List<ChatDtos.Suggestion> suggestions = new ArrayList<>();
        if (data != null && role.equals("ADMIN")) {
            suggestions.add(new ChatDtos.Suggestion("Tổng quan hôm nay"));
            if (role.equals("ADMIN")) {
                suggestions.add(new ChatDtos.Suggestion("Thống kê tháng này"));
                suggestions.add(new ChatDtos.Suggestion("Đơn hàng chưa thanh toán tháng này"));
            }
        }
        knowledge.available(role).stream().limit(5 - suggestions.size())
                .map(g -> new ChatDtos.Suggestion(g.question())).forEach(suggestions::add);
        return new ChatDtos.Status(role.equals("ADMIN") && ai.enabled(), suggestions);
    }

    public ChatDtos.Response reply(Authentication actor, ChatDtos.Request request) {
        String role = role(actor);
        String user = actor.getName();
        enter(user);
        try {
            String query = request.message();
            // Short follow-up questions can use the preceding user question for retrieval only.
            if (ChatKnowledge.normalize(query).matches(".*(buoc tiep|sau do|noi ro|chi tiet|giai thich them|con thang|con nam|con hom|con tuan|the con).*")) {
                var history = request.history() == null ? List.<ChatDtos.Message>of() : request.history();
                for (int i = history.size() - 1; i >= 0; i--) {
                    if (history.get(i).role().equals("user")) {
                        // Put the new period first, so “còn tháng trước?” overrides the old period.
                        String previous = history.get(i).content();
                        if (!ChatDataTools.wantsData(query)) query += " " + previous;
                        else query += " " + ChatPeriod.withoutPeriod(previous);
                        break;
                    }
                }
            }
            String denied = ChatRolePolicy.denial(role, query);
            if (denied != null) return restricted(denied);
            if (knowledge.outsideRole(role, query)) return restricted("Chức năng này không thuộc vai trò đang đăng nhập. Hãy hỏi về chức năng trong vai trò của bạn.");
            if (data != null && role.equals("ADMIN")) {
                var live = data.lookup(actor, query);
                if (live.isPresent()) return new ChatDtos.Response(live.get().answer(), "data", "Số liệu được tra cứu trực tiếp; không gửi kết quả DB đến nhà cung cấp AI.", List.of(live.get().source()));
            }
            List<ChatKnowledge.Guide> guides = knowledge.search(role, query);
            if (guides.isEmpty()) {
                if (!role.equals("ADMIN")) return restricted("Mình chưa tìm thấy hướng dẫn chức năng phù hợp trong vai trò của bạn. Hãy chọn một câu hỏi gợi ý hoặc nêu rõ chức năng cần hỗ trợ.");
                return new ChatDtos.Response("Mình chưa tìm thấy hướng dẫn phù hợp với câu hỏi trong phạm vi vai trò của bạn. "
                        + "Bạn hãy nêu rõ chức năng, tên/mã cần tìm hoặc kỳ cần xem. Có thể hỏi ‘Tổng quan hôm nay’, "
                        + "‘Danh sách học viên’ hoặc ‘Thống kê tháng này’ nếu được cấp quyền. Trợ lý không thay đổi dữ liệu.", "guide", "", List.of());
            }
            if (!role.equals("ADMIN")) {
                // Curated role-specific instructions cannot be expanded by an untrusted model/history.
                var guide = guides.get(0);
                return new ChatDtos.Response(guide.title() + "\n" + guide.content(), "guide", "Chỉ hướng dẫn chức năng theo vai trò; không tra cứu dữ liệu thật.", List.of(guide.source(role)));
            }
            Optional<String> generated = ai.answer(role, request, guides);
            String answer = generated.orElseGet(() -> String.join("\n\n", guides.stream()
                    .map(g -> g.title() + "\n" + g.content()).toList()));
            String notice = generated.isPresent() ? ""
                    : ai.enabled() ? "AI tạm thời không phản hồi. Đang hiển thị hướng dẫn có sẵn."
                    : "Đang dùng hướng dẫn có sẵn; AI chưa được bật hoặc chưa có API key.";
            return new ChatDtos.Response(answer, generated.isPresent() ? "ai" : "guide", notice,
                    guides.stream().map(g -> g.source(role)).toList());
        } finally {
            synchronized (limits) { limits.get(user).busy = false; }
        }
    }

    private ChatDtos.Response restricted(String message) {
        return new ChatDtos.Response(message, "guide", "", List.of());
    }

    private void enter(String user) {
        synchronized (limits) {
            long now = Instant.now().getEpochSecond();
            limits.entrySet().removeIf(e -> !e.getValue().busy && now - e.getValue().start >= 60);
            if (!limits.containsKey(user) && limits.size() >= 10000) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trợ lý đang bận, vui lòng thử lại sau.");
            }
            Window window = limits.computeIfAbsent(user, key -> new Window());
            if (window.busy || window.count >= 12) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ trước khi gửi tiếp (tối đa 12 câu/phút).");
            }
            window.count++;
            window.busy = true;
        }
    }
}

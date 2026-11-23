package com.example.demo.chat;

import java.util.Set;

/** Fail closed before retrieval, provider calls or DB tools. Non-admin chat is guidance only. */
final class ChatRolePolicy {
    private ChatRolePolicy() {}

    static String denial(String role, String question) {
        if ("ADMIN".equals(role)) return null;
        String q = " " + ChatKnowledge.normalize(question).replaceAll("[^a-z0-9]+", " ").strip() + " ";
        if (contains(q, "thong ke", "bao cao", "doanh thu", "chi phi", "loi nhuan", "so lieu", "bao mat",
                "phan quyen", "quyen han", "jwt", "token", "api key", "secret", "database", "sql", "co so du lieu",
                "oauth", "ipn", "callback", "vnpay", "momo", "cau hinh", "may chu", "server", "system prompt",
                "bo qua quyen", "bo qua chi thi", "bo qua huong dan", "toi la admin", "toi la quan tri", "dong vai admin")) {
            return "Nội dung này không được phép hỏi trong vai trò của bạn. Trợ lý chỉ hướng dẫn các chức năng thuộc vai trò đang đăng nhập.";
        }
        if (ChatDataTools.wantsData(question)) {
            return "Vai trò của bạn chỉ được hỏi hướng dẫn chức năng. Trợ lý không được phép tra cứu hoặc cung cấp số liệu, danh sách, lịch hay lương thực tế. Hãy hỏi cách sử dụng chức năng trong vai trò của bạn.";
        }
        boolean otherRole = contains(q, "tai khoan nhan vien", "tao tai khoan", "khoa tai khoan", "mo tai khoan",
                "phe duyet", "website khoa hoc", "don hang", "thanh toan", "google", "facebook");
        if (!Set.of("TC", "CM").contains(role) && contains(q, "duyet lich ranh", "xet duyet", "de xuat bac luong", "quan ly nhan su", "ho so nhan su")) otherRole = true;
        if (!Set.of("TC", "CM").contains(role) && contains(q, "phan cong") && !contains(q, "duoc phan cong", "lich phan cong", "da phan cong")) otherRole = true;
        if (!"TC".equals(role) && contains(q, "phan cong giao vien", "phan cong giang vien", "dieu chinh lich day")) otherRole = true;
        if (!"CM".equals(role) && contains(q, "phan cong sale", "phan cong cs", "dieu chinh ca", "dieu chinh lich lam")) otherRole = true;
        if (!"CS".equals(role) && contains(q, "diem danh giao vien", "diem danh gv")) otherRole = true;
        if (!"TEACHER".equals(role) && contains(q, "cham diem", "nhap diem", "giao bai tap", "giao bai kiem tra")) otherRole = true;
        if (!Set.of("TEACHER", "CS").contains(role) && contains(q, "quan ly lop", "ke hoach buoi hoc", "quan ly hoc vien")) otherRole = true;
        if (otherRole) return "Chức năng này không thuộc vai trò đang đăng nhập. Mình chỉ hướng dẫn các chức năng được cấp cho vai trò của bạn.";
        return null;
    }

    private static boolean contains(String query, String... phrases) {
        for (String phrase : phrases) if (query.contains(" " + phrase + " ")) return true;
        return false;
    }
}

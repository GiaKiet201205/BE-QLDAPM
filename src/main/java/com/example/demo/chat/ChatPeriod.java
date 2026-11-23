package com.example.demo.chat;

import java.time.*;
import java.util.regex.Pattern;

/** Parses only explicit, supported periods. Never silently substitutes an invalid date. */
record ChatPeriod(String kind, LocalDate start, LocalDate end) {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    ChatPeriod {
        if (start.getYear() < 1 || end.getYear() > 9999) throw new DateTimeException("Year must be 1..9999");
    }

    static ChatPeriod parse(String question, LocalDate today) {
        String q = ChatKnowledge.normalize(question);
        var iso = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b").matcher(q);
        var vn = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b").matcher(q);
        if (iso.find()) return day(LocalDate.parse(iso.group()));
        if (vn.find()) return day(LocalDate.of(Integer.parseInt(vn.group(3)), Integer.parseInt(vn.group(2)), Integer.parseInt(vn.group(1))));
        if (q.contains("hom qua")) return day(today.minusDays(1));
        if (q.contains("ngay mai")) return day(today.plusDays(1));
        if (q.contains("hom nay")) return day(today);
        var month = Pattern.compile("\\bthang\\s+(\\d{1,2})(?:\\s*[/.-]\\s*|\\s+nam\\s+|\\s+)?(\\d{4})?\\b").matcher(q);
        var ym = Pattern.compile("\\b(\\d{4})-(\\d{2})\\b").matcher(q);
        if (month.find()) return month(YearMonth.of(month.group(2) == null ? today.getYear() : Integer.parseInt(month.group(2)), Integer.parseInt(month.group(1))));
        if (ym.find()) return month(YearMonth.parse(ym.group()));
        if (q.contains("thang truoc")) return month(YearMonth.from(today).minusMonths(1));
        if (q.contains("thang sau") || q.contains("thang toi")) return month(YearMonth.from(today).plusMonths(1));
        if (q.contains("thang nay")) return month(YearMonth.from(today));
        if (q.contains("tuan")) {
            LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
            if (q.contains("tuan truoc")) monday = monday.minusWeeks(1);
            if (q.contains("tuan sau") || q.contains("tuan toi")) monday = monday.plusWeeks(1);
            return new ChatPeriod("WEEK", monday, monday.plusDays(6));
        }
        var year = Pattern.compile("\\bnam\\s+(\\d{4})\\b").matcher(q);
        if (year.find()) return year(Integer.parseInt(year.group(1)));
        if (q.contains("nam truoc")) return year(today.getYear() - 1);
        if (q.contains("nam nay")) return year(today.getYear());
        return new ChatPeriod("CURRENT", today, today);
    }

    private static ChatPeriod day(LocalDate date) { return new ChatPeriod("DAY", date, date); }
    private static ChatPeriod month(YearMonth month) { return new ChatPeriod("MONTH", month.atDay(1), month.atEndOfMonth()); }
    private static ChatPeriod year(int year) { return new ChatPeriod("YEAR", LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)); }
    String label() { return start + (start.equals(end) ? "" : " đến " + end); }

    static String withoutPeriod(String text) {
        return ChatKnowledge.normalize(text)
                .replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\b|\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b|\\b\\d{4}-\\d{2}\\b", " ")
                .replaceAll("\\b(?:thang\\s+\\d{1,2}(?:\\s*[/.-]\\s*\\d{4}|\\s+nam\\s+\\d{4})?|nam\\s+\\d{4})\\b", " ")
                .replaceAll("\\b(?:hom nay|hom qua|ngay mai|(?:thang|nam|tuan)\\s+(?:nay|truoc|sau|toi))\\b", " ");
    }
}

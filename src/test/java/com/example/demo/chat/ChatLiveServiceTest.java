package com.example.demo.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatLiveServiceTest {
    @Test void liveResultNeverGoesToProviderAndRetainsBackendSource() throws Exception {
        var ai = mock(ChatAiClient.class);
        var data = mock(ChatDataTools.class);
        var actor = ChatServiceTest.actor("ADMIN");
        var source = new ChatDtos.Source("live-data", "Nguồn DB", "/admin/statistics");
        when(data.lookup(actor, "Thống kê tháng này")).thenReturn(Optional.of(new ChatDataTools.Result("17 học viên mới", source)));
        var service = new ChatService(new ChatKnowledge(new ObjectMapper()), ai, data);
        var response = service.reply(actor, new ChatDtos.Request("Thống kê tháng này", null));
        assertEquals("data", response.mode());
        assertEquals("17 học viên mới", response.answer());
        assertEquals(List.of(source), response.sources());
        verifyNoInteractions(ai);
    }

    @Test void followUpReusesTopicButReplacesTheOldPeriod() throws Exception {
        var ai = mock(ChatAiClient.class);
        var data = mock(ChatDataTools.class);
        when(data.lookup(any(), anyString())).thenReturn(Optional.of(new ChatDataTools.Result("result", new ChatDtos.Source("data", "DB", "/admin/storefront"))));
        var service = new ChatService(new ChatKnowledge(new ObjectMapper()), ai, data);
        service.reply(ChatServiceTest.actor("ADMIN"), new ChatDtos.Request("Còn tháng trước?", List.of(
                new ChatDtos.Message("user", "Doanh thu hôm nay"),
                new ChatDtos.Message("assistant", "ignored"))));
        verify(data).lookup(any(), argThat(q -> q.contains("doanh thu") && !q.contains("hom nay") && q.contains("tháng trước")));
    }

    @Test void synonymsAndBodyTextRetrieveRelevantDocumentsWithoutCrossingRoleBoundary() throws Exception {
        var knowledge = new ChatKnowledge(new ObjectMapper());
        assertTrue(knowledge.search("ADMIN", "học sinh").stream().anyMatch(g -> g.id().equals("students")));
        assertTrue(knowledge.search("ADMIN", "IPN callback ZaloPay").stream().anyMatch(g -> g.id().equals("payment-setup")));
        assertTrue(knowledge.search("TEACHER", "IPN callback ZaloPay").stream().noneMatch(g -> g.id().equals("payment-setup")));
    }
}

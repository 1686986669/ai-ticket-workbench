package dev.portfolio.ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1","ai.mode=demo"})
@AutoConfigureMockMvc
class TicketApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    long create() throws Exception {
        var r=mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"接口故障\",\"description\":\"数据库慢查询\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.category").value("DATABASE")).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("id").asLong();
    }
    @Test void lifecycleAndOptimisticLock() throws Exception {
        long id=create();
        mvc.perform(patch("/api/tickets/"+id+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(patch("/api/tickets/"+id+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":0}"))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/tickets/"+id+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
    }
    @Test void invalidTransition() throws Exception {
        long id=create();
        mvc.perform(patch("/api/tickets/"+id+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":0}"))
            .andExpect(status().isConflict());
    }
    @Test void invalidRequest() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \",\"description\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void pageBounds() throws Exception {
        mvc.perform(get("/api/tickets?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets?page=0")).andExpect(status().isBadRequest());
    }
    @Test void missingTicket() throws Exception { mvc.perform(get("/api/tickets/9999999")).andExpect(status().isNotFound()); }
    @Test void suggestion() throws Exception {
        long id=create();
        mvc.perform(post("/api/tickets/"+id+"/suggestion")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("demo-rule"));
    }
    @Test void sqlInputRemainsData() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"'; DROP TABLE tickets;--\",\"description\":\"test\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/tickets")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
    }
    @Test void healthAndPage() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}

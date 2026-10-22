package dev.portfolio.ticket;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class TicketService {
    private final TicketMapper mapper;
    public TicketService(TicketMapper mapper) { this.mapper=mapper; }
    public Ticket get(long id) {
        Ticket t=mapper.get(id);
        if(t==null) throw new ResponseStatusException(NOT_FOUND,"Ticket not found");
        return t;
    }
    @Transactional
    public Ticket create(String title,String description) {
        String category=description.contains("数据库")||description.contains("慢查询")?"DATABASE":
            description.contains("超时")?"TIMEOUT":description.contains("部署")?"RELEASE":"OTHER";
        Map<String,Object> row=new HashMap<>();
        row.put("title",title.strip()); row.put("description",description.strip()); row.put("category",category);
        mapper.insert(row);
        return get(((Number)row.get("id")).longValue());
    }
    public Map<String,Object> list(int page,int size) {
        return Map.of("items",mapper.list(size,(page-1)*size),"total",mapper.count(),"page",page,"size",size);
    }
    @Transactional
    public Ticket transition(long id,String status,int version) {
        Ticket old=get(id);
        if(old.version()!=version) throw new ResponseStatusException(CONFLICT,"Stale version; reload ticket");
        boolean allowed=old.status().equals("OPEN")&&status.equals("IN_PROGRESS")||old.status().equals("IN_PROGRESS")&&status.equals("RESOLVED");
        if(!allowed) throw new ResponseStatusException(CONFLICT,"Invalid status transition");
        if(mapper.updateStatus(id,status,version)!=1) throw new ResponseStatusException(CONFLICT,"Concurrent update; reload ticket");
        return get(id);
    }
}

package dev.portfolio.ticket;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TicketMapper {
    @Insert("INSERT INTO tickets(title,description,category,status,version,created_at) VALUES(#{title},#{description},#{category},'OPEN',0,CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys=true,keyProperty="id")
    void insert(Map<String,Object> ticket);

    @Select("SELECT * FROM tickets WHERE id=#{id}") Ticket get(long id);

    @Select("SELECT * FROM tickets ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Ticket> list(@Param("limit") int limit,@Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tickets") long count();

    @Update("UPDATE tickets SET status=#{status},version=version+1 WHERE id=#{id} AND version=#{version}")
    int updateStatus(@Param("id") long id,@Param("status") String status,@Param("version") int version);
}

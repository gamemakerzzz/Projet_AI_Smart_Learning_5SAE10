package com.smartlearn.forum;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.smartlearn.forum.ForumInsightModels.*;

@Service
public class ForumContextService {
    public static final int REPLY_LIMIT=20, REPLY_CHARACTERS=1500;
    private final ForumService forum; private final ReplyRepository replies;
    public ForumContextService(ForumService forum,ReplyRepository replies) { this.forum=forum; this.replies=replies; }
    @Transactional(readOnly=true)
    public ThreadContext snapshot(Long id) {
        var d=forum.get(id);
        var selected=new ArrayList<>(replies.findByDiscussionId(id,PageRequest.of(0,REPLY_LIMIT,Sort.by(Sort.Direction.DESC,"createdAt","id"))));
        if(d.acceptedReplyId!=null && selected.stream().noneMatch(r->r.id.equals(d.acceptedReplyId))) {
            replies.findById(d.acceptedReplyId).filter(r->r.discussionId.equals(id)).ifPresent(r->{
                if(selected.size()>=REPLY_LIMIT) selected.remove(selected.size()-1);
                selected.add(r);
            });
        }
        selected.sort(Comparator.comparing((Reply r)->r.createdAt).thenComparing(r->r.id));
        long total=replies.countByDiscussionId(id);
        boolean truncated=total>selected.size() || selected.stream().anyMatch(r->r.body.length()>REPLY_CHARACTERS);
        var content=selected.stream().map(r->new ContextReply(r.id,r.body.substring(0,Math.min(r.body.length(),REPLY_CHARACTERS)),r.id.equals(d.acceptedReplyId))).toList();
        return new ThreadContext(d.id,d.title,d.body,d.formationId,d.acceptedReplyId,d.updatedAt,content,total,truncated);
    }
}

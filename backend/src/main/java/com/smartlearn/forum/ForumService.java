package com.smartlearn.forum;

import com.smartlearn.entity.Apprenant;
import com.smartlearn.repository.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.smartlearn.forum.ForumModels.*;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class ForumService {
    private final DiscussionRepository discussions;
    private final ReplyRepository replies;
    private final ApprenantRepository users;
    private final FormationRepository courses;
    public ForumService(DiscussionRepository discussions, ReplyRepository replies, ApprenantRepository users, FormationRepository courses) {
        this.discussions=discussions; this.replies=replies; this.users=users; this.courses=courses;
    }
    Apprenant actor(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Please sign in again."));
    }
    private void requireOwner(Long owner, Authentication auth) {
        if (!owner.equals(actor(auth).getId()) && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            throw new ResponseStatusException(FORBIDDEN, "You can only change your own posts.");
    }
    Discussion get(Long id) { return discussions.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Discussion not found.")); }
    private Discussion lock(Long id) { return discussions.lock(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Discussion not found.")); }
    void validateCourse(Long id) {
        if (id != null && !courses.existsById(id)) throw new ResponseStatusException(BAD_REQUEST, "The selected course no longer exists.");
    }
    private Topic topic(Discussion d) { return new Topic(d.id,d.authorId,d.authorName,d.formationId,d.formationTitle,d.title,d.body,d.acceptedReplyId,replies.countByDiscussionId(d.id),d.createdAt,d.updatedAt,readTags(d.tags)); }
    static List<String> readTags(String value) { return value==null || value.isBlank()?List.of():List.of(value.split(",")); }
    static List<String> normalizeTags(List<String> values) {
        if(values==null) return List.of();
        if(values.size()>6) throw new ResponseStatusException(BAD_REQUEST,"Use at most six topic tags.");
        var normalized=new LinkedHashSet<String>();
        for(String value:values) {
            if(value==null) throw new ResponseStatusException(BAD_REQUEST,"Tags cannot be empty.");
            String tag=value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+","-");
            if(!tag.matches("[a-z0-9][a-z0-9+#.\\-]{0,27}")) throw new ResponseStatusException(BAD_REQUEST,"Each tag must contain 1–28 letters, digits, hyphens, dots, + or #.");
            normalized.add(tag);
        }
        return List.copyOf(normalized);
    }
    @Transactional(readOnly=true)
    public Topics list(Long course, String state, String search, int page) {
        if (!Set.of("all","open","resolved").contains(state) || page<0 || search.length()>180)
            throw new ResponseStatusException(BAD_REQUEST, "Invalid discussion filters.");
        var result = discussions.browse(course,state,search.strip().toLowerCase(Locale.ROOT),PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"updatedAt","id")));
        return new Topics(result.stream().map(this::topic).toList(),result.getTotalElements(),page,result.getTotalPages());
    }
    @Transactional(readOnly=true)
    public Detail detail(Long id) { return new Detail(topic(get(id)),replies.findByDiscussionIdOrderByCreatedAtAscIdAsc(id)); }
    public Detail create(Draft draft, Authentication auth) {
        var author=actor(auth); var d=new Discussion(); d.authorId=author.getId(); d.authorName=author.getPrenom()+" "+author.getNom();
        apply(d,draft); discussions.saveAndFlush(d); return detail(d.id);
    }
    public Detail update(Long id, Draft draft, Authentication auth) {
        var d=lock(id); requireOwner(d.authorId,auth); apply(d,draft); d.acceptedReplyId=null; return detail(id);
    }
    private void apply(Discussion d, Draft draft) {
        if (draft.title().strip().length()<8 || draft.body().strip().length()<15)
            throw new ResponseStatusException(BAD_REQUEST,"Use at least 8 characters for the title and 15 for the details.");
        validateCourse(draft.formationId()); d.title=draft.title().strip(); d.body=draft.body().strip(); d.formationId=draft.formationId();
        d.tags=String.join(",",normalizeTags(draft.tags()));
        d.formationTitle=d.formationId==null?null:courses.findById(d.formationId).orElseThrow().getTitre(); d.updatedAt=Instant.now();
    }
    public void delete(Long id, Authentication auth) {
        var d=lock(id); requireOwner(d.authorId,auth); replies.deleteByDiscussionId(id); discussions.delete(d);
    }
    public Detail reply(Long id, ReplyDraft draft, Authentication auth) {
        var d=lock(id); var author=actor(auth); var r=new Reply(); r.discussionId=id; r.authorId=author.getId(); r.authorName=author.getPrenom()+" "+author.getNom();
        r.body=replyText(draft); replies.saveAndFlush(r); d.updatedAt=Instant.now(); return detail(id);
    }
    private String replyText(ReplyDraft draft) {
        if (draft.body().strip().length()<2) throw new ResponseStatusException(BAD_REQUEST,"A reply must contain at least 2 characters.");
        return draft.body().strip();
    }
    private Reply replyIn(Long topicId, Long replyId) {
        return replies.findById(replyId).filter(r -> r.discussionId.equals(topicId)).orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"Reply not found in this discussion."));
    }
    public Detail editReply(Long id, Long replyId, ReplyDraft draft, Authentication auth) {
        var d=lock(id); var r=replyIn(id,replyId); requireOwner(r.authorId,auth); r.body=replyText(draft); r.updatedAt=Instant.now();
        if (replyId.equals(d.acceptedReplyId)) d.acceptedReplyId=null; d.updatedAt=Instant.now(); return detail(id);
    }
    public Detail deleteReply(Long id, Long replyId, Authentication auth) {
        var d=lock(id); var r=replyIn(id,replyId); requireOwner(r.authorId,auth); if (replyId.equals(d.acceptedReplyId)) d.acceptedReplyId=null;
        replies.delete(r); replies.flush(); d.updatedAt=Instant.now(); return detail(id);
    }
    public Detail accept(Long id, Long replyId, Authentication auth) {
        var d=lock(id); requireOwner(d.authorId,auth); if(replyId!=null) replyIn(id,replyId); d.acceptedReplyId=replyId; d.updatedAt=Instant.now(); return detail(id);
    }
}

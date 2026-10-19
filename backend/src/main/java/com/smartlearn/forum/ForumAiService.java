package com.smartlearn.forum;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.smartlearn.forum.ForumModels.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class ForumAiService {
    public static final int LIMIT=20;
    private final DiscussionRepository discussions;
    private final ForumService forum;
    private final ForumAiClient ai;
    private final long cooldownMs;
    private record Cached(Analysis result, Instant until) {}
    private final Map<String,Cached> cache=new LinkedHashMap<>();
    private final Map<Long,Long> lastCalls=new HashMap<>();
    private final Set<Long> inFlight=new HashSet<>();
    public ForumAiService(DiscussionRepository discussions, ForumService forum, ForumAiClient ai,
                          @Value("${smartlearn.forum.cooldown-ms:10000}") long cooldownMs) {
        this.discussions=discussions; this.forum=forum; this.ai=ai; this.cooldownMs=cooldownMs;
    }
    public Analysis analyze(Draft draft, Long excludeId, Long userId) {
        long start=System.nanoTime(); forum.validateCourse(draft.formationId());
        if(draft.title().strip().length()<8 || draft.body().strip().length()<15)
            throw new ResponseStatusException(BAD_REQUEST,"Please add a meaningful title and details before checking.");
        var page=discussions.browse(draft.formationId(),"all","",PageRequest.of(0,LIMIT+1,Sort.by(Sort.Direction.DESC,"updatedAt","id")));
        var selected=page.stream().filter(d->!Objects.equals(d.id,excludeId)).limit(LIMIT).toList();
        long available=page.getTotalElements();
        if(excludeId!=null && discussions.findById(excludeId).filter(d->draft.formationId()==null || Objects.equals(d.formationId,draft.formationId())).isPresent()) available--;
        if(selected.isEmpty()) return new Analysis(List.of(),"There are no existing discussions in this scope yet. Be the first to ask!",0,available,0,false,false,ai.model(),ForumAiClient.VERSION);
        // Only public question text and opaque IDs are sent. No author names, emails or replies.
        var candidates=selected.stream().map(d->new Candidate(d.id,d.title,d.body.substring(0,Math.min(1200,d.body.length())))).toList();
        String key=draft.toString()+"|"+candidates+"|"+selected.stream().map(d->d.updatedAt.toString()+":"+d.acceptedReplyId).toList()+"|"+ai.model()+"|"+ForumAiClient.VERSION;
        synchronized(this) {
            cache.entrySet().removeIf(e->e.getValue().until.isBefore(Instant.now()));
            var found=cache.get(key);
            if(found!=null) { var r=found.result; return new Analysis(r.matches(),r.summary(),r.scannedCount(),available,elapsed(start),true,true,r.model(),r.promptVersion()); }
            long now=System.currentTimeMillis(); lastCalls.entrySet().removeIf(e->now-e.getValue()>60000);
            if(inFlight.contains(userId) || now-lastCalls.getOrDefault(userId,0L)<cooldownMs)
                throw new ResponseStatusException(TOO_MANY_REQUESTS,"Please wait a few seconds before checking again.");
            if(lastCalls.size()>=1000 || inFlight.size()>=10) throw new ResponseStatusException(TOO_MANY_REQUESTS,"AI is busy. Please try again shortly.");
            lastCalls.put(userId,now); inFlight.add(userId);
        }
        try {
            var raw=ai.classify(draft,candidates); var matches=validate(raw,selected);
            String summary=matches.stream().anyMatch(m->m.relationship().equals("DUPLICATE")) ? "A similar question may already have an answer. Review the suggestions before publishing." :
                matches.isEmpty()?"No close matches were found in the discussions checked. You can publish your question.":"Related discussions were found, but they ask different questions. Your question may still be worth posting.";
            var result=new Analysis(matches,summary,selected.size(),available,elapsed(start),false,true,ai.model(),ForumAiClient.VERSION);
            synchronized(this) { if(cache.size()>=100) cache.remove(cache.keySet().iterator().next()); cache.put(key,new Cached(result,Instant.now().plusSeconds(300))); }
            return result;
        } finally { synchronized(this) { inFlight.remove(userId); } }
    }
    static List<Match> validate(JsonNode raw, List<Discussion> candidates) {
        if(raw==null || !raw.path("results").isArray() || raw.path("results").size()!=candidates.size()) throw invalid();
        Map<Long,Discussion> allowed=new HashMap<>(); candidates.forEach(d->allowed.put(d.id,d));
        Set<Long> seen=new HashSet<>(); List<Match> matches=new ArrayList<>();
        for(var item:raw.path("results")) {
            if(!item.path("discussionId").isIntegralNumber() || !item.path("discussionId").canConvertToLong()) throw invalid();
            long id=item.path("discussionId").asLong(); String label=item.path("relationship").asText(); String why=item.path("explanation").asText().strip();
            if(!allowed.containsKey(id) || !seen.add(id) || !Set.of("DUPLICATE","RELATED","UNRELATED").contains(label) || why.isEmpty() || why.length()>600) throw invalid();
            var d=allowed.get(id);
            if(!label.equals("UNRELATED")) matches.add(new Match(id,d.title,label,why,d.acceptedReplyId!=null));
        }
        matches.sort(Comparator.comparing(m->m.relationship().equals("DUPLICATE")?0:1));
        return matches.stream().limit(5).toList();
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(BAD_GATEWAY,"AI returned invalid discussion references or classifications. Please retry."); }
    private static long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
}

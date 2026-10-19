package com.smartlearn.forum;

import com.smartlearn.repository.ApprenantRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("demo")
public class ForumDemoData {
    private final DiscussionRepository discussions; private final ReplyRepository replies; private final ApprenantRepository users;
    public ForumDemoData(DiscussionRepository discussions,ReplyRepository replies,ApprenantRepository users) { this.discussions=discussions; this.replies=replies; this.users=users; }
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if(discussions.count()>0) return;
        var author=users.findByEmailIgnoreCase("ahmed@example.com").orElse(null); if(author==null) return;
        String[][] samples={
            {"How can I protect Angular routes with an auth guard?","I need to redirect visitors who are not signed in to the login page before they can open the dashboard."},
            {"How do I style an Angular login form?","My login form works but I want the fields to align and display validation errors under the inputs."},
            {"When should I use COMMIT or ROLLBACK in SQL?","I am learning transactions. How do I save successful changes and undo the transaction if an operation fails?"},
            {"How do I fix a NullPointerException in Java?","A service throws NullPointerException when I call a method on an object. How can I find which reference is null?"},
            {"What is the difference between a Java List and a Set?","I want to choose a collection for student IDs. I need to understand ordering and whether duplicate values are allowed."}
        };
        for(var sample:samples) { var d=new Discussion(); d.authorId=author.getId(); d.authorName=author.getPrenom()+" "+author.getNom(); d.title=sample[0]; d.body=sample[1]; discussions.save(d);
            if(sample==samples[0]) { var r=new Reply(); r.discussionId=d.id; r.authorId=d.authorId; r.authorName=d.authorName; r.body="Use an Angular route guard to check the session and return a login UrlTree for anonymous visitors. Enforce authorization on the backend too; a client-side guard alone cannot secure an API."; replies.save(r); d.acceptedReplyId=r.id; discussions.save(d); }
        }
    }
}

package ir.ac.sbu.tweeter.dao;

import ir.ac.sbu.tweeter.dto.TweetSearchParamsDto;
import ir.ac.sbu.tweeter.dto.UserSearchDto;
import ir.ac.sbu.tweeter.entity.Tweet;
import ir.ac.sbu.tweeter.entity.User;
import ir.ac.sbu.tweeter.manager.Tweet.TweetNotFoundException;
import ir.ac.sbu.tweeter.manager.User.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.util.List;

@Slf4j
@Component
public class TweetDao {

    @PersistenceContext
    EntityManager entityManager;

    public void save(Tweet tweet) {
        log.info("going to save tweet to db :{}", tweet);
        entityManager.persist(tweet);
        entityManager.flush();
        log.info("user is saved to database. Id is : {}", tweet.getId());
    }

    public Tweet loadByUsername(String uuid) {
        log.debug("going to load Tweet with uuid number : {}", uuid);
        try {
            return doLoadByUsername(uuid);
        } catch (NoResultException e) {
            throw new TweetNotFoundException(uuid);
        }
    }

    private Tweet doLoadByUsername(String uuid) {
        String queryString = "select t from Tweet t where t.uuid = :uuid";
        TypedQuery<Tweet> query = entityManager.createQuery(queryString, Tweet.class);
        query.setParameter("uuid", uuid);
        return query.getSingleResult();
    }

    public int delete(String uuid) {
        Query query = entityManager.createQuery("delete " +
                "from Tweet t " + "where t.uuid = :uuid");
        query.setParameter("uuid", uuid);
        return query.executeUpdate();
    }



    public List<Tweet> search(TweetSearchParamsDto dto) {
        String queryExpression = createSearchQuery(dto);
        TypedQuery<Tweet> query = entityManager.createQuery(queryExpression, Tweet.class);
        if (StringUtils.hasText(dto.getName())) {
            query.setParameter("name", dto.getName());
        }
        if (StringUtils.hasText(dto.getHashtag())) {
            query.setParameter("hashtag", dto.getHashtag());
        }
        return query.getResultList();
    }

    private String createSearchQuery(TweetSearchParamsDto params) {
        String queryExpression = "select t from Tweet t ";
        queryExpression += addWhereClause(params.getName(), params.getHashtag());
        return queryExpression;

    }

    private String addWhereClause(String name, String hashtag) {
        String result ="where 1=1";
        if (StringUtils.hasText(name)) {
            result += " and t.name = :name ";
        }
        if (StringUtils.hasText(hashtag)) {
            result += " and :hashtag  in t.hashtags";
        }
        return result;
    }
}
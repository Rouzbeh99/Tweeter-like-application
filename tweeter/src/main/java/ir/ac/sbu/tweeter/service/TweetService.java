package ir.ac.sbu.tweeter.service;

import ir.ac.sbu.tweeter.dto.*;
import ir.ac.sbu.tweeter.entity.Tweet;
import ir.ac.sbu.tweeter.entity.User;
import ir.ac.sbu.tweeter.manager.Tweet.TweetManager;
import ir.ac.sbu.tweeter.manager.Tweet.TweetNotFoundException;
import ir.ac.sbu.tweeter.manager.User.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.*;

@Slf4j
@Path("tweet")
@Component
public class TweetService {

    private final TweetManager tweetManager;
    private static final int NOT_FOUND = 404;

    @Autowired
    public TweetService(TweetManager tweetManager) {
        this.tweetManager = tweetManager;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTweet(TweetSaveRequestDto dto) {
        Response response;
        log.info("save dto is :{}",dto);
        try {
            Tweet savedTweet = tweetManager.save(dto);
            TweetResponseDto responseDto = creatDto(savedTweet,dto.getOwnerUsername());
            response = ok(responseDto).build();
        } catch (UserNotFoundException e) {
            response = Response.status(NOT_FOUND).build();
        }
        return response;
    }


    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadByUUID(@PathParam("uuid") String uuid) {
        Response response;
        try {
            Tweet tweet = tweetManager.loadByUUID(uuid);
            TweetResponseDto dto = creatDto(tweet, tweet.getOwner().getUsername());
            response = Response.ok(dto).build();
        } catch (UserNotFoundException e) {
            response = status(NOT_FOUND).build();
        }
        return response;
    }


    @DELETE
    @Path("{uuid}")
    public Response removeUser(@PathParam("uuid") String uuid) {

        Response response;
        try {
            tweetManager.delete(uuid);
            response = noContent().build();
        } catch (TweetNotFoundException e) {
            response = status(NOT_FOUND).build();
        }
        return response;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("username") String ownerUsername,
                           @QueryParam("hashtag") String hashtag,
                           @QueryParam("startDate") String startDate,
                           @QueryParam("finishDate") String finishDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        TweetSearchParamsDto dto = TweetSearchParamsDto.builder()
                .ownerUsername(ownerUsername)
                .hashtag(hashtag)
                .build();
        if (StringUtils.hasText(startDate)){
            dto.setStartDate(LocalDateTime.parse(startDate, formatter));
        }
        if (StringUtils.hasText(finishDate)){
            dto.setFinishDate(LocalDateTime.parse(finishDate, formatter));
        }
        log.info("search params dto is :{}", dto);

        List<Tweet> tweetList = tweetManager.search(dto);
        List<TweetResponseDto> tweetResponseDtos = new ArrayList<>();
        for (Tweet tweet : tweetList) {
            tweetResponseDtos.add(creatDto(tweet,dto.getOwnerUsername()));
        }
        TweetPageDto resultDto = TweetPageDto.builder()
                .tweets(tweetResponseDtos)
                .build();
        log.info("page is :{}",resultDto);
        return Response.ok(resultDto).build();

    }

    @GET
    @Path("like")
    public Response like(@QueryParam("uuid") String tweetUUID,
                         @QueryParam("username") String ownerUsername){
        Response response;
        try {
            tweetManager.like(tweetUUID, ownerUsername);
            log.info("manager finished");
            response = noContent().build();
        } catch (TweetNotFoundException | UserNotFoundException e) {
            log.info("exception thrown");
            response = status(NOT_FOUND).build();
        }
        return response;
    }

    @GET
    @Path("unlike")
    public Response unLike(@QueryParam("uuid") String tweetUUID,
                         @QueryParam("username") String ownerUsername){
        Response response;
        try {
            tweetManager.unLike(tweetUUID, ownerUsername);
            log.info("manager finished");
            response = noContent().build();
        } catch (TweetNotFoundException | UserNotFoundException e) {
            log.info("exception thrown");
            response = status(NOT_FOUND).build();
        }
        return response;
    }

    @GET
    @Path("retweet")
    public Response retweet(@QueryParam("uuid") String tweetUUID,
                         @QueryParam("username") String ownerUsername){
        Response response;
        try {
            tweetManager.retweet(tweetUUID, ownerUsername);
            response = noContent().build();
        } catch (TweetNotFoundException | UserNotFoundException e) {
            response = status(NOT_FOUND).build();
        }
        return response;
    }

    @GET
    @Path("unretweet")
    public Response unRetweet(@QueryParam("uuid") String tweetUUID,
                            @QueryParam("username") String ownerUsername){
        Response response;
        try {
            tweetManager.unRetweet(tweetUUID, ownerUsername);
            response = noContent().build();
        } catch (TweetNotFoundException | UserNotFoundException e) {
            response = status(NOT_FOUND).build();
        }
        return response;
    }


    private TweetResponseDto creatDto(Tweet savedTweet,String ownerUsername) {
        TweetResponseDto tweet = TweetResponseDto.builder()
                .body(savedTweet.getBody())
                .ownerUsername(ownerUsername)
                .hashtags(savedTweet.getHashtags())
                .mentions(savedTweet.getMentions())
                .uuid(savedTweet.getUuid())
                .likedBy(savedTweet.getLikedBy().stream().map(User::getUsername).collect(Collectors.toList()))
                .retweetedBy(savedTweet.getRetweetedBy().stream().map(User::getUsername).collect(Collectors.toList()))
                .build();
        if(Objects.nonNull(savedTweet.getTime())){
            tweet.setTime(savedTweet.getTime().toString());
        }
        return tweet;
    }

}

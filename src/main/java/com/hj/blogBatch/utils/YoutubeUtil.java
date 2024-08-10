package com.hj.blogBatch.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
// import com.google.api.services.samples.youtube.cmdline.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ContentRating;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.ThumbnailSetResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.collect.Lists;
import com.hj.blogBatch.config.Auth;
import com.hj.blogBatch.dto.ShortsDto;
import com.hj.blogBatch.dto.VideoDto;
import com.hj.blogBatch.entity.Category;

import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class YoutubeUtil {

    private static final String apiKey = "AIzaSyDzABT_l26KbZerteW9Xew-nFWf2NnUZ6k";
    private static final String APPLICATION_NAME = "hojun-youtube";
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String VIDEO_FILE_FORMAT = "video/*";

    String uName = "user_name";
    String uPassword = "user_password";
    YouTube easyChoiceYoutube;
    String easyChoiceeKeyPath = "/client_secrets.json";
    YouTube shortsYoutube;
    String shortsKeyPath = "/client_secrets2.json";
    @Value("${com.blogBatch.img.path}")
    String mediaPath;
    @Value("${com.shortsBatch.shortsYn}")
    String shortsYn;

    @PostConstruct
    public void init() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        if(!shortsYn.equals("N")){
            List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.upload");

            Credential easyCredential = Auth.authorize(scopes, "uploadvideo", easyChoiceeKeyPath);
            Credential shortsCredential = Auth.authorize(scopes, "uploadvideo", shortsKeyPath);
    
            this.easyChoiceYoutube = new YouTube.Builder(httpTransport, JSON_FACTORY, easyCredential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
    
            this.shortsYoutube = new YouTube.Builder(httpTransport, JSON_FACTORY, shortsCredential)
            .setApplicationName("shorts-youtube")
            .build();    
        }
    }

    public void uploadVideo(com.hj.blogBatch.entity.Video video) throws Exception {

        try {

            /*
             * YouTube youTube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new
             * HttpRequestInitializer() {
             * 
             * public void initialize(HttpRequest request) throws IOException {
             * }
             * 
             * }).setApplicationName("youtube-cmdline-search-sample")
             * .build();
             */

            YouTube youtube = this.easyChoiceYoutube;

            Video youtubeVideo = new Video();

            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            // status.setPrivacyStatus("private");
            status.setMadeForKids(false);
            status.setSelfDeclaredMadeForKids(false);
            youtubeVideo.setStatus(status);
            

            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(video.getVideoTitle());
            snippet.setDescription(video.getVideoDesc());
            snippet.setTags(Arrays.asList(video.getTags().split(",")));
            // snippet.setCategoryId("26");

            youtubeVideo.setSnippet(snippet);


            File file = new File(mediaPath + video.getVideoFile().getFileNm());
            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new FileInputStream(file));
            mediaContent.setLength(file.length());

            // Insert the video. The command sends three arguments. The first
            // specifies which information the API request is setting and which
            // information the API response should return. The second argument
            // is the video resource that contains metadata about the new video.
            // The third argument is the actual video content.

            // YouTube.Videos.Insert videoInsert = youTube.videos().insert();
            // videoInsert.setKey(apiKey);
            YouTube.Videos.Insert request = youtube.videos().insert( List.of("snippet","status"), youtubeVideo, mediaContent);

            // YouTube.Videos.Insert videoInsert =
            // youtube.videos().insert("snippet,statistics,status",
            // videoObjectDefiningMetadata, mediaContent);

            // Set the upload type and add an event listener.
            MediaHttpUploader uploader = request.getMediaHttpUploader();

            // Indicate whether direct media upload is enabled. A value of
            // "True" indicates that direct media upload is enabled and that
            // the entire media content will be uploaded in a single request.
            // A value of "False," which is the default, indicates that the
            // request will use the resumable media upload protocol, which
            // supports the ability to resume an upload operation after a
            // network interruption or other transmission failure, saving
            // time and bandwidth in the event of network failures.
            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            log.info("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            log.info("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            log.info("Upload in progress");
                            log.info("Upload percentage: " + uploader.getProgress());
                            break;
                        case MEDIA_COMPLETE:
                            log.info("Upload Completed!");
                            break;
                        case NOT_STARTED:
                            log.info("Upload Not Started!");
                            break;
                    }
                }
            };
            uploader.setProgressListener(progressListener);

            // Call the API and upload the video.
            Video returnedVideo = request
                    .setKey(apiKey)
                    .execute();

            // Print data about the newly inserted video from the API response.
            log.info("\n================== Returned Video ==================\n");
            log.info("  - Id: " + returnedVideo.getId());
            log.info("  - Title: " + returnedVideo.getSnippet().getTitle());
            log.info("  - Tags: " + returnedVideo.getSnippet().getTags());
            log.info("  - channel Title: " + returnedVideo.getSnippet().getChannelTitle());
            video.setYoutubeChannel(returnedVideo.getSnippet().getChannelTitle());
            video.setYoutubeVideoId(returnedVideo.getId());
            // video.setyotube(returnedVideo.getId());
            // log.info("  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
            // log.info("  - Video Count: " + returnedVideo.getStatistics().getViewCount());

            
            if(video.getThumbnailFile() != null){
                uploadThumbnail(returnedVideo.getId(), mediaPath + video.getThumbnailFile().getFileNm(), youtube);
            }

        } catch (GoogleJsonResponseException e) {
            log.error(e.getMessage());
            // System.err.println("GoogleJsonResponseException code: " +
            // e.getDetails().getCode() + " : "
            // + e.getDetails().getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("IOException: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Throwable t) {
            log.error("Throwable: " + t.getMessage());
            throw new RuntimeException(t);
        }


    }


    public void uploadThumbnail(String videoId ,String filePath, YouTube youtubeService) throws IOException, GeneralSecurityException{
        // YouTube youtubeService = this.youtube;
        //       with a pointer to the actual file you are uploading.
        //       The maximum file size for this operation is 2097152.
        File mediaFile = new File(filePath);
        InputStreamContent mediaContent =
            new InputStreamContent("image/jpeg", new BufferedInputStream(new FileInputStream(mediaFile)));
        mediaContent.setLength(mediaFile.length());

        // Define and execute the API request
        YouTube.Thumbnails.Set request = youtubeService.thumbnails()
            .set(videoId, mediaContent);
        ThumbnailSetResponse response = request.execute();
        log.info("----- finish uploadThumbnail ------\nresponse : " + response);
    }

    public void uploadShorts(ShortsDto shortsDto) {
        try {

            YouTube youtube = this.easyChoiceYoutube;

            Video youtubeVideo = new Video();

            VideoStatus status = new VideoStatus();
            // status.setPrivacyStatus("public");
            status.setPrivacyStatus("private");
            status.setMadeForKids(false);
            status.setSelfDeclaredMadeForKids(false);
            youtubeVideo.setStatus(status);
            

            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(shortsDto.getUploadTitle());
            snippet.setDescription(shortsDto.getDescription());
            snippet.setTags(shortsDto.getTagList());
            // snippet.setCategoryId("23");
            youtubeVideo.setSnippet(snippet);


            File file = new File(shortsDto.getShortsFilePath());
            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new FileInputStream(file));
            mediaContent.setLength(file.length());

            YouTube.Videos.Insert request = youtube.videos().insert( List.of("snippet","status"), youtubeVideo, mediaContent);

            
            MediaHttpUploader uploader = request.getMediaHttpUploader();

            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            log.info("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            log.info("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            log.info("Upload in progress");
                            log.info("Upload percentage: " + uploader.getProgress());
                            break;
                        case MEDIA_COMPLETE:
                            log.info("Upload Completed!");
                            break;
                        case NOT_STARTED:
                            log.info("Upload Not Started!");
                            break;
                    }
                }
            };
            uploader.setProgressListener(progressListener);

            // Call the API and upload the video.
            Video returnedVideo = request
                    .setKey(apiKey)
                    .execute();

            // Print data about the newly inserted video from the API response.
            log.info("\n================== Returned Video ==================\n");
            log.info("  - Id: " + returnedVideo.getId());
            log.info("  - Title: " + returnedVideo.getSnippet().getTitle());
            log.info("  - Tags: " + returnedVideo.getSnippet().getTags());
            log.info("  - channel Title: " + returnedVideo.getSnippet().getChannelTitle());
            shortsDto.setYoutubeChannel(returnedVideo.getSnippet().getChannelTitle());
            shortsDto.setYoutubeVideoId(returnedVideo.getId());
            // video.setyotube(returnedVideo.getId());
            // log.info("  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
            // log.info("  - Video Count: " + returnedVideo.getStatistics().getViewCount());

        } catch (GoogleJsonResponseException e) {
            log.error(e.getMessage());
            // System.err.println("GoogleJsonResponseException code: " +
            // e.getDetails().getCode() + " : "
            // + e.getDetails().getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("IOException: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Throwable t) {
            log.error("Throwable: " + t.getMessage());
            throw new RuntimeException(t);
        }
    }

}

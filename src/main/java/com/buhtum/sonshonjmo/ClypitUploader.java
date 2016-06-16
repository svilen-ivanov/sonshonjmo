package com.buhtum.sonshonjmo;

import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ClypitUploader {
    private final static Logger log = LoggerFactory.getLogger(ClypitUploader.class);
    private static final String UPLOAD_URL = "https://upload.clyp.it/upload";
    private static final MediaType MEDIA_TYPE_MP3 = MediaType.parse("audio/mpeg3");

    private final Gson gson = new Gson();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    public ClypitUploader() {

    }

    public String upload(File audioFile, String description) throws IOException {
        final HttpUrl url = HttpUrl.parse(UPLOAD_URL).newBuilder().build();
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audioFile", audioFile.getName(), RequestBody.create(MEDIA_TYPE_MP3, audioFile))
                .addFormDataPart("description", description)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        final String body = response.body().string();
        log.debug("Clypit response: {}", body);

        ClypitResponse clypitResponse = gson.fromJson(body, ClypitResponse.class);
        if (!clypitResponse.Successful || StringUtils.isBlank(clypitResponse.Url)) {
            throw new RuntimeException("Failed to upload audio file");
        }

        return clypitResponse.Url;
    }

    private static final class ClypitResponse {
        private boolean Successful;
        private String Url;
    }

//    {
//        "Successful": true,
//            "PlaylistId": "zft4ysyo",
//            "PlaylistUploadToken": "2b15ec28d5a572f61e4e9eff7b9cd40d",
//            "AudioFileId": "j3xlmxzl",
//            "Title": "speech-2851501441840801081",
//            "Description": "test",
//            "Duration": 33.593,
//            "Url": "https://clyp.it/j3xlmxzl",
//            "Mp3Url": "http://a.clyp.it/j3xlmxzl.mp3",
//            "SecureMp3Url": "https://a.clyp.it/j3xlmxzl.mp3",
//            "OggUrl": "http://a.clyp.it/j3xlmxzl.ogg",
//            "SecureOggUrl": "https://a.clyp.it/j3xlmxzl.ogg",
//            "DateCreated": "2016-06-16T07:48:32.61"
//    }


    public static void main(String[] args) throws IOException {
        File f = new File("/tmp/speech-2851501441840801081.mp3");
        ClypitUploader clypitUploader = new ClypitUploader();
        clypitUploader.upload(f, "test");
    }
}

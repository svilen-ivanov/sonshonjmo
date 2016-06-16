package com.buhtum.sonshonjmo;

import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.ibm.icu.text.Transliterator;
import com.ivona.services.tts.IvonaSpeechCloudClient;
import com.ivona.services.tts.model.CreateSpeechRequest;
import com.ivona.services.tts.model.CreateSpeechResult;
import com.ivona.services.tts.model.Input;
import com.ivona.services.tts.model.Voice;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Speaker {
    private final static Logger log = LoggerFactory.getLogger(Speaker.class);

    private final String credentials;

    public Speaker(String credentials) {
        this.credentials = credentials;
    }

    public File speak(String text, Language lang) throws Exception {
        IvonaSpeechCloudClient speechCloud = new IvonaSpeechCloudClient(
                new PropertiesFileCredentialsProvider(credentials));
        speechCloud.setEndpoint("https://tts.eu-west-1.ivonacloud.com");

//        ListVoicesRequest listVoicesRequest = new ListVoicesRequest();
//        final ListVoicesResult listVoicesResult = speechCloud.listVoices(listVoicesRequest);
//        log.debug("{}", listVoicesResult);

        File outputAudioFile = File.createTempFile("speech-", ".mp3");
        CreateSpeechRequest createSpeechRequest = new CreateSpeechRequest();
        Input input = new Input();
        Voice voice = new Voice();

        // 27 = {com.ivona.services.tts.model.Voice@2894} "Voice [name=Chantal, language=fr-CA, gender=Female]"
        // 28 = {com.ivona.services.tts.model.Voice@2895} "Voice [name=Celine, language=fr-FR, gender=Female]"
        // 29 = {com.ivona.services.tts.model.Voice@2896} "Voice [name=Mathieu, language=fr-FR, gender=Male]"
        // 47 = {com.ivona.services.tts.model.Voice@2914} "Voice [name=Maxim, language=ru-RU, gender=Male]"
        // 48 = {com.ivona.services.tts.model.Voice@2915} "Voice [name=Tatyana, language=ru-RU, gender=Female]"

        if (lang == Language.FR) {
            text = transliterateFr(text);
            log.debug("Transliterated: " + text);
            voice.setName("Celine");
            voice.setLanguage("fr-FR");
        } else if (lang == Language.RU) {
            text = transliterateRu(text);
            log.debug("Transliterated: " + text);
            voice.setName("Maxim");
            voice.setLanguage("ru-RU");
        }
        input.setData(text);

        createSpeechRequest.setInput(input);
        createSpeechRequest.setVoice(voice);

        CreateSpeechResult createSpeechResult = speechCloud.createSpeech(createSpeechRequest);
        log.debug("Success sending request:");
        log.debug(" content type:" + createSpeechResult.getContentType());
        log.debug(" request id: " + createSpeechResult.getTtsRequestId());
        log.debug(" request chars: " + createSpeechResult.getTtsRequestCharacters());
        log.debug(" request units: " + createSpeechResult.getTtsRequestUnits());

        FileUtils.copyInputStreamToFile(createSpeechResult.getBody(), outputAudioFile);

        return outputAudioFile;
    }

    public static void main(String[] args) {
        Speaker speaker = new Speaker("data/ivona.properties");
        try {
            speaker.speak("Ново село 354/-33\n" +
                    "Видин 420/-21\n" +
                    "Лом 448/-9\n" +
                    "Оряхово 350/+8\n" +
                    "Никопол 414/+19\n" +
                    "Свищов 371/+17\n" +
                    "Русе 389/+10\n" +
                    "Силистра 396/+4", Language.RU);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String transliterateFr(String bgString) {
        Transliterator transliterator = Transliterator.getInstance("Bulgarian-Latin/BGN");
        String transliterated = transliterator.transliterate(bgString);
        return transliterated.replaceAll("\n", ". ").replaceAll("(\\d+)/([+-]\\d+)", "$1 centimètre $2 centimètre");

    }

    public String transliterateRu(String bgString) {
        return bgString.replaceAll("\n", ". ").replaceAll("(\\d+)/([+-]\\d+)", "$1 сантиметрах, $2 сантиметрах");

    }

}

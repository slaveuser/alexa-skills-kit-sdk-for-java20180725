/*
    Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
    except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
    the specific language governing permissions and limitations under the License.
 */

package com.amazon.ask.response;

import com.amazon.ask.model.Directive;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.dialog.ConfirmIntentDirective;
import com.amazon.ask.model.dialog.ConfirmSlotDirective;
import com.amazon.ask.model.dialog.DelegateDirective;
import com.amazon.ask.model.dialog.ElicitSlotDirective;
import com.amazon.ask.model.interfaces.audioplayer.AudioItem;
import com.amazon.ask.model.interfaces.audioplayer.ClearBehavior;
import com.amazon.ask.model.interfaces.audioplayer.ClearQueueDirective;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.model.interfaces.audioplayer.PlayDirective;
import com.amazon.ask.model.interfaces.audioplayer.StopDirective;
import com.amazon.ask.model.interfaces.audioplayer.Stream;
import com.amazon.ask.model.interfaces.display.HintDirective;
import com.amazon.ask.model.interfaces.display.PlainTextHint;
import com.amazon.ask.model.interfaces.display.RenderTemplateDirective;
import com.amazon.ask.model.interfaces.display.Template;
import com.amazon.ask.model.interfaces.videoapp.LaunchDirective;
import com.amazon.ask.model.interfaces.videoapp.Metadata;
import com.amazon.ask.model.interfaces.videoapp.VideoItem;
import com.amazon.ask.model.ui.AskForPermissionsConsentCard;
import com.amazon.ask.model.ui.Card;
import com.amazon.ask.model.ui.Image;
import com.amazon.ask.model.ui.LinkAccountCard;
import com.amazon.ask.model.ui.OutputSpeech;
import com.amazon.ask.model.ui.Reprompt;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amazon.ask.model.ui.StandardCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper to create response using ask-sdk-model as per Alexa Skills Kit interface
 */
public class ResponseBuilder {

    protected OutputSpeech speech;
    protected Card card;
    protected List<Directive> directiveList;
    protected Boolean shouldEndSession;
    protected Reprompt reprompt;

    public Optional<Response> build() {
        return Optional.of(Response.builder()
                .withOutputSpeech(speech)
                .withCard(card)
                .withReprompt(reprompt)
                .withDirectives(directiveList)
                .withShouldEndSession(shouldEndSession)
                .build());
    }

    /**
     * Has Alexa say the provided speech to the user
     * @param speechText speech text
     * @return response builder
     */
    public ResponseBuilder withSpeech(String speechText) {
        this.speech = SsmlOutputSpeech.builder()
                .withSsml("<speak>" + trimOutputSpeech(speechText) + "</speak>")
                .build();
        return this;
    }

    /**
     * Renders a simple card with the specified title and content
     * @param cardTitle title for card
     * @param cardText text in the card
     * @return response builder
     */
    public ResponseBuilder withSimpleCard(String cardTitle, String cardText) {
        this.card = SimpleCard.builder()
                .withContent(cardText)
                .withTitle(cardTitle)
                .build();
        return this;
    }

    /**
     * Renders a standard card with the specified title, content and image
     * @param cardTitle title for card
     * @param cardText text in the card
     * @param image image
     * @return response builder
     */
    public ResponseBuilder withStandardCard(String cardTitle, String cardText, Image image) {
        this.card = StandardCard.builder()
                .withText(cardText)
                .withImage(image)
                .withTitle(cardTitle)
                .build();
        return this;
    }

    /**
     * Renders a link account card
     * @return response builder
     */
    public ResponseBuilder withLinkAccountCard() {
        this.card = LinkAccountCard.builder().build();
        return this;
    }

    /**
     * Renders an AskForPermissionsConsent card
     * @param permissions permission array
     * @return response builder
     */
    public ResponseBuilder withAskForPermissionsConsentCard(List<String> permissions) {
        this.card = AskForPermissionsConsentCard.builder()
                .withPermissions(permissions)
                .build();
        return this;
    }

    /**
     * Has alexa listen for speech from the user. If the user doesn't respond within 8 seconds, then has alexa reprompt with the provided reprompt speech
     * @param text reprompt text
     * @return response builder
     */
    public ResponseBuilder withReprompt(String text) {
        this.reprompt = Reprompt.builder()
                .withOutputSpeech(SsmlOutputSpeech.builder()
                        .withSsml("<speak>" + trimOutputSpeech(text) + "</speak>")
                        .build())
                .build();

        if (!this.isVideoAppLaunchDirectivePresent()) {
            this.shouldEndSession = false;
        }

        return this;
    }

    /**
     * Sets shouldEndSession value to null/false/true
     * @param shouldEndSession whether session should end or not
     * @return response builder
     */
    public ResponseBuilder withShouldEndSession(Boolean shouldEndSession) {
        if (!this.isVideoAppLaunchDirectivePresent()) {
            this.shouldEndSession = shouldEndSession;
        }

        return this;
    }

    /**
     * Adds a hint directive - show a hint on the screen of the echo show
     * @param hintText hint text
     * @return response builder
     */
    public ResponseBuilder addHintDirective(String hintText) {
        PlainTextHint hint = PlainTextHint.builder()
                .withText(hintText)
                .build();
        HintDirective hintDirective = HintDirective.builder()
                .withHint(hint)
                .build();
        return addDirective(hintDirective);
    }

    /**
     * Adds a VideoApp play directive to play a video
     * @param source Identifies the location of video content at a remote HTTPS location and video file must be hosted at an Internet-accessible HTTPS endpoint
     * @param title title that can be displayed on VideoApp
     * @param subtitle subtitle that can be displayed on videoApp
     * @return response builder
     */
    public ResponseBuilder addVideoAppLaunchDirective(String source, String title, String subtitle) {
        Metadata metadata = Metadata.builder()
                .withSubtitle(subtitle)
                .withTitle(title)
                .build();

        VideoItem videoItem = VideoItem.builder()
                .withSource(source)
                .withMetadata(metadata)
                .build();
        LaunchDirective videoDirective = LaunchDirective.builder()
                .withVideoItem(videoItem)
                .build();

        this.shouldEndSession = null;
        return addDirective(videoDirective);
    }

    /**
     * Adds a Display RenderTemplate Directive
     * @param template display template
     * @return response builder
     */
    public ResponseBuilder addRenderTemplateDirective(Template template) {
        RenderTemplateDirective templateDirective = RenderTemplateDirective.builder()
                .withTemplate(template)
                .build();
        return addDirective(templateDirective);
    }

    /**
     * Adds a Dialog delegate directive to response
     * @param updatedIntent updated intent
     * @return response builder
     */
    public ResponseBuilder addDelegateDirective(Intent updatedIntent) {
        DelegateDirective delegateDirective = DelegateDirective.builder()
                .withUpdatedIntent(updatedIntent)
                .build();
        return addDirective(delegateDirective);
    }

    /**
     * Adds a Dialog elicitSlot directive to response
     * @param slotName name of slot
     * @param updatedIntent updated intent
     * @return response builder
     */
    public ResponseBuilder addElicitSlotDirective(String slotName, Intent updatedIntent) {
        ElicitSlotDirective elicitSlotDirective = ElicitSlotDirective.builder()
                .withUpdatedIntent(updatedIntent)
                .withSlotToElicit(slotName)
                .build();
        return addDirective(elicitSlotDirective);
    }

    /**
     * Adds a Dialog confirmSlot directive to response
     * @param slotName name of slot
     * @param updatedIntent updated intent
     * @return response builder
     */
    public ResponseBuilder addConfirmSlotDirective(String slotName, Intent updatedIntent) {
        ConfirmSlotDirective confirmSlotDirective = ConfirmSlotDirective.builder()
                .withSlotToConfirm(slotName)
                .withUpdatedIntent(updatedIntent)
                .build();
        return addDirective(confirmSlotDirective);
    }

    /**
     * Adds a Dialog confirmIntent directive to response
     * @param updatedIntent updated intent
     * @return response builder
     */
    public ResponseBuilder addConfirmIntentDirective(Intent updatedIntent) {
        ConfirmIntentDirective confirmIntentDirective = ConfirmIntentDirective.builder()
                .withUpdatedIntent(updatedIntent)
                .build();
        return addDirective(confirmIntentDirective);
    }

    /**
     * Adds an AudioPlayer play directive
     * @param playBehavior Describes playback behavior
     * @param offsetInMilliseconds The timestamp in the stream from which Alexa should begin playback
     * @param expectedPreviousToken A token that represents the expected previous stream
     * @param token A token that represents the audio stream. This token cannot exceed 1024 characters
     * @param url Identifies the location of audio content at a remote HTTPS location
     * @return response builder
     */
    public ResponseBuilder addAudioPlayerPlayDirective(PlayBehavior playBehavior, Long offsetInMilliseconds,
                                                       String expectedPreviousToken, String token, String url) {
        Stream stream = Stream.builder()
                .withOffsetInMilliseconds(offsetInMilliseconds)
                .withExpectedPreviousToken(expectedPreviousToken)
                .withToken(token)
                .withUrl(url)
                .build();

        AudioItem audioItem = AudioItem.builder()
                .withStream(stream)
                .build();

        PlayDirective playDirective = PlayDirective.builder()
                .withPlayBehavior(playBehavior)
                .withAudioItem(audioItem)
                .build();
        return addDirective(playDirective);
    }

    /**
     * Adds an AudioPlayer Stop directive - Stops the current audio Playback
     * @return response builder
     */
    public ResponseBuilder addAudioPlayerStopDirective() {
        StopDirective stopDirective = StopDirective.builder().build();
        return addDirective(stopDirective);
    }

    /**
     * Adds an AudioPlayer ClearQueue directive - clear the queue without stopping the currently playing stream,
     * or clear the queue and stop any currently playing stream.
     * @param clearBehavior  Describes the clear queue behavior
     * @return response builder
     */
    public ResponseBuilder addAudioPlayerClearQueueDirective(ClearBehavior clearBehavior) {
        ClearQueueDirective clearQueueDirective = ClearQueueDirective.builder()
                .withClearBehavior(clearBehavior)
                .build();
        return addDirective(clearQueueDirective);
    }

    /**
     * Helper method for adding directives to responses
     * @param directive directive the directive send back to Alexa device
     * @return response builder
     */
    public ResponseBuilder addDirective(Directive directive) {
        if (directiveList == null) {
            directiveList = new ArrayList<>();
        }
        directiveList.add(directive);

        return this;
    }


    private boolean isVideoAppLaunchDirectivePresent() {
        if (directiveList == null || directiveList.isEmpty()) {
            return false;
        }

        for (Directive directive : directiveList) {
            if (directive.getType().equals("VideoApp.Launch")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes <speak></speak> XML tag in speechOutput
     * @param outputSpeech output speech
     * @return trimmed output speech
     */
    private String trimOutputSpeech(String outputSpeech) {
        if (outputSpeech == null) {
            return "";
        }
        String trimmedOutputSpeech = outputSpeech.trim();
        if (trimmedOutputSpeech.startsWith("<speak>") && trimmedOutputSpeech.endsWith("</speak>")) {
            return trimmedOutputSpeech.substring(7, trimmedOutputSpeech.length() - 8).trim();
        }
        return trimmedOutputSpeech;
    }

}

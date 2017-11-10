package ru.vzmx.webhookconvert;

import com.fasterxml.jackson.annotation.JsonProperty;

class SlackHook {
    SlackHook(String text) {
        this.text = text;
    }

    @JsonProperty
    private String text;
}
